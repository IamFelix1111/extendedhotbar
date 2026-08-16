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

package com.extendedhotbar;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import org.joml.Matrix3x2fStack;

public final class Util {

    public static final int LEFT_BOTTOM_ROW_SLOT_INDEX = 27;

    private static final int LEFT_HOTBAR_SLOT_INDEX = 36;
    private static final int BOTTOM_RIGHT_CRAFTING_SLOT_INDEX = 4;

    public static final int DISTANCE = -22;

    public static final int SLOT_OFFSET = LEFT_BOTTOM_ROW_SLOT_INDEX;

    public static ConfigHolder<ModConfig> configHolder = null;

    public static ConfigHolder<ExtendedHotbarState> stateHolder = null;

    private static boolean swapRender = false;

    private Util() {}

    public static boolean isEnabled() {
        return configHolder != null && configHolder.getConfig().enabled;
    }

    public static boolean isSwappingEnabled() {
        if (configHolder == null) {
            return false;
        }
        final ModConfig config = configHolder.getConfig();
        return config.enabled && !config.fluent;
    }

    public static boolean isFluent() {
        if (configHolder == null) {
            return false;
        }
        final ModConfig config = configHolder.getConfig();
        return config.enabled && config.fluent;
    }

    public static ExtendedHotbarState.Position getFluentPosition() {
        if (stateHolder == null) {
            return ExtendedHotbarState.Position.LEFT;
        }
        return stateHolder.getConfig().position;
    }

    public static ExtendedHotbarState.Position getRenderedFluentPosition() {
        if (stateHolder == null) {
            return ExtendedHotbarState.Position.LEFT;
        }
        final ExtendedHotbarState state = stateHolder.getConfig();
        if (swapRender) {
            return switch (state.position) {
                case LEFT -> ExtendedHotbarState.Position.RIGHT ;
                case RIGHT -> ExtendedHotbarState.Position.LEFT;
            };
        } else {
            return state.position;
        }
    }

    public static void swapRenderedPosition() {
        swapRender = true;
    }

    public static boolean isRenderSwapped() {
        return swapRender;
    }

    public static void resetRenderedPosition() {
        swapRender = false;
    }

    public static void switchFluentPosition() {
        if (stateHolder == null) {
            return;
        }
        stateHolder.getConfig().position = switch (stateHolder.getConfig().position) {
            case LEFT -> ExtendedHotbarState.Position.RIGHT;
            case RIGHT -> ExtendedHotbarState.Position.LEFT;
        };
        stateHolder.save();
    }

    public static void moveUp(final Matrix3x2fStack poseStack) {
        poseStack.pushMatrix();
        poseStack.translate(0, DISTANCE);
    }

    public static void reset(final Matrix3x2fStack poseStack) {
        poseStack.popMatrix();
    }

    public static void performSwap(final Minecraft client, final boolean fullRow) {
        final LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        final InventoryScreen inventory = new InventoryScreen(player);
        final int syncId = inventory.getMenu().containerId;

        if (fullRow) {
            swapRows(client, syncId);
        } else {
            final MultiPlayerGameMode interactionManager = client.gameMode;
            if (interactionManager != null) {
                final int currentItem = player.getInventory().getSelectedSlot();
                swapItem(interactionManager, player, syncId, currentItem);
            }
        }
    }

    private static void swapRows(final Minecraft client, final int syncId) {
        final MultiPlayerGameMode interactionManager = client.gameMode;
        final LocalPlayer player = client.player;
        if (interactionManager == null || player == null)  {
            return;
        }

        for (int i = 0; i < 9; i++) {
            swapItem(interactionManager, player, syncId, i);
        }
    }

    private static void swapItem(
        final MultiPlayerGameMode interactionManager,
        final LocalPlayer player,
        final int syncId,
        final int slotId
    ) {
        /*
         * Implementation note:
         * There are fancy click mechanisms to swap item stacks without using a temporary slot, but when swapping between two identical item
         * stacks, things can get messed up. Using a temporary slot that we know is guaranteed to be empty is the safest option.
         */

        // Move hotbar item to crafting slot
        interactionManager.handleInventoryMouseClick(syncId, slotId + Util.LEFT_HOTBAR_SLOT_INDEX, 0, ClickType.PICKUP, player);
        interactionManager.handleInventoryMouseClick(syncId, Util.BOTTOM_RIGHT_CRAFTING_SLOT_INDEX, 0, ClickType.PICKUP, player);
        // Move bottom row item to hotbar
        interactionManager.handleInventoryMouseClick(syncId, slotId + Util.LEFT_BOTTOM_ROW_SLOT_INDEX, 0, ClickType.PICKUP, player);
        interactionManager.handleInventoryMouseClick(syncId, slotId + Util.LEFT_HOTBAR_SLOT_INDEX, 0, ClickType.PICKUP, player);
        // Move crafting slot item to bottom row
        interactionManager.handleInventoryMouseClick(syncId, Util.BOTTOM_RIGHT_CRAFTING_SLOT_INDEX, 0, ClickType.PICKUP, player);
        interactionManager.handleInventoryMouseClick(syncId, slotId + Util.LEFT_BOTTOM_ROW_SLOT_INDEX, 0, ClickType.PICKUP, player);
    }
}
