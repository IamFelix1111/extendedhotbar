Extended Hotbar
===============

![GIF shows the hotbar swapping mechanic, with both full and individual item swaps.](example.gif "Mod Example GIF")

Top bar is the bottom bar of the player's inventory.

* Press `R` to swap the rows
* Press `Shift+R` to swap only the items in your hand. (Disabled by default)
* Press the key of your currently selected hotbar slot again to swap only that slot. (Disabled by default, via the "re-press to swap" option)
* Press `=` to disable/enable
* Press `V` to enable/disable fluent mode

Hotkeys are configurable in the standard Minecraft controls settings.
Key actions are edge-triggered, so holding a key down won't repeat the action.

Additional configuration is in the mod's menu page. 
[ModMenu](https://www.curseforge.com/minecraft/mc-mods/modmenu) is required for the extra configuration.

## Fluent Mode

Fluent mode can be enabled in the mod's config menu (using ModMenu) or by pressing the `V` key. 
This disables manual swapping using the `R` key, 
and instead does the swapping transparently as the user scrolls along a double-wide hotbar, 
created by rendering the second hotbar alongside the first, rather than on top.

While the secondary hotbar is selected, opening the inventory shows your items in their original
rows (visual only — slot interaction is unaffected), and the normal hotbar is restored when you
close the inventory.

![GIF shows the fluent extended hotbar mechanic.](fluent.gif "Mod Example Fluent GIF")

## Requirements

### Required

- [Fabric Loader](https://fabricmc.net/) 0.19.3 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Minecraft](https://www.minecraft.net/) 1.21.11

### Optional

- [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) — adds the in-game configuration screen

### Bundled

- [Cloth Config](https://modrinth.com/mod/cloth-config) — already bundled inside the mod jar, no separate installation needed

## Credits

Maintained by [IamFelix](https://github.com/IamFelix1111).

Original author: [DenWav](https://github.com/DenWav).

Source repository: [IamFelix1111/extendedhotbar](https://github.com/IamFelix1111/extendedhotbar.git).
