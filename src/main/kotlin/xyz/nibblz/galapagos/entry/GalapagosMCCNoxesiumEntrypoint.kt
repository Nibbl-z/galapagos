package xyz.nibblz.galapagos.entry

import com.noxcrew.noxesium.core.fabric.mcc.MccNoxesiumEntrypoint
import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccServerPacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import com.noxcrew.noxesium.core.mcc.MccPackets
import xyz.nibblz.galapagos.events.MCCGameStateEvent
import xyz.nibblz.galapagos.events.MCCServerEvent
import xyz.nibblz.galapagos.events.MCCStatisticEvent

class GalapagosMCCNoxesiumEntrypoint : MccNoxesiumEntrypoint() {
    override fun initialize() {
        MccPackets.CLIENTBOUND_MCC_STATISTIC.addListener(
            this, ClientboundMccStatisticPacket::class.java
        ) { _, packet, _ ->
            MCCStatisticEvent.EVENT.invoker().invoke(packet)
        }

        MccPackets.CLIENTBOUND_MCC_SERVER.addListener(
            this, ClientboundMccServerPacket::class.java
        ) { _, packet, _ ->
            MCCServerEvent.EVENT.invoker().invoke(packet)
        }

        MccPackets.CLIENTBOUND_MCC_GAME_STATE.addListener(
            this, ClientboundMccGameStatePacket::class.java
        ) { _, packet, _ ->
            MCCGameStateEvent.EVENT.invoker().invoke(packet)
        }
    }
}