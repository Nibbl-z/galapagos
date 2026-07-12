package xyz.nibblz.galapagos

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.ConstantIslandData
import xyz.nibblz.galapagos.features.BlueprintAssemblerInfo
import xyz.nibblz.galapagos.features.CoinTracking
import xyz.nibblz.galapagos.features.CosmeticMachineChances
import xyz.nibblz.galapagos.features.CraftingInstructions
import xyz.nibblz.galapagos.features.CrateChances
import xyz.nibblz.galapagos.features.ExchangeUnitPrice
import xyz.nibblz.galapagos.features.Feature
import xyz.nibblz.galapagos.features.QuestTracking
import xyz.nibblz.galapagos.util.GalapagosCommand
import xyz.nibblz.galapagos.util.PlayerData
import xyz.nibblz.galapagos.util.PlayerSave
import xyz.nibblz.galapagos.util.Save

object Galapagos : ModInitializer {
	const val MOD_ID: String = "galapagos"

	val logger: Logger = LoggerFactory.getLogger(MOD_ID)
	var save: PlayerSave = PlayerSave()
	val font = FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "main"))

	val features: List<Feature> = listOf(
		CoinTracking,
		QuestTracking,
		CrateChances,
		CosmeticMachineChances,
		ExchangeUnitPrice,
		CraftingInstructions,
		BlueprintAssemblerInfo
	)

	fun registerFeatures() {
		features.forEach { it.init() }
	}

	override fun onInitialize() {
		Config.handler.load()
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