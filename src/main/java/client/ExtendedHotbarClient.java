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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

    private boolean swapKeyWasDown;
    private boolean toggleKeyWasDown;
    private boolean fluentKeyWasDown;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(swapKeyBinding);
        KeyBindingHelper.registerKeyBinding(toggleKeyBinding);
        KeyBindingHelper.registerKeyBinding(fluentKeyBinding);

        Util.configHolder = AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        // Use "config" to hold state because it's simple
        Util.stateHolder = AutoConfig.register(ExtendedHotbarState.class, Toml4jConfigSerializer::new);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(final Minecraft client) {
        final ModConfig config = Util.configHolder.getConfig();

        // Edge detection via isDown() instead of consumeClick(). consumeClick() is fed by both
        // press AND repeat events, so holding a key would otherwise retrigger the action every tick.
        final boolean toggleDown = toggleKeyBinding.isDown();
        final boolean togglePressed = toggleDown && !this.toggleKeyWasDown;
        this.toggleKeyWasDown = toggleDown;

        final boolean fluentDown = fluentKeyBinding.isDown();
        final boolean fluentPressed = fluentDown && !this.fluentKeyWasDown;
        this.fluentKeyWasDown = fluentDown;

        final boolean swapDown = swapKeyBinding.isDown();
        final boolean swapPressed = swapDown && !this.swapKeyWasDown;
        this.swapKeyWasDown = swapDown;

        if (togglePressed) {
            config.enabled = !config.enabled;
            Util.configHolder.save();
            return;
        }

        if (fluentPressed) {
            config.fluent = !config.fluent;
            Util.configHolder.save();
            return;
        }

        if (!swapPressed || !config.enabled || config.fluent) {
            return;
        }

        if (client.level == null || client.screen != null || client.options.hideGui) {
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

}
