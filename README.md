# Tamed Phantoms

**[English](README.md)** · **[Русский](README.ru.md)**

A **Minecraft 1.21.1** NeoForge mod: tame phantoms, ride them with a saddle, sit them, leash them, and release them. Written in Kotlin with [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge).

The UI and in-game guidebook are available in English and Russian.

## Downloads

Jars live on [GitHub Releases](https://github.com/Nergan/tamedphantoms-mod/releases/latest). A push to `main` updates the files on the current version’s release.

Download these files and put them in the `mods` folder:

| File                               | Required | What it is                                                                      |
| ---------------------------------- | -------- | ------------------------------------------------------------------------------- |
| `tamedphantoms-1.0.0.jar`          | Yes      | this mod                                                                        |
| `kotlinforforge-5.8.0-all.jar`     | Yes      | [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge)                   |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | No       | [Patchouli](https://modrinth.com/mod/patchouli), only if you want the guidebook |

The release workflow builds the mod and fetches the two companion jars from Modrinth. GitHub shows a SHA-256 digest next to each file on the release page. Do not install `*-sources.jar`.

## Features

- **Taming.** Right-click a wild phantom with the tame item (cookie by default). The phantom grows larger and darker, and its eyes glow green. While you hold the tame item, wild phantoms approach you and will not attack.
- **Eyes.** Wild phantoms have blood-red eyes. Tamed phantoms have bright green eyes. Released phantoms have bright yellow eyes. During self-defense the eyes turn red again.
- **Following.** A tamed phantom stays near its owner like a normal pet.
- **Sit.** Right-click (crouch first if it has a saddle). The phantom flips, falls, and stays put. It takes no fall damage. You cannot sit it while riding, and you cannot mount a sitting phantom.
- **Saddle and flight.** The owner equips a saddle and mounts. Movement uses the usual walk keys; jump climbs; Left Control descends. Keys can be remapped. Night vision is granted while flying. A second player can sit behind. Remove the saddle with shears.
- **Leash.** A tamed phantom can be leashed and unleashed with an empty hand.
- **Healing.** The owner can feed it any food.
- **Self-defense.** If the phantom is hit, it attacks the attacker for about ten seconds — including the owner. Duration is configurable. Each hit heals the phantom for the damage it dealt. While it is defending, it can scream once the scream is ready.
- **Scream.** The owner, while riding, presses R (remap under Tamed Phantoms). The phantom plays a louder, deeper ambient cry. For one second its eyes turn red, its body glows, and mobs in range flee and are blinded. The owner is not blinded. Cooldown is 30 seconds; if it is not ready, a red action-bar line says how many seconds remain. The first use grants the advancement Winged Beast. Radius and cooldown are server settings. The client volume slider does not quiet this cry. On a full moon, a released phantom may cry on its own even when it is not attacking: the eyes stay yellow, the glow still appears, mobs still flee, and blindness is not applied. Anyone who has that chunk loaded can hear it.
- **Stats.** Tamed and released phantoms have 40 health, 12 attack damage, and fly twice as fast as a vanilla phantom. They do not burn in sunlight and do not drown.
- **Repel.** A tamed phantom pushes wild phantoms away (64 blocks by default).
- **Release.** Right-click with the release item (poisonous potato by default). Ownership ends, the saddle drops, and the eyes turn yellow. A released phantom can be tamed again.
- **Advancements.** “Winged Companion” for taming, “No Longer Responsible” for releasing, and “Winged Beast” the first time the owner makes a phantom scream.
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
| Scream                | R             |

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
| `defend_time_ticks`      | `200`                        | self-defense duration (20 ticks = 1 second) |
| `scream_radius`          | `32.0`                       | blocks; mobs in range flee from the scream |
| `scream_cooldown_seconds`| `30`                         | seconds before the scream can be used again |

Client config: `config/tamedphantoms-client.toml` (or Mods → Config in single-player).

| Parameter            | Default | Meaning                             |
| -------------------- | ------- | ----------------------------------- |
| `tamed_sound_volume` | `0.5`   | tamed phantom volume on this client |

An invalid item id falls back to the default and a warning is written to the log.

## License

The code is [MIT](LICENSE). Vanilla Minecraft textures are not shipped: eye and body recoloring is computed on the client from the installed game.
