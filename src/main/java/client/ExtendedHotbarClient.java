/*
 * This file is part of ExtendedHotbar, a FabricMC mod.
 * Copyright (C) 2023 Kyle Wood (DenWav)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.extendedhotbar.client;

import com.extendedhotbar.ExtendedHotbarState;
import com.extendedhotbar.ModConfig;
import com.extendedhotbar.Util;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.resources.Identifier;

import static org.lwjgl.glfw.GLFW.*;

@Environment(EnvType.CLIENT)
public class ExtendedHotbarClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("extendedhotbar", "extendedhotbar"));

    private static final KeyMapping swapKeyBinding = new KeyMapping(
        "key.extendedhotbar.switch",
        InputConstants.Type.KEYSYM,
        GLFW_KEY_R,
        CATEGORY
    );

    private static final KeyMapping toggleKeyBinding = new KeyMapping(
        "key.extendedhotbar.toggle",
        InputConstants.Type.KEYSYM,
        GLFW_KEY_EQUAL,
        CATEGORY
    );

    private static final KeyMapping fluentKeyBinding = new KeyMapping(
        "key.extendedhotbar.fluent",
        InputConstants.Type.KEYSYM,
        GLFW_KEY_V,
        CATEGORY
    );

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(swapKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleKeyBinding);
        KeyBindingHelper.registerKeyBinding(fluentKeyBinding);

        Util.configHolder = AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        // Use "config" to hold state because it's simple
        Util.stateHolder = AutoConfig.register(ExtendedHotbarState.class, Toml4jConfigSerializer::new);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ScreenEvents.BEFORE_INIT.register(this::onScreenOpen);
    }

    private void onTick(final Minecraft client) {
        final ModConfig config = Util.configHolder.getConfig();
        if (toggleKeyBinding.consumeClick()) {
            config.enabled = !config.enabled;
            Util.configHolder.save();
            return;
        }

        if (fluentKeyBinding.consumeClick()) {
            config.fluent = !config.fluent;
            Util.configHolder.save();
            return;
        }

        if (!config.enabled || config.fluent) {
            return;
        }

        if (client.level == null || client.screen != null || client.options.hideGui) {
            return;
        }

        if (!swapKeyBinding.consumeClick()) {
            return;
        }

        boolean singleSwap;
        if (config.enableModifier) {
            final long window = client.getWindow().handle();
            singleSwap = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) != GLFW_PRESS && glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) != GLFW_PRESS;

            if (config.invert) {
                singleSwap = !singleSwap;
            }
        } else {
            singleSwap = !config.invert;
        }

        Util.performSwap(client, singleSwap);
    }

    private void onScreenOpen(
        final Minecraft client,
        final Screen screen,
        final int scaledWidth,
        final int scaledHeight
    ) {
        if (!(screen instanceof AbstractContainerScreen<?>) && !(screen instanceof HorseInventoryScreen)) {
            return;
        }
        final MultiPlayerGameMode manager = client.gameMode;
        if (manager != null && client.player != null && client.player.hasInfiniteMaterials()) {
            if (!(screen instanceof CreativeModeInventoryScreen)) {
                // Creative inventories are opened after the normal inventory is opened, so we want to ignore when
                // the first one closes (the non-creative inventory).
                // It goes setScreen(InventoryScreen) -> InventoryScreen.init() -> setScreen(CreativeInventoryScreen)
                return;
            }
        }
        ScreenEvents.remove(screen).register(this::onScreenClose);
    }

    private void onScreenClose(final Screen screen) {
        if (Util.isRenderSwapped()) {
            // swap back
            Util.resetRenderedPosition();
            Util.performSwap(Minecraft.getInstance(), true);
        }
    }
}
