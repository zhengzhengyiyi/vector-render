# 1.21.11 Renderer

A Fabric client mod for Minecraft 1.20.4 that backports rendering improvements from 1.21.11.

## Requirements

- Minecraft 1.20.4
- Fabric Loader 0.19.3 or later
- Fabric API 0.97.3+1.20.4

## What it does

### GPU backend (blaze3d)

Ports the 1.21.11 `blaze3d` rendering stack into 1.20.4. This includes the new `GpuDevice`/`RenderPass`/`CommandEncoder` abstraction, pipeline-based shader system, and the `RenderPipeline` / `CompiledRenderPipeline` classes. The backend is initialised after the OpenGL context is ready and replaces the relevant parts of 1.20.4's rendering path via Mixin.

### Frame pacing

Caps the frame rate at the monitor's refresh rate when the in-game FPS limit is set to "Unlimited". Vanilla 1.20.4 renders without any throttle at that setting, causing the GPU to run at 100% producing frames the display can never show. On a 144 Hz monitor this reduces GPU load significantly with no visible difference.

### Fog rendering

Replaces 1.20.4's `BackgroundRenderer.render()` call in `WorldRenderer` with the 1.21.11 fog pipeline, which includes improved fog modifiers for different dimension and environment types.

### Chunk build scheduling

Replaces 1.20.4's dual-queue chunk rebuild system with a proximity-aware scheduler that prioritises chunks closest to the camera, matching 1.21.11 behaviour.

### Leash-aware frustum culling

Fixes a visual glitch where a leashed mob at the edge of the screen is culled while its holder is still visible, causing the leash rope to render without one endpoint. The fix checks the holder's bounding box as well as the union of both boxes before culling, matching 1.21.11's `EntityRenderer.shouldRender()` logic.

### Blocking pose item transform

Fixes the gap between the player's arm and a held item when blocking with a non-shield item (sword, axe, etc.). Vanilla 1.20.4 only applies the equip offset in the `BLOCK` use action case, leaving the item in its default position while the arm rotates. The fix applies the same rotation correction that 1.21.11 introduced to align the item flush with the raised arm.

## Building

```
./gradlew build
```

The output jar is in `build/libs/`.

## Project structure

```
src/
  main/          — mod initialiser, shared code
  client/        — all rendering code (client-only)
    mixin/       — Mixin injections into Minecraft classes
    render/      — fog renderer, render engine, GPU backend wrappers
    renderer/    — blaze3d API ports, GL backend, shader loader
decompile/
  1.20.4-vineflower/   — reference decompile of 1.20.4 client, git ignored, reference for ai
  1.21.11-vineflower/  — reference decompile of 1.21.11 client, git ignored, reference for ai
```

<!-- WordStatusExisting Mods Found?VerdictVelocity🟢 CLEARNone. (Only the standalone "Velocity" proxy server tool for BungeeCord exists, but no Minecraft game performance mods use this name).Perfect choice for your main rendering engine.Vector🟢 CLEARNone. Completely untouched in the performance/optimization category.Perfect for your CPU/Tick logic optimizer.Static🟢 CLEARNone. Completely untouched.Perfect for your RAM/Memory reducer.Inertia🟡 AVOIDThere is an old, well-known hacked/utility client called "Inertia Client". Using this would confuse players.Avoid using for this suite.Friction🟡 AVOIDThere is a recent PvP aiming adjustment mod called "FrictionAim".Avoid using for this suite.Momentum🟢 CLEARNone. Only mentioned as a general gameplay mechanic descriptor.Excellent backup option. -->
