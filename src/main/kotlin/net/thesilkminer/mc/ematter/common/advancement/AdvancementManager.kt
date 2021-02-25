package net.thesilkminer.mc.ematter.common.advancement

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.MOD_NAME

internal object AdvancementManager {
    private val l = L(MOD_NAME, "Advancement Manager")
    private val rootAdvancement = NameSpacedString(MOD_ID, "root")

    @SubscribeEvent
    fun onPlayerLoggedIn(e: PlayerEvent.PlayerLoggedInEvent) {
        val player = e.player
        if (player !is EntityPlayerMP) return
        val progress = player.advancements.getProgress(player.world.minecraftServer?.advancementManager?.getAdvancement(rootAdvancement.toResourceLocation()) ?: return)
        if (progress.isDone) return
        progress.remaningCriteria.forEach { progress.grantCriterion(it) }
        l.info("Granted root advancement for player '${player.name}': welcome to $MOD_NAME")
    }
}
