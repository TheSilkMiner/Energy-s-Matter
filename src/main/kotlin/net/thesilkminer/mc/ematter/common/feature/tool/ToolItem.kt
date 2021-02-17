package net.thesilkminer.mc.ematter.common.feature.tool

import com.google.common.collect.Multimap
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.oredict.OreDictionary

internal open class ToolItem(private val toolClass: String, val materialData: MaterialData, private val damageModifier: Double = 0.0,
                             private val attackSpeedModifier: Double = 0.0, private val durabilityModifier: Int = 0, private val efficiencyModifier: Double = 0.0) : Item() {
    val attackDamage get() = this.materialData.baseDamage + this.damageModifier
    val attackSpeed get() = this.materialData.baseAttackSpeed + this.attackSpeedModifier
    val durability get() = this.materialData.baseDurability + durabilityModifier
    val efficiency get() = this.materialData.efficiencyBaseModifier + this.efficiencyModifier
    val enchantModifier get() = this.materialData.enchantModifier
    val harvestLevel get() = this.materialData.harvestLevel
    val repairStacks get() = this.materialData.repairStacks.map { it().copy() }

    override fun hitEntity(stack: ItemStack, target: EntityLivingBase, attacker: EntityLivingBase): Boolean {
        stack.damageItem(2, attacker)
        return true
    }

    override fun onBlockDestroyed(stack: ItemStack, worldIn: World, state: IBlockState, pos: BlockPos, entityLiving: EntityLivingBase): Boolean {
        if (!worldIn.isRemote && state.getBlockHardness(worldIn, pos).toDouble() != 0.0) stack.damageItem(1, entityLiving)
        return true
    }

    override fun getIsRepairable(toRepair: ItemStack, repair: ItemStack): Boolean {
        if (this.repairStacks.filterNot { it.isEmpty }.any { OreDictionary.itemMatches(it, repair, false) }) return true
        return super.getIsRepairable(toRepair, repair)
    }

    override fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack): Multimap<String, AttributeModifier> {
        val map = super.getAttributeModifiers(slot, stack)
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.name, AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", this.attackDamage, 0))
            map.put(SharedMonsterAttributes.ATTACK_SPEED.name, AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", this.attackSpeed, 0))
        }
        return map
    }

    override fun getHarvestLevel(stack: ItemStack, toolClass: String, player: EntityPlayer?, blockState: IBlockState?): Int {
        val level = super.getHarvestLevel(stack, toolClass, player, blockState)
        return if (level == -1 && this.toolClass == toolClass) return this.harvestLevel else level
    }

    override fun getItemStackLimit(stack: ItemStack): Int = 1
    override fun getMaxDamage(stack: ItemStack): Int = this.durability
    override fun getToolClasses(stack: ItemStack): MutableSet<String> = mutableSetOf(this.toolClass)
    override fun getItemEnchantability(stack: ItemStack): Int = this.enchantModifier
    override fun getDestroySpeed(stack: ItemStack, state: IBlockState): Float =
            if (this.getToolClasses(stack).any { state.block.isToolEffective(it, state) }) this.efficiency.toFloat() else 1.0F
}
