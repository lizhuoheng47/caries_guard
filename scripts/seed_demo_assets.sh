#!/usr/bin/env bash
# Seeding Demo Assets for Competition Mode

set -e

REPO_ROOT=$(dirname "$(dirname "$0")")
cd "$REPO_ROOT"

echo -e "\033[1;36m### Seeding Demo Assets for Competition Mode ###\033[0m"
echo -e "\033[1;33mThis script provisions baseline visual assets for the predefined demo cases.\033[0m"

DEMO_ASSET_DIR="evidence/generated/demo_assets"
mkdir -p "$DEMO_ASSET_DIR"

TARGET_ASSETS=(
    "pano_CA-2026-LOW.jpg"
    "peri1_CA-2026-LOW.jpg"
    "pano_CA-2026-HIGH.jpg"
    "peri1_CA-2026-HIGH.jpg"
    "peri2_CA-2026-HIGH.jpg"
)

for target in "${TARGET_ASSETS[@]}"; do
    FILE_PATH="$DEMO_ASSET_DIR/$target"
    echo -e "\033[0;37mGenerating placeholder asset at $FILE_PATH\033[0m"
    echo "DEMO_ASSET_PAYLOAD_FOR_$target" > "$FILE_PATH"
done

echo -e "\033[1;32mAssets seeded locally.\033[0m"
echo -e "\033[1;33mIf running inside Docker, ensure volume mappings cover evidence/generated.\033[0m"
echo -e "\033[1;36m--- Asset Seeding Complete ---\033[0m"
