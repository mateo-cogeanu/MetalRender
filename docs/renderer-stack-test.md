# Full-stack renderer test — 2026-09-05

**Result: incomplete.** The native Metal bootstrap and GPU command test pass,
Minecraft renders through Vulkan/MoltenVK, and the active BSL screen passes
compile and bind. This is not yet complete BSL rendering and is not a valid
performance comparison against a renderer executing the whole pack.

## Environment and preserved inputs

- Minecraft 26.2, Apple M4, MoltenVK 1.4.2 (reported by the running client).
- BSL 10.1.3, existing HIGH profile, zero user option overrides reported by Iris.
- Existing `VulkanShaderTest` world copied into an isolated build directory.
- `options.txt`, `config/iris.properties`, and the shader-pack archive compared
  byte-for-byte with the original run directory after testing: unchanged.
- Tests do not alter Minecraft settings, shader settings, resolution, render
  distance, or shader-pack source files.

The copied world contains references to absent Create blocks. Minecraft reports
recoverable registry errors for those blocks. The original world is untouched;
this fixture cannot establish parity with the original modded scene.

## Renderer changes

The dependency patch targets `fangbm/iris4vulkan` commit
`424e96796d21d0810aadbc609db698c3c3a8b342`:

1. Keep the backend-neutral biome mixins active on Vulkan. This restores biome
   constants and the live biome metadata used by pack expressions.
2. Recognize and copy `ivec2`/`ivec3` custom-uniform values, including smoothed
   eye brightness. Snapshots retain their values when live inputs mutate.
3. Compile translated sources as GLSL 450 and rename the GLSL identifier `new`
   to avoid a keyword collision in generated Metal Shading Language. The pack
   archive remains unchanged.
4. Advance Iris' shared timer and custom-uniform frame state before geometry.
   Smoothing and screen passes now use the same clock as terrain snapshots.

The patch does not enable unsupported routes or introduce dummy shadow inputs.
The fork's pre-existing fully-lit shadow substitutes remain a test failure.

## Verified and missing

| Path | Result |
| --- | --- |
| Native Metal resources and GPU commands | Runtime self-test passed |
| Minecraft Vulkan/MoltenVK device | Active on Apple M4 |
| BSL deferred, deferred1 | Compiled and bound |
| BSL composite, composite1, composite4–7 | Compiled and bound |
| BSL final | Compiled and bound |
| Seed pass and two mipmap pipelines | Compiled |
| Terrain solid/cutout | Rejected: `sampler2DShadow` unsupported |
| Actual shadow rendering | Not implemented; screen passes use upstream substitutes |
| Water, entities, hand, sky, weather and other scene routes | Not implemented in the fork |
| Pixel parity and performance advantage | Not established |

Compilation and binding establish execution coverage only, not correct lighting,
textures, temporal history, or final pixels. The next required renderer work is
real shadow attachments and comparison samplers, scene-pass routing, translucent
depth/order handling, and resource/vertex bindings for every supported stage.
Simply enabling the fork's capability flags would not implement these systems.

## Reproduction

Requires JDK 25 and the existing Gradle/Minecraft build dependencies.

```sh
scripts/build-iris-vulkan.sh
./gradlew build
./gradlew build -PirisVulkanJar=build/dependencies/iris-vulkan.jar
```

The dependency script fetches the pinned revision into a new directory under
`build/`, applies the checked-in patch, builds Iris, and runs regression checks
for integer-vector snapshots, generated GLSL/MSL compatibility, and shared frame
clocks. Upstream's ordinary test task is disabled; it is not counted as passing.
It copies the tested artifact to `build/dependencies/iris-vulkan.jar`. An optional
first argument can name a local Iris Git repository containing the pinned commit.
No external checkout is modified.

To repeat the live test, copy the existing run inputs and world into a separate
run directory first, then launch without changing their contents:

```sh
./gradlew runClient \
  -PirisVulkanJar=build/dependencies/iris-vulkan.jar \
  -PrendererTestRunDir=build/renderer-stack-test \
  -PrendererTestWorld=VulkanShaderTest
python3 scripts/audit-renderer-run.py build/renderer-stack-test --reference run
```

The audit prints JSON and returns status 1 for this incomplete stack, including
missing routes, rejected passes, shadow substitutes, and any changed input
files. Even a clean audit requires visual validation; it cannot certify parity
or a speedup. Live logs and generated artifacts remain under ignored `build/`.
