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

import com.extendedhotbar.Util;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandlerFluent {
    @Shadow @Final private Minecraft minecraft;

    @WrapOperation(
        method = "onScroll",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"
        )
    )
    private int swapOnScroll(final double scrollAmount, final int oldSlot, final int slotCount, final Operation<Integer> original) {
        final int newSlot = original.call(scrollAmount, oldSlot, slotCount);

        if (Util.isFluent()) {
            final int direction = (int) Math.signum(scrollAmount);
            if ((oldSlot == 0 && direction > 0) || (oldSlot == slotCount - 1 && direction < 0)) {
                Util.switchFluentPosition();
                Util.performSwap(this.minecraft, true);
            }
        }

        return newSlot;
    }
}
