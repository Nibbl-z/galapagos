package xyz.nibblz.galapagos.core

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.inventory.Slot
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.events.*
import xyz.nibblz.galapagos.screens.Intro
import xyz.nibblz.galapagos.util.onIsland
import xyz.nibblz.galapagos.util.sendGalapagosChatMessage
import kotlin.math.sin

object OOBE : CoreFeature {
    override fun init() {
        if (Galapagos.save.finishedOOBE) return

        JoinMCCIEvent.EVENT.register { closedInitialContainer = false }
        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(Galapagos.MOD_ID, "oobe"), hotbarOOBELayer())
        ClientTickEvents.END_CLIENT_TICK.register {
            if (openIntroScreen) {
                openIntroScreenDelay--
                if (openIntroScreenDelay > 0) return@register
                Minecraft.getInstance().setScreen(Intro())
                openIntroScreen = false
            }
            ticks++
        }
        ContainerOpenEvent.EVENT.register { containerOpen() }
        ContainerCloseEvent.EVENT.register { containerClose() }
        ContainerSetSlotEvent.EVENT.register { updateAPIToggleState() }
        ContainerRenderEvent.EVENT.register { _, graphics, x, y, w, _ -> containerRender(graphics, x, y, w) }
        SlotRenderEvent.EVENT.register { graphics, slot -> slotRender(graphics, slot) }
    }

    enum class OOBEState {
        JOIN,
        NONE,
        POCKET_MENU,
        SETTINGS,
        API_SETTINGS,
        API_SETTINGS_GOOD,
        SET_API_KEY,
        JOIN_STYLE_PERKS,
        POCKET_MENU_STYLE_PERKS,
        PROFILE,
        COSMETIC_COLLECTION,
        STYLE_PERKS
    }

    var active = true
    var closedInitialContainer = false
    var openIntroScreen = false
    var openIntroScreenDelay = 5
    var state: OOBEState = OOBEState.JOIN
    var ticks = 0
    var collectionEnabled = false
    var infinibagEnabled = false
    var statisticsEnabled = false
    var finishedApiSettings = false
    var hasAPIKey = false

    fun updateAPIToggleState() {
        if (!active) return

        val statisticsSlot = Minecraft.getInstance().player?.containerMenu?.getSlot(22) ?: return
        val collectionSlot = Minecraft.getInstance().player?.containerMenu?.getSlot(17) ?: return
        val infinibagSlot = Minecraft.getInstance().player?.containerMenu?.getSlot(35) ?: return

        if (state == OOBEState.API_SETTINGS) {
            val statisticsToggle = statisticsSlot.item.get(DataComponents.ITEM_MODEL)
            val collectionToggle = collectionSlot.item.get(DataComponents.ITEM_MODEL)
            val infinibagToggle = infinibagSlot.item.get(DataComponents.ITEM_MODEL)

            statisticsEnabled = statisticsToggle?.path?.contains("toggle_on") == true
            collectionEnabled = collectionToggle?.path?.contains("toggle_on") == true
            infinibagEnabled = infinibagToggle?.path?.contains("toggle_on") == true

            state = if (collectionEnabled && infinibagEnabled && statisticsEnabled) OOBEState.API_SETTINGS_GOOD else OOBEState.API_SETTINGS
        }
    }

    fun containerOpen() {
        if (!active) return
        val screen = Minecraft.getInstance().screen ?: return

        if (!finishedApiSettings) {
            state = when {
                screen.title.string.contains("POCKET MENU") -> OOBEState.POCKET_MENU
                screen.title.string.contains("SETTINGS") && !screen.title.string.contains("API") -> OOBEState.SETTINGS
                screen.title.string.contains("SETTINGS") && screen.title.string.contains("API") -> OOBEState.API_SETTINGS
                else -> OOBEState.NONE
            }
        } else {
            state = when {
                screen.title.string.contains("POCKET MENU") -> OOBEState.POCKET_MENU_STYLE_PERKS
                screen.title.string.contains("MY PROFILE") && !screen.title.string.contains("COSMETIC COLLECTION") -> OOBEState.PROFILE
                screen.title.string.contains("MY PROFILE") && screen.title.string.contains("COSMETIC COLLECTION") -> OOBEState.COSMETIC_COLLECTION
                screen.title.string.contains("STYLE PERKS") -> OOBEState.STYLE_PERKS
                else -> OOBEState.NONE
            }
        }

        if (state == OOBEState.API_SETTINGS) {
            updateAPIToggleState()
        }
    }

    fun containerClose() {
        if (!active) return

        if (!closedInitialContainer) {
            closedInitialContainer = true
            openIntroScreen = true
            openIntroScreenDelay = 5
        }

        if (state == OOBEState.STYLE_PERKS) {
            state = OOBEState.NONE
            active = false
            Galapagos.save.finishedOOBE = true
        }

        if (state == OOBEState.API_SETTINGS_GOOD && !hasAPIKey) {
            sendGalapagosChatMessage(
                Component.literal("It may take a few minutes for API changes to go into effect. If an error occurs, run /galapagos api manualFetch after a few minutes!")
                    .withColor(ChatFormatting.AQUA.color!!)
            )
            PlayerData.fetchAPI()
            finishedApiSettings = true
            state = OOBEState.JOIN_STYLE_PERKS
            return
        }

        if (state == OOBEState.SET_API_KEY) return

        state = if (state == OOBEState.API_SETTINGS_GOOD) OOBEState.SET_API_KEY else OOBEState.JOIN
    }

    fun containerRender(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int) {
        if (!active) return
        when (state) {
            OOBEState.POCKET_MENU,
            OOBEState.SETTINGS,
            OOBEState.API_SETTINGS,
            OOBEState.API_SETTINGS_GOOD,
            OOBEState.POCKET_MENU_STYLE_PERKS,
            OOBEState.PROFILE,
            OOBEState.COSMETIC_COLLECTION,
            OOBEState.STYLE_PERKS -> {
                graphics.fill(x, y - 60, x + w, y - 30, ARGB.color(0.5f, 0x000000))
            }
            else -> {}
        }

        when (state) {
            OOBEState.POCKET_MENU -> {
                graphics.text(Minecraft.getInstance().font, "Navigate to", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                graphics.text(Minecraft.getInstance().font, "Settings...", x + 70, y - 53, ARGB.opaque(ChatFormatting.AQUA.color!!))
            }
            OOBEState.SETTINGS -> {
                graphics.text(Minecraft.getInstance().font, "Navigate to", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                graphics.text(
                    Minecraft.getInstance().font, "API Settings...", x + 70, y - 53, ARGB.opaque(
                        ChatFormatting.AQUA.color!!))
            }
            OOBEState.API_SETTINGS, OOBEState.API_SETTINGS_GOOD -> {
                when {
                    // YYY
                    collectionEnabled && infinibagEnabled && statisticsEnabled -> {
                        graphics.text(Minecraft.getInstance().font, "Good! Everything is", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(Minecraft.getInstance().font, "enabled! Now, close this menu...", x + 10, y - 43, ARGB.opaque(0xFFFFFF))
                    }

                    // YNN
                    collectionEnabled && !infinibagEnabled && !statisticsEnabled -> {
                        graphics.text(Minecraft.getInstance().font, "Enable ", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(
                            Minecraft.getInstance().font, "Infinibag and Statistics...", x + 47, y - 53, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                    }

                    //NYN
                    !collectionEnabled && infinibagEnabled && !statisticsEnabled -> {
                        graphics.text(Minecraft.getInstance().font, "Enable ", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(
                            Minecraft.getInstance().font, "Collections and ", x + 47, y - 53, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                        graphics.text(
                            Minecraft.getInstance().font, "Statistics...", x + 10, y - 43, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                    }

                    //NNY
                    !collectionEnabled && !infinibagEnabled && statisticsEnabled -> {
                        graphics.text(Minecraft.getInstance().font, "Enable ", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(
                            Minecraft.getInstance().font, "Collections and", x + 47, y - 53, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                        graphics.text(
                            Minecraft.getInstance().font, "Infinibag...", x + 10, y - 43, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                    }

                    //YNY
                    collectionEnabled && !infinibagEnabled && statisticsEnabled -> {
                        graphics.text(Minecraft.getInstance().font, "Enable ", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(
                            Minecraft.getInstance().font, "Infinibag...", x + 47, y - 53, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                    }
                    //NYY
                    !collectionEnabled && infinibagEnabled && statisticsEnabled -> {
                        graphics.text(Minecraft.getInstance().font, "Enable ", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(
                            Minecraft.getInstance().font, "Collections...", x + 47, y - 53, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                    }
                    //YYN
                    collectionEnabled && infinibagEnabled && !statisticsEnabled -> {
                        graphics.text(Minecraft.getInstance().font, "Enable ", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(
                            Minecraft.getInstance().font, "Statistics...", x + 47, y - 53, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                    }

                    else -> {
                        graphics.text(Minecraft.getInstance().font, "Enable ", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                        graphics.text(
                            Minecraft.getInstance().font, "Collections,", x + 47, y - 53, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                        graphics.text(
                            Minecraft.getInstance().font, "Infinibag, and Statistics...", x + 10, y - 43, ARGB.opaque(
                                ChatFormatting.AQUA.color!!))
                    }
                }
            }
            OOBEState.POCKET_MENU_STYLE_PERKS -> {
                graphics.text(Minecraft.getInstance().font, "Navigate to", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                graphics.text(Minecraft.getInstance().font, "My Profile...", x + 70, y - 53, ARGB.opaque(ChatFormatting.AQUA.color!!))
            }
            OOBEState.PROFILE -> {
                graphics.text(Minecraft.getInstance().font, "Navigate to", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                graphics.text(Minecraft.getInstance().font, "Cosmetic Collection...", x + 70, y - 53, ARGB.opaque(ChatFormatting.AQUA.color!!))
            }
            OOBEState.COSMETIC_COLLECTION -> {
                graphics.text(Minecraft.getInstance().font, "Navigate to", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                graphics.text(Minecraft.getInstance().font, "Style Perks...", x + 70, y - 53, ARGB.opaque(ChatFormatting.AQUA.color!!))
            }
            OOBEState.STYLE_PERKS -> {
                graphics.text(Minecraft.getInstance().font, "Everything is now set up!", x + 10, y - 53, ARGB.opaque(0xFFFFFF))
                graphics.text(Minecraft.getInstance().font, "You're good to go!", x + 10, y - 43, ARGB.opaque(ChatFormatting.AQUA.color!!))
            }
            else -> {}
        }
    }

    fun slotRender(graphics: GuiGraphicsExtractor, slot: Slot) {
        if (!active) return

        when (state) {
            OOBEState.POCKET_MENU -> {
                if (slot.item.itemName.string != "Settings") return
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/select.png"),
                    slot.x - 8, slot.y - 8,
                    0f, 0f, 32, 32, 32, 32
                )
            }
            OOBEState.SETTINGS -> {
                if (slot.item.itemName.string != "API Settings") return
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/select.png"),
                    slot.x - 8, slot.y - 8,
                    0f, 0f, 32, 32, 32, 32
                )
            }
            OOBEState.API_SETTINGS -> {
                if (slot.index != 14 && slot.index != 32 && slot.index != 19) return
                if (slot.index == 14 && collectionEnabled) return
                if (slot.index == 32 && infinibagEnabled) return
                if (slot.index == 19 && statisticsEnabled) return

                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/select.png"),
                    slot.x - 8, slot.y - 8,
                    0f, 0f, 32, 32, 32, 32
                )
            }
            OOBEState.POCKET_MENU_STYLE_PERKS -> {
                if (slot.item.itemName.string != "My Profile") return
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/select.png"),
                    slot.x - 8, slot.y - 8,
                    0f, 0f, 32, 32, 32, 32
                )
            }
            OOBEState.PROFILE -> {
                if (slot.item.itemName.string != "Cosmetic Collection") return
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/select.png"),
                    slot.x - 8, slot.y - 8,
                    0f, 0f, 32, 32, 32, 32
                )
            }
            OOBEState.COSMETIC_COLLECTION -> {
                if (slot.item.itemName.string != "Style Perks") return
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/select.png"),
                    slot.x - 8, slot.y - 8,
                    0f, 0f, 32, 32, 32, 32
                )
            }
            else -> {}
        }
    }

    fun hotbarOOBELayer(): HudElement {
        return element@{ graphics, _ ->
            if (!onIsland()) return@element

            if (!active) return@element

            when (state) {
                OOBEState.JOIN -> {
                    graphics.fill(10, 10, 340, 50, ARGB.color(0.5f, 0x000000))
                    graphics.text(Minecraft.getInstance().font, "Let's make sure you have the required API features enabled!", 20, 20, ARGB.opaque(0xFFFFFF))
                    graphics.text(Minecraft.getInstance().font, "Navigate to the ", 20, 35, ARGB.opaque(0xFFFFFF))
                    graphics.text(Minecraft.getInstance().font, "Pocket Menu", 100, 35, ARGB.opaque(ChatFormatting.AQUA.color!!))
                    graphics.text(Minecraft.getInstance().font, "in your hotbar.", 165, 35, ARGB.opaque(0xFFFFFF))

                    graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/back.png"),
                        graphics.guiWidth() / 2 + 93 + (sin(ticks / 3.0) * 5).toInt(), graphics.guiHeight() - 19,
                        0f, 0f, 16, 16, 16, 16
                    )
                }
                OOBEState.SET_API_KEY -> {
                    graphics.fill(10, 10, 340, 50, ARGB.color(0.5f, 0x000000))
                    graphics.text(Minecraft.getInstance().font, "Next, set your API key by running", 20, 20, ARGB.opaque(0xFFFFFF))
                    graphics.text(
                        Minecraft.getInstance().font, "/galapagos api set <API_KEY>!", 20, 35, ARGB.opaque(
                            ChatFormatting.AQUA.color!!))
                }
                OOBEState.JOIN_STYLE_PERKS -> {
                    graphics.fill(10, 10, 340, 50, ARGB.color(0.5f, 0x000000))
                    graphics.text(Minecraft.getInstance().font, "Next, Galapagos needs to know your Style Perk levels!", 20, 20, ARGB.opaque(0xFFFFFF))
                    graphics.text(Minecraft.getInstance().font, "Navigate to the ", 20, 35, ARGB.opaque(0xFFFFFF))
                    graphics.text(Minecraft.getInstance().font, "Pocket Menu", 100, 35, ARGB.opaque(ChatFormatting.AQUA.color!!))
                    graphics.text(Minecraft.getInstance().font, "in your hotbar.", 165, 35, ARGB.opaque(0xFFFFFF))

                    graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/generic/back.png"),
                        graphics.guiWidth() / 2 + 93 + (sin(ticks / 3.0) * 5).toInt(), graphics.guiHeight() - 19,
                        0f, 0f, 16, 16, 16, 16
                    )
                }
                else -> {}
            }


        }
    }
}