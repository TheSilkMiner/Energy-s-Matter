@file:JvmName("SFZSC")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.compiler

import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import stanhebben.zenscript.IZenErrorLogger
import stanhebben.zenscript.IZenRegistry
import stanhebben.zenscript.ZenTokener
import stanhebben.zenscript.annotations.ZenMethod
import stanhebben.zenscript.compiler.EnvironmentClass
import stanhebben.zenscript.compiler.EnvironmentMethod
import stanhebben.zenscript.compiler.IEnvironmentGlobal
import stanhebben.zenscript.compiler.ITypeRegistry
import stanhebben.zenscript.compiler.TypeRegistry
import stanhebben.zenscript.compiler.ZenClassWriter
import stanhebben.zenscript.definitions.ParsedFunction
import stanhebben.zenscript.impl.GenericCompileEnvironment
import stanhebben.zenscript.impl.GenericRegistry
import stanhebben.zenscript.statements.StatementReturn
import stanhebben.zenscript.symbols.SymbolArgument
import stanhebben.zenscript.symbols.SymbolJavaStaticMethod
import stanhebben.zenscript.type.ZenType
import stanhebben.zenscript.type.natives.JavaMethod
import stanhebben.zenscript.util.MethodOutput
import stanhebben.zenscript.util.ZenPosition
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

internal fun compileFunction(string: String): (Long) -> Long {
    if (!string.startsWith("function __(x as double) as double { return") || !string.endsWith(";}")) {
        throw IllegalArgumentException("'$string' is not a valid compilable expression for stepping functions")
    }
    return string.compile()
}

private fun String.compile(): (Long) -> Long {
    val compileEnvironment = GenericCompileEnvironment()
    val errorLogger = SteppingFunctionCompilationErrorLogger()
    val typeRegistry = TypeRegistry()
    val registry = GenericRegistry(compileEnvironment, errorLogger).apply { this.prepareRegistry(typeRegistry) }
    @Suppress("SpellCheckingInspection") val tokener = ZenTokener(this, compileEnvironment, null, false)

    val classes = mutableMapOf<String, ByteArray>()
    val globalEnvironment = registry.makeGlobalEnvironment(classes)

    val parsedExpression = ParsedFunction.parse(tokener, globalEnvironment)

    return parsedExpression.compile(globalEnvironment)
}

private fun ParsedFunction.compile(globalEnvironment: IEnvironmentGlobal): (Long) -> Long {
    val classVisitor = ZenClassWriter(ClassWriter.COMPUTE_FRAMES)
    val classEnvironment = EnvironmentClass(classVisitor, globalEnvironment)
    classVisitor.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC, "\$", null, "java/lang/Object",
            arrayOf("net/thesilkminer/mc/ematter/compatibility/crafttweaker/compiler/BridgeSF"))

    classVisitor.makeConstructor()

    val methodOutput = MethodOutput(classVisitor, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC, "function", this.signature, null, null)
    val methodEnvironment = EnvironmentMethod(methodOutput, classEnvironment)
    methodEnvironment.putValue("x", SymbolArgument(0, ZenType.DOUBLE), this.position)

    //methodOutput.enableDebug()
    methodOutput.start()

    val statements = this.statements
    statements.forEach { it.compile(methodEnvironment) }

    if (this.returnType != ZenType.VOID) {
        if (statements.isNotEmpty() && statements.last() is StatementReturn) {
            (statements.last() as StatementReturn).expression?.let {
                this.returnType.defaultValue(this.position).compile(true, methodEnvironment)
                methodOutput.returnType(this.returnType.toASMType())
            }
        } else {
            this.returnType.defaultValue(this.position).compile(true, methodEnvironment)
            methodOutput.returnType(this.returnType.toASMType())
        }
    } else if (statements.isEmpty() || statements.last() !is StatementReturn) {
        methodOutput.ret()
    }
    methodOutput.end()

    classVisitor.visitEnd()

    val classRepresentation = classVisitor.toByteArray()
    val customLoader = SteppingFunctionClassLoader(SteppingFunctionCompilationErrorLogger::class.java.classLoader, classRepresentation)
    val targetClass = customLoader.loadClass("\$")
    val instance = targetClass.newInstance() as BridgeSF

    return { instance.function(it.toDouble()).toLong() }
}

private fun IZenRegistry.prepareRegistry(typeRegistry: ITypeRegistry) {
    Class.forName("net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.math.ZenMath").kotlin.functions.asSequence()
            .map { it.findAnnotation<ZenMethod>()?.value to it }
            .filter { it.first != null }
            .map { it.first!! to it.second }
            .forEach { this.tryRegisterGlobal(it.first, it.second.find(typeRegistry)) }
}

private fun IZenRegistry.tryRegisterGlobal(name: String, method: SymbolJavaStaticMethod) = try {
    this.registerGlobal(name, method)
} catch (ignore: IllegalArgumentException) {
    // Ignore
}

private fun <T> KFunction<T>.find(typeRegistry: ITypeRegistry) = SymbolJavaStaticMethod(JavaMethod.get(typeRegistry, this.javaMethod))

private fun ClassVisitor.makeConstructor() = this.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).let {
    it.visitCode()
    it.visitVarInsn(Opcodes.ALOAD, 0)
    it.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
    it.visitInsn(Opcodes.RETURN)
    it.visitMaxs(1, 1)
    it.visitEnd()
}

private class SteppingFunctionCompilationErrorLogger : IZenErrorLogger {
    private val l = L(MOD_NAME, "ZenScript Stepping Function Compiler")

    private fun log(position: ZenPosition?, message: String?, target: (String) -> Unit) = target("${position ?: "system"}: ${message ?: "~~NULL~~"}")

    override fun warning(position: ZenPosition?, message: String?) = this.log(position, message, l::warn)
    override fun warning(message: String?) = this.warning(null, message)
    override fun info(position: ZenPosition?, message: String?) = this.log(position, message, l::info)
    override fun info(message: String?) = this.info(null, message)
    override fun error(position: ZenPosition?, message: String?) = this.log(position, message, l::error)
    override fun error(message: String?) = this.error(null, message)
    override fun error(message: String?, e: Throwable?) = this.l.error("system:${message ?: "~~NULL~~"}", e)
}

private class SteppingFunctionClassLoader(parent: ClassLoader, private val classRepresentation: ByteArray) : ClassLoader(parent) {
    private var actualClass: Class<*>? = null

    override fun findClass(name: String?): Class<*> {
        if (name == "\$") {
            actualClass?.let { return it }
            val defined = this.defineClass(name, this.classRepresentation, 0, this.classRepresentation.count())
            actualClass = defined
            return defined
        }
        return super.findClass(name)
    }

    override fun loadClass(name: String?): Class<*> {
        if (name == "\$") {
            actualClass?.let { return it }
            val defined = this.defineClass(name, this.classRepresentation, 0, this.classRepresentation.count())
            actualClass = defined
            return defined
        }
        return super.loadClass(name)
    }
}

@Suppress("unused")
internal interface BridgeSF {
    fun function(x: Double): Double
}
