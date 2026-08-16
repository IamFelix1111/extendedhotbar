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

package com.extendedhotbar.mixin;

import com.extendedhotbar.Util;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

    @Shadow public Screen screen;
    @Shadow public HitResult hitResult;
    @Shadow public ClientLevel level;
    @Shadow public LocalPlayer player;
    @Shadow public Options options;

    @Unique private int hotbarKeyWasDownMask;

    @Inject(
        method = "pickBlock",
        at = @At("HEAD")
    )
    private void beforeDoItemPick(final CallbackInfo ci) {
        if (!Util.isEnabled()) {
            return;
        }

        if (this.screen != null) {
            return;
        }

        final HitResult target = this.hitResult;
        if (target == null || target.getType() != HitResult.Type.BLOCK) {
            return;
        }

        final BlockPos pos = ((BlockHitResult) target).getBlockPos();
        final BlockState blockState = this.level.getBlockState(pos);
        final Block block = blockState.getBlock();
        if (blockState.isAir()) {
            return;
        }

        // If the block is in the hotbar, we do nothing and let Minecraft do its thing
        final Inventory inventory = this.player.getInventory();
        for (int i = 0; i < 9; i++) {
            // While LEFT_HOTBAR_SLOT_INDEX is the base index for hotbar slots in the inventory gui, in Inventory,
            // the hotbar starts at 0
            final Item item = inventory.getItem(i).getItem();
            final Block blockFromItem = Block.byItem(item);

            if (block == blockFromItem) {
                return;
            }
        }

        // If the block is in the bottom row and not in the hotbar, we need to emulate Minecraft's default behavior
        // We do this by swapping the rows and then letting Minecraft go from there
        // It'll find the item in the hotbar and move the selection to that item accordingly
        for (int i = 0; i < 9; i++) {
            final Item item = inventory.getItem(i + Util.LEFT_BOTTOM_ROW_SLOT_INDEX).getItem();
            final Block blockFromItem = Block.byItem(item);

            if (block != blockFromItem) {
                continue;
            }

            if (Util.isFluent()) {
                Util.switchFluentPosition();
            }
            Util.performSwap((Minecraft) (Object) this, true);
            break;
        }
    }

    @Inject(
        method = "handleKeybinds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"
        )
    )
    private void handleKeybinds(final CallbackInfo ci, @Local(ordinal = 0) final int loopIndex) {
        if (!Util.configHolder.getConfig().enableDoubleTap || Util.isFluent()) {
            // In fluent mode, pressing a hotbar slot key just selects the slot; no swap.
            return;
        }

        // Edge detection: only swap on the initial press, not while the key is held.
        final KeyMapping keyMapping = this.options.keyHotbarSlots[loopIndex];
        final boolean down = keyMapping.isDown();
        final boolean pressed = down && (this.hotbarKeyWasDownMask & (1 << loopIndex)) == 0;
        if (down) {
            this.hotbarKeyWasDownMask |= 1 << loopIndex;
        } else {
            this.hotbarKeyWasDownMask &= ~(1 << loopIndex);
        }

        if (!pressed) {
            return;
        }

        if (this.player.getInventory().getSelectedSlot() == loopIndex) {
            Util.performSwap((Minecraft) (Object) this, false);
        }
    }
}
