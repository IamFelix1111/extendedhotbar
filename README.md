# Extended Hotbar

A [Fabric](https://fabricmc.net/) mod for [Minecraft](https://www.minecraft.net/) that places the bottom row of your inventory above the hotbar, doubling your quick-access slots.

Latest version: **1.0.2** for Minecraft **1.21.11**.

## Features

* Press `R` to swap the hotbar with the row above it.
* Press `Shift+R` to swap only the items in your hand.
* Press the key of your currently selected hotbar slot again to swap just that slot.
* Press `=` to enable or disable the mod.
* Press `V` to toggle fluent mode.

Hotkeys are configurable in the standard Minecraft controls settings, under the "Extended Hotbar" category.
Key actions are edge-triggered, so holding a key down won't repeat the action.

Additional configuration is available on the mod's in-game configuration screen, which requires [Mod Menu].

## Fluent Mode

Fluent mode can be enabled on the mod's configuration screen (requires [Mod Menu]) or by pressing `V`.
It disables manual swapping with `R` and instead swaps transparently as you scroll along a
double-wide hotbar; the second row is rendered next to the first, rather than on top of it.

While the secondary hotbar is selected, opening your inventory or any container
(chest, furnace, etc.) shows your items in their original rows, and clicking or shift-clicking
acts on the item you see. This works in single-player and on servers, and in both survival and
creative mode, including the creative inventory's item-grid tabs.

## Requirements

### Required

* [Minecraft](https://www.minecraft.net/) 1.21.11
* [Fabric Loader](https://fabricmc.net/) 0.19.3 or newer
* [Fabric API](https://modrinth.com/mod/fabric-api)
* Java 21 or newer

### Optional

* [Mod Menu] — adds the in-game configuration screen

[Mod Menu]: https://www.curseforge.com/minecraft/mc-mods/modmenu

### Bundled

* [Cloth Config](https://modrinth.com/mod/cloth-config) — included inside the mod jar, no separate installation needed

## Development Setup

After cloning this repository,
run the setup script once to initialize the `web` submodule (the mod's documentation pages):

* **Windows**: run `setup.cmd` (double-click it, or run `.\setup.cmd` in `cmd`).
* **Linux / macOS / WSL**: run `./setup.sh`.

The scripts:

1. initialize the `web` submodule (the [iamfelix1111.github.io](https://github.com/IamFelix1111/iamfelix1111.github.io.git) repo),
2. apply sparse-checkout so only `src/ExtendedHotbar` is checked out,
3. enable the git hooks in `githooks/`, and
4. create a symlink `src/main/resources/assets/extendedhotbar/icon.png` -> root `icon.png`.

Git hooks (`githooks/post-merge` and `githooks/pre-push`) keep the `web`
submodule in sync: pulls update it automatically, and pushes to this repo
first commit & push any pending changes in `/web` to the website repo.

> **Windows note**: creating the `icon.png` symlink (step 4) requires Windows
> Developer Mode to be enabled, otherwise `mklink` fails. On Unix/macOS it
> works out of the box.

## Credits

Maintained by [IamFelix](https://github.com/IamFelix1111).

Original author: [DenWav](https://github.com/DenWav).

Source repository: <https://github.com/IamFelix1111/extendedhotbar.git>.
