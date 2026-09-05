#!/usr/bin/env bash
# Build a pinned, patched renderer dependency without editing an existing checkout.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
revision=424e96796d21d0810aadbc609db698c3c3a8b342
repository="${1:-https://github.com/fangbm/iris4vulkan.git}"
mkdir -p "$root/build"
checkout="$(mktemp -d "$root/build/iris-vulkan.XXXXXX")"
git -C "$checkout" init -q
git -C "$checkout" fetch --depth=1 "$repository" "$revision"
git -C "$checkout" checkout --detach FETCH_HEAD
git -C "$checkout" apply --check "$root/patches/iris-vulkan/renderer-compatibility.patch"
git -C "$checkout" apply "$root/patches/iris-vulkan/renderer-compatibility.patch"
(cd "$checkout" && bash ./gradlew :fabric:build :fabric:metalrenderRegression \
    --init-script "$root/scripts/iris-regression.init.gradle" "-Dmetalrender.root=$root")
mkdir -p "$root/build/dependencies"
cp "$checkout/build/libs/iris-fabric-1.11.2-snapshot+mc26.2-local.jar" "$root/build/dependencies/iris-vulkan.jar"
printf 'Patched renderer: %s\nSource checkout: %s\n' "$root/build/dependencies/iris-vulkan.jar" "$checkout"
