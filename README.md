# Tamed Phantoms

**[English](README.md)** · **[Русский](README.ru.md)**

A **Minecraft 1.21.1** NeoForge mod: tame phantoms, ride them with a saddle, sit them, leash them, and release them. Written in Kotlin with [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge).

The UI and in-game guidebook are available in English and Russian.

## Downloads

Jars live on [GitHub Releases](https://github.com/Nergan/tamedphantoms-mod/releases/latest). The **Releases** tab appears after the first `v*` tag is pushed.

Download these files and put them in the `mods` folder:

| File | Required | What it is |
| --- | --- | --- |
| `tamedphantoms-1.0.0.jar` | Yes | this mod |
| `kotlinforforge-5.8.0-all.jar` | Yes | [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | No | [Patchouli](https://modrinth.com/mod/patchouli), only if you want the guidebook |

The release workflow builds the mod and fetches the two companion jars from Modrinth. Do not install `*-sources.jar`.

## Features

- **Taming.** Right-click a wild phantom with the tame item (cookie by default). The phantom grows larger and darker, and its eyes glow green. While you hold the tame item, wild phantoms approach you and will not attack.
- **Eyes.** Wild phantoms have blood-red eyes. Tamed phantoms have bright green eyes. Released phantoms have bright yellow eyes. During self-defense the eyes turn red again.
- **Following.** A tamed phantom stays near its owner like a normal pet.
- **Sit.** Right-click (crouch first if it has a saddle). The phantom flips, falls, and stays put. It takes no fall damage. You cannot sit it while riding, and you cannot mount a sitting phantom.
- **Saddle and flight.** The owner equips a saddle and mounts. Movement uses the usual walk keys; jump climbs; Left Control descends. Keys can be remapped. Night vision is granted while flying. A second player can sit behind. Remove the saddle with shears.
- **Leash.** A tamed phantom can be leashed and unleashed with an empty hand.
- **Healing.** The owner can feed it any food.
- **Self-defense.** If the phantom is hit, it attacks the attacker for about ten seconds — including the owner. Duration is configurable. Each hit heals the phantom for the damage it dealt.
- **Stats.** Tamed and released phantoms have 40 health, 12 attack damage, and fly twice as fast as a vanilla phantom. They do not burn in sunlight and do not drown.
- **Repel.** A tamed phantom pushes wild phantoms away (64 blocks by default).
- **Release.** Right-click with the release item (poisonous potato by default). Ownership ends, the saddle drops, and the eyes turn yellow. A released phantom can be tamed again.
- **Advancements.** “Winged Companion” for taming and “No Longer Responsible” for releasing.
- **Guidebook (optional).** With [Patchouli](https://modrinth.com/mod/patchouli) installed, the first join into a world gives *Phantom Taming Guide*. Craft it with a book and a phantom membrane. Without Patchouli the rest of the mod works as usual.

## Requirements

| Component        | Version                                     |
| ---------------- | ------------------------------------------- |
| Minecraft        | 1.21.1                                      |
| NeoForge         | 21.1.209 (any 21.1.x should work)           |
| Kotlin for Forge | 5.8.0, **NeoForge** build                   |
| Java             | 21                                          |
| Patchouli        | any 1.21.1 build, only if you want the book |

## Installation

1. Install NeoForge 1.21.1.
2. Download the jars from [the latest Release](https://github.com/Nergan/tamedphantoms-mod/releases/latest).
3. Put `tamedphantoms-1.0.0.jar` and `kotlinforforge-5.8.0-all.jar` in `mods`.
4. Optionally add `Patchouli-1.21.1-93-NEOFORGE.jar` from the same release.

The mod is required on both client and server. You can also get Kotlin for Forge and Patchouli from [Modrinth](https://modrinth.com/mod/kotlin-for-forge) instead of the GitHub release.

## Flight controls

Options → Controls → Tamed Phantoms.

| Action                | Default       |
| --------------------- | ------------- |
| Forward, back, strafe | movement keys |
| Ascend                | Space         |
| Descend               | Left Control  |

Descend is not bound to Shift on purpose: while riding, Shift dismounts.

Only the owner in the front seat can steer.

## Configuration

In-game: Mods → Tamed Phantoms → Config.

World file: `saves/<world>/serverconfig/tamedphantoms-server.toml`.

Dedicated server: `world/serverconfig/tamedphantoms-server.toml`. This is a `SERVER` config: the server owns the values and syncs them to clients.

| Option              | Default                      | Meaning                                     |
| ------------------- | ---------------------------- | ------------------------------------------- |
| `tame_item`         | `minecraft:cookie`           | taming item                                 |
| `release_item`      | `minecraft:poisonous_potato` | release item                                |
| `tame_chance`       | `1.0`                        | chance to tame per attempt                  |
| `repel_radius`      | `64.0`                       | radius that pushes wild phantoms away       |
| `defend_time_ticks` | `200`                        | self-defense duration (20 ticks = 1 second) |

An invalid item id falls back to the default and a warning is written to the log.

## Building

You need JDK 21 and access to the NeoForge, Kotlin for Forge, and Minecraft Maven repositories.

This repo does not include `gradle/wrapper/gradle-wrapper.jar`. Generate it once locally:

```bash
gradle wrapper --gradle-version 8.10
```

Or open the project in IntelliJ IDEA and let it fetch Gradle.

```bash
./gradlew build          # Linux and macOS
gradlew.bat build        # Windows

./gradlew test
./gradlew runClient
./gradlew runServer
```

The playable jar is `build/libs/tamedphantoms-1.0.0.jar`. Do not put the `-sources.jar` in `mods`.

On low-memory machines, build with `CI=true` and JDK 21 so Minecraft is not decompiled again.

## Tests

`./gradlew test` runs JUnit 5 against the pure logic in `util/` (eye recoloring, anger timer, saddle seats, tame chance, and healing). Gameplay itself is not covered.

## Publishing

### GitHub Release

Bump `mod_version` in `gradle.properties` if needed, then tag the same number:

```bash
git tag v1.0.0
git push origin v1.0.0
```

[`.github/workflows/release.yml`](.github/workflows/release.yml) builds the mod, downloads Kotlin for Forge and Patchouli from Modrinth, and publishes all three jars on the Releases page. You can also run **Actions → Release → Run workflow**. The tag must be `v` plus `mod_version` (for `1.0.0` that is `v1.0.0`). Companion versions are `kff_version` and `patchouli_version` in `gradle.properties`.

### Modrinth

On Modrinth, set the loader to NeoForge, the game version to 1.21.1, and list **Kotlin for Forge** as a required dependency. Mark Patchouli as optional.

## License

The code is [MIT](LICENSE). Vanilla Minecraft textures are not shipped: eye and body recoloring is computed on the client from the installed game.
