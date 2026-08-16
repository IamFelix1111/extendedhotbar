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

package com.extendedhotbar.mixin.fluent;

import com.extendedhotbar.ExtendedHotbarState.Position;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public abstract class MixinInGameHudFluent {

    @Unique private int offset;

    @WrapOperation(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        )
    )
    private void drawExtraHotbarBackground(
        final GuiGraphics guiGraphics,
        final RenderPipeline renderPipeline,
        final Identifier texture,
        final int x,
        final int y,
        final int width,
        final int height,
        final Operation<Void> original
    ) {
        if (!Util.isFluent()) {
            this.offset = 0;
            original.call(guiGraphics, renderPipeline, texture, x, y, width, height);
            return;
        }

        this.offset = width / 2;

        original.call(guiGraphics, renderPipeline, texture, x - this.offset, y, width, height);
        guiGraphics.blitSprite(renderPipeline, texture, x + this.offset, y, width, height);
    }

    @ModifyArg(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 1,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        ),
        index = 2
    )
    private int drawHotbarSelection(final int x) {
        if (this.offset == 0) {
            return x;
        }

        final Position position = Util.getFluentPosition();
        return switch (position) {
            case LEFT -> x - this.offset;
            case RIGHT -> x + this.offset;
        };
    }

    @WrapOperation(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"
        )
    )
    private void drawExtraHotbarItem(
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
        if (this.offset == 0) {
            original.call(instance, guiGraphics, x, y, deltaTracker, player, itemStack, seed);
            return;
        }

        final Position position = Util.getFluentPosition();

        final int originalX;
        final int newX;
        switch (position) {
            case LEFT -> {
                originalX = x - this.offset;
                newX = x + this.offset;
            }
            case RIGHT -> {
                originalX = x + this.offset;
                newX = x - this.offset;
            }
            default -> throw new IllegalStateException("unknown position");
        }

        original.call(instance, guiGraphics, originalX, y, deltaTracker, player, itemStack, seed);
        original.call(instance, guiGraphics, newX, y, deltaTracker, player, player.getInventory().getItem(loopIndex + Util.SLOT_OFFSET), seed);
    }

    @ModifyArg(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 2,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        ),
        index = 2
    )
    private int drawOffhandItemBackgroundLeft(final int x) {
        return x - this.offset;
    }

    @ModifyArg(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 1,
            target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"
        ),
        index = 1
    )
    private int drawOffhandItemLeft(final int x) {
        return x - this.offset;
    }

    @ModifyArg(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 3,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        ),
        index = 2
    )
    private int drawOffhandItemBackgroundRight(final int x) {
        return x + this.offset;
    }

    @ModifyArg(
        method = "renderItemHotbar",
        at = @At(
            value = "INVOKE",
            ordinal = 2,
            target = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"
        ),
        index = 1
    )
    private int drawOffhandItemRight(final int x) {
        return x - this.offset;
    }
}
