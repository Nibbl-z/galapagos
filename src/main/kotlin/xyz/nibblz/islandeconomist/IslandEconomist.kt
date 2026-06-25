package xyz.nibblz.islandeconomist

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import xyz.nibblz.islandeconomist.features.CoinTracking

object IslandEconomist : ModInitializer {
	const val MOD_ID: String = "island-economist"

	val logger: Logger = LoggerFactory.getLogger(MOD_ID)
	var save: PlayerSave = PlayerSave()



	fun registerFeatures() {
		CoinTracking.init()
	}

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		Save.load()

		logger.info("Hello Fabric world!")

		ClientLifecycleEvents.CLIENT_STOPPING.register {onShutdown()}
	}

	private fun onShutdown() {
		logger.info("bye!")
		Save.save()
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}