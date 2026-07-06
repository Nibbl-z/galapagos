package xyz.nibblz.galapagos.dialogs

import com.noxcrew.sheeplib.theme.DefaultTheme
import com.noxcrew.sheeplib.theme.Theme
import com.noxcrew.sheeplib.util.opacity

object GalapagosTheme : Theme by DefaultTheme {
    override val theme: Theme = this
    override val colors = object : Theme.Colors by DefaultTheme.colors {
        override val dialogBackgroundAlt = 0x000000 opacity 200
    }
}