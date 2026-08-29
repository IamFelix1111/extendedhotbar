#!/bin/sh
# ============================================================
#  ExtendedHotbar project setup (Linux / macOS / WSL)
#  Run this ONCE after cloning the repository.
# ============================================================

set -e

cd "$(dirname "$0")"

echo "[setup] 1/4 Initializing submodules..."
git submodule update --init --recursive

# Ignore IntelliJ module files locally (sparse-checkout hides the repo .gitignore).
gitdir="$(git -C web rev-parse --absolute-git-dir)"
grep -qxF '*.iml' "$gitdir/info/exclude" 2>/dev/null || echo '*.iml' >> "$gitdir/info/exclude"

echo "[setup] 2/4 Applying sparse-checkout (src/ExtendedHotbar + src/style.css + src/i18n.js)..."
# MSYS_NO_PATHCONV=1 prevents Git Bash from mangling the leading slash.
MSYS_NO_PATHCONV=1 git -C web sparse-checkout set --no-cone /src/ExtendedHotbar/ /src/style.css /src/i18n.js

echo "[setup] 3/4 Enabling git hooks..."
git config core.hooksPath githooks

echo "[setup] 4/4 Creating icon symlink..."
rm -f src/main/resources/assets/extendedhotbar/icon.png
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
        # Windows (Git Bash): file symlink requires Developer Mode.
        MSYS_NO_PATHCONV=1 cmd /c 'mklink src\main\resources\assets\extendedhotbar\icon.png ..\..\..\..\..\icon.png'
        ;;
    *)
        ln -s ../../../../../icon.png src/main/resources/assets/extendedhotbar/icon.png
        ;;
esac

echo
echo "Setup complete. You can now build the project."
