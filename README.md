<p align="center"><img src="src/main/resources/assets/fireglow/icon.png" width="128"></p>

# Fire Glow

Makes fire glow using shaders. Fire blocks, soul fire, lit campfires, burning mobs and optionally torches, lanterns and the fire on your screen. Fire and soul fire each get their own color, radius, strength and falloff, and the flames themselves get repainted in that color too, rainbow included (turn off Recolor flames in Effects for vanilla colored fire). Theres also pulse, flicker and shimmer, additive or screen blend. The glow stays behind whatever you hold unless you turn on Glow over hand in Show.

Press **J** to open the settings, or use Mod Menu. Rebind it in Controls > Key Binds > Fire Glow.

## How it works

- **Blocks:** fire, campfire, torch and lantern positions get tracked per chunk (chunk loads and block updates, no scanning every frame). Every frame the ones in range get drawn again into a mask with `core/mask`, depth tested against the world so walls block them.
- **Mobs and screen:** the flames on burning mobs and on your screen get drawn a second time into the same mask.
- **Glow:** the mask splits fire and soul fire by hue, gets downsampled, blurred (`core/down`, `core/blur`) and put over the frame with `core/merge`.
- **Recolor:** the mask also keeps how bright each flame pixel is, so the flames get repainted in the glow color with a fire style ramp (darker and deeper at the edges, hotter and whiter in the core) and keep their shape.
- **Hand:** the glow gets put over the frame right before your hand is drawn, so held items cover it. Screen fire gets its own pass after that.
- **Settings screen:** every shape is a distance field in `core/ui`. The blur behind it is the mod's own (`core/down`, `core/blur`, `core/frost`).

## Build

```
gradlew build
```

Jars go to `build/libs`:

| Jar | Minecraft |
|---|---|
| `fireglow-1.1.0+1.21.11.jar` | 1.21.11 |
| `fireglow-1.1.0+26.1.jar` | 26.1.x |
| `fireglow-1.1.0+26.2.jar` | 26.2.x |

Gradle needs Java 25. 1.21.11 builds on Java 21 through toolchains.

## Porting

Nothing outside `versions/` touches Minecraft's render code, it all goes through `hits.fire.Gpu`. A version folder only has:

- `Gpu.java`, `Keys.java`, `ui/Panel.java`
- the mixins that feed fire blocks, burning mobs and screen fire into the mask
- `fireglow.mixins.json`, `gradle.properties`, `build.gradle`

New version: copy the closest `versions/<mc>` folder, bump the versions in its `gradle.properties`, fix whatever breaks.

all rights reserved. made by wysss.

the menu needs [Wysss Library](https://github.com/realwysss/Wysss-Library) installed.
