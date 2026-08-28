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
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public final class Util {
    public static final int LEFT_BOTTOM_ROW_SLOT_INDEX = 27;

    public static final int DISTANCE = -22;

    public static final int SLOT_OFFSET = LEFT_BOTTOM_ROW_SLOT_INDEX;

    public static ConfigHolder<ModConfig> configHolder = null;

    public static ConfigHolder<ExtendedHotbarState> stateHolder = null;

    private Util() {}

    public static boolean isEnabled() {
        return configHolder != null && configHolder.getConfig().enabled;
    }

    public static boolean isSwappingEnabled() {
        if (configHolder == null)
            return false;
        final ModConfig config = configHolder.getConfig();
        return config.enabled && !config.fluent;
    }

    public static boolean isFluent() {
        if (configHolder == null)
            return false;
        final ModConfig config = configHolder.getConfig();
        return config.enabled && config.fluent;
    }

    public static ExtendedHotbarState.Position getFluentPosition() {
        if (stateHolder == null)
            return ExtendedHotbarState.Position.LEFT;
        return stateHolder.getConfig().position;
    }

    public static void switchFluentPosition() {
        if (stateHolder == null)
            return;
        stateHolder.getConfig().position = switch (stateHolder.getConfig().position) {
            case LEFT -> ExtendedHotbarState.Position.RIGHT;
            case RIGHT -> ExtendedHotbarState.Position.LEFT;
        };
        CompletableFuture.runAsync(stateHolder::save);
    }

    public static void moveUp(final @NonNull Matrix3x2fStack poseStack) {
        poseStack.pushMatrix();
        poseStack.translate(0, DISTANCE);
    }

    public static void reset(final @NonNull Matrix3x2fStack poseStack) {
        poseStack.popMatrix();
    }

    public static void performSwap(final @NonNull Minecraft client, final boolean fullRow) {
        final LocalPlayer player = client.player;
        if (player == null)
            return;

        final MultiPlayerGameMode interactionManager = client.gameMode;
        if (interactionManager == null)
            return;

        final int syncId = player.inventoryMenu.containerId;

        final AbstractContainerMenu original = player.containerMenu;
        player.containerMenu = player.inventoryMenu;
        try {
            if (fullRow)
                for (int i = 0; i < 9; i++)
                    swapItem(interactionManager, player, syncId, i);
            else
                swapItem(interactionManager, player, syncId, player.getInventory().getSelectedSlot());
        } finally {
            player.containerMenu = original;
        }
    }

    private static void swapItem(
        final @NonNull MultiPlayerGameMode interactionManager,
        final LocalPlayer player,
        final int syncId,
        final int slotId
    ) {
        interactionManager.handleInventoryMouseClick(syncId, slotId + Util.LEFT_BOTTOM_ROW_SLOT_INDEX, slotId, ClickType.SWAP, player);
    }
}
