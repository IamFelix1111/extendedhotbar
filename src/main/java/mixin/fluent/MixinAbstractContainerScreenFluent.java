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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreenFluent {
    @Final
    @Shadow protected AbstractContainerMenu menu;

    @Unique private boolean extendedhotbar$indexesSwapped;

    /**
     * When the secondary hotbar is selected (position RIGHT) the hotbar row and the bottom inventory row have been physically swapped.
     * While a container screen is open we swap the paired slots' `index` fields so every operation (click, shift-click, double-click, quick-craft, key press, number-key swap) acts as if the swap never happened.
     * Rendering is handled separately (see swapRenderedItem),
     * because it reads the item via the container index.
     * Creative uses a special slot-ID mapping (see swapIndexes).
     */
    @WrapOperation(
        method = "renderSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;",
            ordinal = 0
        )
    )
    private ItemStack swapRenderedItem(final Slot slot, final @NonNull Operation<ItemStack> original) {
        final ItemStack stack = original.call(slot);

        if (!extendedhotbar$shouldSwap())
            return stack;

        final Slot paired = extendedhotbar$getPairedSlot(this.menu, slot);
        return paired == null ? stack : paired.getItem();
    }

    @WrapOperation(
        method = "renderTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;hasItem()Z"
        )
    )
    private boolean renderedTooltipHasItem(final Slot slot, final Operation<Boolean> original) {
        if (!extendedhotbar$shouldSwap()) {
            return original.call(slot);
        }
        final Slot paired = extendedhotbar$getPairedSlot(this.menu, slot);
        return paired == null ? original.call(slot) : paired.hasItem();
    }

    @WrapOperation(
        method = "renderTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack renderedTooltipItem(final Slot slot, final Operation<ItemStack> original) {
        if (!extendedhotbar$shouldSwap()) {
            return original.call(slot);
        }
        final Slot paired = extendedhotbar$getPairedSlot(this.menu, slot);
        return paired == null ? original.call(slot) : paired.getItem();
    }

    /**
     * The drop/pick keys are gated on the physical `hoveredSlot.hasItem()`, but the slot renders
     * the paired item. Use the paired slot so dropping works even when the physical (other-hotbar)
     * slot is empty.
     */
    @WrapOperation(
        method = "keyPressed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;hasItem()Z"
        )
    )
    private boolean keyPressedHasItem(final Slot slot, final Operation<Boolean> original) {
        if (!extendedhotbar$shouldSwap()) {
            return original.call(slot);
        }
        final Slot paired = extendedhotbar$getPairedSlot(this.menu, slot);
        return paired == null ? original.call(slot) : paired.hasItem();
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(final CallbackInfo ci) {
        extendedhotbar$reconcile();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(final CallbackInfo ci) {
        extendedhotbar$reconcile();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(final CallbackInfo ci) {
        if (this.extendedhotbar$indexesSwapped) {
            extendedhotbar$swapIndexes();
            this.extendedhotbar$indexesSwapped = false;
        }
    }

    @Unique
    private void extendedhotbar$reconcile() {
        if (extendedhotbar$shouldSwap()) {
            if (!this.extendedhotbar$indexesSwapped) {
                extendedhotbar$swapIndexes();
                this.extendedhotbar$indexesSwapped = true;
            }
        } else if (this.extendedhotbar$indexesSwapped) {
            extendedhotbar$swapIndexes();
            this.extendedhotbar$indexesSwapped = false;
        }
    }

    @Unique
    private void extendedhotbar$swapIndexes() {
        final Minecraft minecraft = Minecraft.getInstance();
        final AbstractContainerMenu swapMenu;
        if (this.menu instanceof CreativeModeInventoryScreen.ItemPickerMenu && minecraft.player != null)
            swapMenu = minecraft.player.inventoryMenu;
        else
            swapMenu = this.menu;

        for (final Slot slot : swapMenu.slots) {
            if (!(slot.container instanceof Inventory))
                continue;

            final int containerIndex = slot.getContainerSlot();
            if (containerIndex < 0 || containerIndex >= 9)
                continue;

            final Slot paired = extendedhotbar$getPairedSlot(swapMenu, slot);
            if (paired == null)
                continue;

            final int tmp = slot.index;
            slot.index = paired.index;
            paired.index = tmp;
        }
    }

    @Unique
    private static boolean extendedhotbar$shouldSwap() {
        final Minecraft minecraft = Minecraft.getInstance();
        return Util.isFluent()
            && Util.getFluentPosition() == Position.RIGHT
            && minecraft.player != null;
    }

    @Unique
    private static Slot extendedhotbar$getPairedSlot(final AbstractContainerMenu menu, final Slot slot) {
        if (slot == null || !(slot.container instanceof Inventory))
            return null;

        final boolean creative = menu instanceof CreativeModeInventoryScreen.ItemPickerMenu;
        final int index = slot.getContainerSlot();
        final int pairedIndex;
        if (creative)
            if (index >= 36 && index < 45)
                pairedIndex = index - 9; // Inventory-tab hotbar wrapper
            else if (index >= 27 && index < 36)
                pairedIndex = index + 9; // Inventory-tab bottom-row wrapper
            else if (index >= 0 && index < 9 && !extendedhotbar$creativeMenuHasInventoryTabWrappers(menu))
                pairedIndex = index + 27; // item-grid-tab hotbar slot
            else
                return null; // Inventory-tab result/crafting/armor wrappers: not a hotbar
        else
            if (index >= 0 && index < 9)
                pairedIndex = index + 27;
            else if (index >= 27 && index < 36)
                pairedIndex = index - 27;
            else
                return null;

        final Slot found = extendedhotbar$findPaired(menu, slot, pairedIndex);
        if (found != null)
            return found;
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.inventoryMenu != menu)
            return extendedhotbar$findPaired(player.inventoryMenu, slot, pairedIndex);
        return null;
    }

    @Unique
    private static boolean extendedhotbar$creativeMenuHasInventoryTabWrappers(final AbstractContainerMenu menu) {
        // In the creative Inventory tab every real inventory-menu slot is wrapped, so its
        // container-slot index is a menu index and the hotbar wrappers are 36-44. In the
        // item-grid tabs the only Inventory-container slots are the plain hotbar slots (0-8).
        for (final Slot candidate : menu.slots)
            if (candidate.container instanceof Inventory) {
                final int i = candidate.getContainerSlot();
                if (i >= 36 && i < 45)
                    return true;
            }
        return false;
    }

    @Unique
    private static Slot extendedhotbar$findPaired(final AbstractContainerMenu menu, final Slot slot, final int pairedIndex) {
        for (final Slot candidate : menu.slots)
            if (candidate.container == slot.container && candidate.getContainerSlot() == pairedIndex)
                return candidate;
        return null;
    }
}
