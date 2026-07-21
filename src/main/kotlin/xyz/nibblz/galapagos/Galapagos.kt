package xyz.nibblz.galapagos

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.core.CoreFeature
import xyz.nibblz.galapagos.core.GalapagosCommand
import xyz.nibblz.galapagos.data.ConstantIslandData
import xyz.nibblz.galapagos.features.*
import xyz.nibblz.galapagos.core.OOBE
import xyz.nibblz.galapagos.core.PlayerData
import xyz.nibblz.galapagos.core.Save

object Galapagos : ModInitializer {
	const val MOD_ID: String = "galapagos"

	val logger: Logger = LoggerFactory.getLogger(MOD_ID)
	var save: Save.PlayerSave = Save.PlayerSave()
	val font = FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "main"))

	val features: List<Feature> = listOf(
		CoinTracking,
		QuestTracking,
		CrateChances,
		CosmeticMachineChances,
		ExchangeUnitPrice,
		CraftingInstructions,
		BlueprintAssemblerInfo,
		WeeklyVaultInfo
	)

	val coreFeatures: List<CoreFeature> = listOf(
		Save,
		PlayerData,
		OOBE,
		GalapagosCommand
	)

	fun registerFeatures() {
		features.forEach { it.init() }
		coreFeatures.forEach { it.init() }
	}

	override fun onInitialize() {
		Config.handler.load()
		registerFeatures()
		ConstantIslandData.load()

		ClientLifecycleEvents.CLIENT_STOPPING.register { onShutdown() }

		logger.info("Galapagos initialized!")
	}

	private fun onShutdown() {
		Save.save()
	}

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}