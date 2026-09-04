# Changelog

All notable changes to MetalRender are documented in this file.

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
