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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Creative-mode fixes for the fluent swapped hotbar:
 * <ul>
 *   <li>item-grid tab hotbar clicks act on the displayed item;</li>
 *   <li>Inventory-tab drops act on the displayed (paired) item and sync the correct server slot.</li>
 * </ul>
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreenFluent {
    @WrapOperation(
        method = "slotClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"
        )
    )
    private void redirectItemGridHotbarClick(
        final CreativeModeInventoryScreen.ItemPickerMenu menu,
        final int i,
        final int j,
        final ClickType clickType,
        final Player player,
        final Operation<Void> original,
        @Local(argsOnly = true, ordinal = 0) final Slot slot
    ) {
        if (clickType != ClickType.QUICK_CRAFT
            && slot != null
            && slot.container instanceof Inventory
            && extendedhotbar$shouldSwap()) {
            final int containerIndex = slot.getContainerSlot();
            if (containerIndex >= 0 && containerIndex < 9) {
                player.inventoryMenu.clicked(27 + containerIndex, j, clickType, player);
                return;
            }
        }

        original.call(menu, i, j, clickType, player);
    }

    /**
     * The Inventory-tab THROW branch removes directly from the physical slot, which holds the
     * swapped-in item. Redirect the removal to the paired slot so the displayed item is dropped.
     */
    @WrapOperation(
        method = "slotClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;remove(I)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack redirectDropRemove(final Slot slot, final int amount, final Operation<ItemStack> original) {
        if (extendedhotbar$shouldSwap()) {
            final Slot paired = extendedhotbar$getPairedWrapper(slot);
            if (paired != null) {
                return original.call(paired, amount);
            }
        }
        return original.call(slot, amount);
    }

    /**
     * The Inventory-tab THROW branch then syncs the remaining stack back to the server using the
     * wrapper's own slot index. Redirect it to the paired slot's remaining stack and server index.
     */
    @WrapOperation(
        method = "slotClicked",
        at = @At(
            value = "INVOKE",
            ordinal = 1,
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleCreativeModeItemAdd(Lnet/minecraft/world/item/ItemStack;I)V"
        )
    )
    private void redirectDropSync(
        final MultiPlayerGameMode gameMode,
        final ItemStack itemStack,
        final int slotIndex,
        final Operation<Void> original,
        @Local(argsOnly = true, ordinal = 0) final Slot slot
    ) {
        if (extendedhotbar$shouldSwap()) {
            final Slot paired = extendedhotbar$getPairedWrapper(slot);
            if (paired != null) {
                original.call(gameMode, paired.getItem(), paired.getContainerSlot());
                return;
            }
        }
        original.call(gameMode, itemStack, slotIndex);
    }

    @Unique
    private static boolean extendedhotbar$shouldSwap() {
        final Minecraft minecraft = Minecraft.getInstance();
        return Util.isFluent()
            && Util.getFluentPosition() == Position.RIGHT
            && minecraft.player != null;
    }

    /**
     * In the creative Inventory tab slots are SlotWrappers around the real inventory menu, whose
     * container-slot index is the survival menu index (hotbar 36-44, bottom row 27-35).
     */
    @Unique
    private Slot extendedhotbar$getPairedWrapper(final Slot slot) {
        if (slot == null
            || !(slot.container instanceof Inventory)
            || !(extendedhotbar$getScreenMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu)) {
            return null;
        }

        final int index = slot.getContainerSlot();
        final int pairedIndex;
        if (index >= 36 && index < 45) {
            pairedIndex = index - 9;
        } else if (index >= 27 && index < 36) {
            pairedIndex = index + 9;
        } else {
            return null;
        }

        final AbstractContainerMenu menu = extendedhotbar$getScreenMenu();
        for (final Slot candidate : menu.slots) {
            if (candidate.container == slot.container && candidate.getContainerSlot() == pairedIndex) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * The `menu` field is inherited from AbstractContainerScreen. Rather than @Shadow (which
     * cannot resolve inherited fields), call the inherited getMenu().
     */
    @Unique
    private AbstractContainerMenu extendedhotbar$getScreenMenu() {
        return ((AbstractContainerScreen<?>) (Object) this).getMenu();
    }
}
