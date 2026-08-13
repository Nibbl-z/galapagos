package xyz.nibblz.galapagos.core.game_state_handlers

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

interface Handler {
    fun init() {}
    fun handleStatisticPacket(packet: ClientboundMccStatisticPacket) {}
    fun handleGameStatePacket(packet: ClientboundMccGameStatePacket) {}
    fun handleSystemChatPacket(packet:  ClientboundSystemChatPacket) {}
    fun update() {}
}
