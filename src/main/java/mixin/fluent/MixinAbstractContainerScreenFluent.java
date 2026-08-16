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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreenFluent {

    @Shadow protected AbstractContainerMenu menu;

    /**
     * In fluent mode, when the secondary hotbar is selected (position RIGHT), the hotbar row and
     * the bottom inventory row have been physically swapped. This swaps only what is rendered so
     * the inventory still shows items in their original rows, without touching slot interaction.
     */
    @WrapOperation(
        method = "renderSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;",
            ordinal = 0
        )
    )
    private ItemStack swapRenderedItem(final Slot slot, final Operation<ItemStack> original) {
        final ItemStack stack = original.call(slot);

        if (!Util.isFluent() || Util.getFluentPosition() != Position.RIGHT || !(this.menu instanceof InventoryMenu)) {
            return stack;
        }

        final int pairedIndex;
        if (slot.index >= 36 && slot.index < 45) {
            // Hotbar slot: pair with the bottom-row slot directly above it.
            pairedIndex = slot.index - 9;
        } else if (slot.index >= 27 && slot.index < 36) {
            // Bottom-row slot: pair with the hotbar slot directly below it.
            pairedIndex = slot.index + 9;
        } else {
            return stack;
        }

        final Slot paired = this.menu.getSlot(pairedIndex);
        return paired == null ? stack : paired.getItem();
    }
}
