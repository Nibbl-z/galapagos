package xyz.nibblz.galapagos

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import xyz.nibblz.galapagos.features.CoinTracking
import xyz.nibblz.galapagos.features.QuestTracking

object Galapagos : ModInitializer {
	const val MOD_ID: String = "galapagos"

	val logger: Logger = LoggerFactory.getLogger(MOD_ID)
	var save: PlayerSave = PlayerSave()

	fun registerFeatures() {
		CoinTracking.init()
		QuestTracking.init()
	}

	override fun onInitialize() {
		Save.load()
		registerFeatures()

		ClientLifecycleEvents.CLIENT_STOPPING.register {onShutdown()}

		logger.info("Galapagos initialized!")
	}

	private fun onShutdown() {
		logger.info("bye!")
		Save.save()
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}