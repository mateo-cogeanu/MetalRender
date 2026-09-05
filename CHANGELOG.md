# Changelog

All notable changes to MetalRender are documented in this file.

## 0.2.2 - 2026-09-05

### Fixed

- Fixed the Iris shader-pack menu crashing on Vulkan because its custom button
  widgets unconditionally changed OpenGL blend and depth-test state.
- Kept Iris' custom buttons on Minecraft's backend-neutral GUI pipelines while
  retaining their original OpenGL behavior when MetalRender's Vulkan backend
  is disabled.

### Verified

- Forced the Iris shader-pack screen open during a no-OpenGL development smoke
  test and kept it in the live render loop without a native GL abort.

## 0.2.1 - 2026-09-04

### Added

- Added Iris 1.11.2 to the Minecraft 26.2 development runtime and marked it as
  an optional supported integration target.
- Added the first no-OpenGL Iris compatibility layer: portable sampler limits,
  Vulkan-safe texture hooks, and isolation for Iris' OpenGL-only startup code.

### Changed

- Iris can now coexist with MetalRender through complete title-screen resource
  loading while Minecraft remains on Vulkan over MoltenVK with no OpenGL
  context.
- Shader-pack program execution remains disabled until Iris render targets and
  GLSL-to-SPIR-V pipeline creation are implemented; this milestone does not
  claim shader rendering support.

## 0.2.0 - 2026-09-04

### Added

- Added an operational no-OpenGL rendering path on macOS by handing the
  verified `GLFW_NO_API` window to Minecraft's Vulkan backend, which executes
  on Metal through MoltenVK.
- Enabled the no-OpenGL backend path by default on macOS. It can be disabled
  with `-Dmetalrender.experimental.fullMetalBackend=false`.
- Added runtime HUD detection for Vulkan-over-MoltenVK, including an accurate
  `ACTIVE VIA MOLTENVK` state and dynamic window-backend reporting.

### Changed

- The experimental backend no longer deliberately falls back after its native
  Metal resource and command self-tests pass.
- Kept the duplicate legacy hybrid renderer disabled while native MSL shader
  compilation and render-pass support remain under development.

## 0.1.12 - 2026-09-04

### Added

- Added native Metal command-buffer creation, submission, and synchronous
  completion for backend bring-up and verification.
- Added Metal color, depth, and combined color/depth attachment clears.
- Added direct buffer writes and GPU-side buffer-to-buffer copies with bounds
  validation.
- Added an initial transient-memory implementation backed by mapped Metal
  buffers.
- Expanded the no-OpenGL backend self-test to submit real Metal commands and
  verify a GPU-copied byte marker after completion.

## 0.1.11 - 2026-09-04

### Added

- Added Minecraft 26.2-compatible Metal implementations of `GpuBuffer`,
  `GpuTexture`, `GpuTextureView`, and `GpuSampler`.
- Added native creation, mapping, and deterministic release for Metal buffers,
  textures, texture views, and sampler states.
- Added a backend resource self-test that exercises all four resource types on
  the real `GLFW_NO_API` window before the still-required command-encoder
  fallback.
- Added mappings from Minecraft GPU formats and texture usage flags to their
  native Metal equivalents.

## 0.1.10 - 2026-09-04

### Fixed

- Disabled the incomplete legacy OpenGL/Metal hybrid renderer by default so it
  no longer duplicates Sodium chunk scanning and mesh construction while
  submitting zero Metal frames on Minecraft 26.2.
- Kept the legacy hybrid path available for development with
  `-Dmetalrender.enabled=true`; the independent no-OpenGL backend bootstrap is
  still controlled by `-Dmetalrender.experimental.fullMetalBackend=true`.

## 0.1.9 - 2026-09-04

### Added

- Added an opt-in Minecraft 26.2 `GpuBackend` bootstrap controlled by
  `-Dmetalrender.experimental.fullMetalBackend=true`.
- Added a native full-window `CAMetalLayer` presentation check on a
  `GLFW_NO_API` window, proving that the backend can start without creating an
  OpenGL context.
- Added safe fallback to Minecraft's next graphics backend while Metal device,
  resource, and command-encoder support is still under construction.

### Fixed

- Made the native build script explicitly compile as C++17 and removed its
  incompatible ARC flag so the bundled renderer can be rebuilt reproducibly.

## 0.1.8 - 2026-09-04

### Added

- Added an F3 diagnostics overlay that distinguishes initialized state from
  verified Metal frame submission and IOSurface presentation.
- Added counters for Metal frames, chunk draws, compositor successes, failures,
  fast-path presentation, slow readback fallback, and texture-atlas fallback.
- Added an opt-in direct `CAMetalLayer` presentation probe controlled by
  `-Dmetalrender.experimental.directPresentationProbe=true`.
- Added extraction and loading of the bundled ARM64 native library when the mod
  runs from a JAR.

### Changed

- Updated Minecraft support from 26.1 to 26.2.
- Updated Fabric Loader to 0.19.3, Fabric API to 0.158.0+26.2, and Sodium to
  0.9.1+mc26.2.
- Updated Minecraft API call sites for the 26.2 mappings.

### Fixed

- Made renderer diagnostics report observed Metal work instead of treating a
  successfully allocated native handle as proof that Metal rendered a frame.
