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

package com.extendedhotbar.mixin.swapping;

import com.extendedhotbar.Util;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinInGameHudSwapping {
    @Unique private boolean extendedHotbar$shifted;

    @WrapOperation(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        )
    )
    private void drawTopHotbarBackground(
        final GuiGraphics guiGraphics,
        final RenderPipeline renderPipeline,
        final Identifier texture,
        final int x,
        final int y,
        final int width,
        final int height,
        final Operation<Void> original
    ) {
        original.call(guiGraphics, renderPipeline, texture, x, y, width, height);

        if (Util.isSwappingEnabled())
            guiGraphics.blitSprite(renderPipeline, texture, x, y + Util.DISTANCE, width, height);
    }

    @WrapOperation(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"
        )
    )
    private void drawTopHotbarItem(
        final Gui instance,
        final GuiGraphics guiGraphics,
        final int x,
        final int y,
        final DeltaTracker deltaTracker,
        final Player player,
        final ItemStack itemStack,
        final int seed,
        final Operation<Void> original,
        @Local(ordinal = 4) final int loopIndex
    ) {
        original.call(instance, guiGraphics, x, y, deltaTracker, player, itemStack, seed);

        if (Util.isSwappingEnabled())
            original.call(instance, guiGraphics, x, y + Util.DISTANCE, deltaTracker, player, player.getInventory().getItem(loopIndex + Util.SLOT_OFFSET), seed);
    }

    @Inject(
        method = "renderOverlayMessage",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;"
        )
    )
    private void moveActionBarTextUp(final CallbackInfo ci, @Local(argsOnly = true) final GuiGraphics guiGraphics) {
        if (Util.isSwappingEnabled())
            guiGraphics.pose().translate(0, Util.DISTANCE);
    }

    @Inject(
        method = "renderHotbarAndDecorations",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/Gui;renderItemHotbar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void moveHudUp(final CallbackInfo ci, @Local(argsOnly = true) final GuiGraphics guiGraphics) {
        this.extendedHotbar$shifted = false;
        if (Util.isSwappingEnabled()) {
            Util.moveUp(guiGraphics.pose());
            this.extendedHotbar$shifted = true;
        }
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
    private void moveHudDown(final CallbackInfo ci, @Local(argsOnly = true) final GuiGraphics guiGraphics) {
        if (this.extendedHotbar$shifted) {
            Util.reset(guiGraphics.pose());
            this.extendedHotbar$shifted = false;
        }
    }
}
