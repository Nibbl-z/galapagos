package xyz.nibblz.galapagos

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.ConstantIslandData
import xyz.nibblz.galapagos.events.JoinMCCIEvent
import xyz.nibblz.galapagos.features.*
import xyz.nibblz.galapagos.util.GalapagosCommand
import xyz.nibblz.galapagos.util.OOBE
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
		OOBE.init()

		JoinMCCIEvent.EVENT.register {
			if (!save.finishedOOBE) return@register
			PlayerData.fetchAPI()
		}

		ClientLifecycleEvents.CLIENT_STOPPING.register {onShutdown()}
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> GalapagosCommand.register(dispatcher) }

		logger.info("Galapagos initialized!")
	}

	private fun onShutdown() {
		Save.save()
	}

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}