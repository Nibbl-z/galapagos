package xyz.nibblz.galapagos

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import xyz.nibblz.galapagos.data.ConstantIslandData
import xyz.nibblz.galapagos.features.CoinTracking
import xyz.nibblz.galapagos.features.CosmeticMachineChances
import xyz.nibblz.galapagos.features.CraftingInstructions
import xyz.nibblz.galapagos.features.CrateChances
import xyz.nibblz.galapagos.features.ExchangeUnitPrice
import xyz.nibblz.galapagos.features.QuestTracking

object Galapagos : ModInitializer {
	const val MOD_ID: String = "galapagos"

	val logger: Logger = LoggerFactory.getLogger(MOD_ID)
	var save: PlayerSave = PlayerSave()
	val font = FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "main"))

	fun registerFeatures() {
		CoinTracking.init()
		QuestTracking.init()
		CrateChances.init()
		CosmeticMachineChances.init()
		ExchangeUnitPrice.init()
		CraftingInstructions.init()
	}

	override fun onInitialize() {
		Save.load()
		ConstantIslandData.load()
		registerFeatures()
		PlayerData.init()

		ClientLifecycleEvents.CLIENT_STOPPING.register {onShutdown()}
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> GalapagosCommand.register(dispatcher) }


		logger.info("Galapagos initialized!")
	}

	private fun onShutdown() {
		Save.save()
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}