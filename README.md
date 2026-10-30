# Tamed Phantoms

**[English](README.md)** · **[Русский](README.ru.md)**

A **Minecraft 1.21.1** mod for NeoForge and Fabric: tame phantoms, ride them with a saddle, sit them, leash them, and release them. The NeoForge build uses [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge). The Fabric build uses [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin). Install one loader, not both.

The UI and in-game guidebook are available in English and Russian.

## Downloads

Jars live on [GitHub Releases](https://github.com/Nergan/tamedphantoms-mod/releases/latest). A push to `main` updates one GitHub Release page per Minecraft version and loader. Each page includes `all-jars.zip`.

Download one set and put those files in the `mods` folder.

### NeoForge

| File                               | Required | What it is                                                                      |
| ---------------------------------- | -------- | ------------------------------------------------------------------------------- |
| `tamedphantoms-neoforge-1.21.1-1.0.10.jar` | Yes | this mod, NeoForge 1.21.1 |
| `kotlinforforge-5.8.0-all.jar`     | Yes      | [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge)                   |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | No       | [Patchouli](https://modrinth.com/mod/patchouli), only if you want the guidebook |

### Fabric

| File                                      | Required | What it is                                                                                         |
| ----------------------------------------- | -------- | -------------------------------------------------------------------------------------------------- |
| `tamedphantoms-fabric-1.21.1-1.0.10.jar` | Yes | this mod, Fabric 1.21.1 |
| `fabric-api-0.116.17+1.21.1.jar`          | Yes      | [Fabric API](https://modrinth.com/mod/fabric-api)                                                  |
| `fabric-language-kotlin-1.13.2+kotlin.2.1.20.jar` | Yes | [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)                    |
| `cloth-config-15.0.140-fabric.jar`        | Yes      | [Cloth Config](https://modrinth.com/mod/cloth-config), the config screen                           |
| `modmenu-11.0.4.jar`                      | No       | [Mod Menu](https://modrinth.com/mod/modmenu), the button that opens that screen                    |
| `placeholder-api-2.4.2+1.21.jar`          | No       | [Text Placeholder API](https://modrinth.com/mod/placeholder-api), required by Mod Menu             |
| `Patchouli-1.21.1-93-FABRIC.jar`          | No       | [Patchouli](https://modrinth.com/mod/patchouli), only if you want the guidebook                    |

The release workflow builds both jars and fetches the companion jars from Modrinth. GitHub shows a SHA-256 digest next to each file on the release page. Do not install `*-sources.jar`.

## Features

- **Taming.** Right-click a wild phantom with the tame item (cookie by default). The phantom grows larger and darker, and its eyes glow green. While you hold the tame item, wild phantoms approach you and will not attack.
- **Eyes.** Wild phantoms have blood-red eyes. Tamed phantoms have bright green eyes. Released phantoms have bright yellow eyes. During self-defense the eyes turn red again.
- **Following.** A tamed phantom stays near its owner like a normal pet. If you teleport far enough that its chunk unloads, it follows to a clear spot beside you: not inside a wall, lava, fire, or a cactus. A phantom you told to sit stays where you left it. The client config button *Where are my phantoms* writes each of yours into chat, sitting or moving, with its last coordinates.
- **Sit.** Right-click (crouch first if it has a saddle). The phantom flips, falls, and stays put. It takes no fall damage. You cannot sit it while riding, and you cannot mount a sitting phantom.
- **Saddle and flight.** The owner equips a saddle and mounts. Movement uses the usual walk keys; jump climbs; Left Control descends. Keys can be remapped. Night vision is granted while flying. A second player can sit behind; the owner always takes the front seat, even if the other player climbed on first. More than eight blocks above the surface, the first Shift only warns both riders; a second press within a second dismounts. Remove the saddle with shears.
- **Healing.** Anyone can feed a tamed phantom ordinary food. If it is hurt and the owner or another player holds that food nearby — anything except a poisonous potato or the release item — the phantom stops, turns its head toward them, and nods. At full health it does not nod. A poisonous potato or the release item still makes it stop, turn its head toward them, and shake; only the owner can give those, and someone else's click also puffs angry villager particles. The motion stops as soon as the item leaves their hands. Food held by a rider of that phantom does not count, and a phantom that already has a rider does not stop. The food's own effects apply too: poison food poisons, a golden apple regenerates, and a chorus fruit teleports the phantom. Glow berries add eight seconds of glowing. A stew leaves the bowl in the hand, and a honey bottle leaves the bottle. Milk clears every effect and leaves the bucket; the phantom nods at milk even at full health. Hunger inverts the controls while it lasts. Speed, slowness, jump boost, levitation, slow falling, and dolphin's grace change the flight. Flying out of lava makes it shake off lava sparks.
- **Self-defense.** If the phantom is hit, it attacks the attacker for about ten seconds — including the owner. Duration is configurable. Each hit heals the phantom for the damage it dealt. While it is defending, it can scream once the scream is ready.
- **Pack.** Phantoms stand together against a player. Anger a released phantom for any reason, and every released phantom within 32 blocks joins in. Anger someone's tamed phantom, even your own, and all of that owner's phantoms turn on you.
- **Scream.** The owner, while riding, presses R (remap under Tamed Phantoms). The phantom plays a louder, deeper ambient cry. For one second its eyes turn red, its body glows, and mobs in range flee and are blinded. Anyone sitting on the screaming phantom is spared. An owner who is riding any of their phantoms, and anyone riding with them, is spared from all of that owner's phantoms. The owner on the ground is blinded if the phantom screams in self-defense. Only wild phantoms are frightened: tamed and released ones ignore every cry. Every other tamed phantom of that owner that can hear the rider's cry answers at once. Cooldown is 4 seconds; if it is not ready, a red action-bar line says how many seconds remain. The first use grants the advancement Winged Beast. Radius and cooldown are server settings. With `stacked_screams` on (the default), several cries heard at once last longer, and from the third one players and mobs take a little magic damage that ignores armor. Nine cries are the cap: eight seconds of blindness and ten damage. Mob fear grows the same way. The client volume slider does not quiet this cry. On a full moon, a released phantom may cry on its own even when it is not attacking: the eyes stay yellow, the glow still appears, mobs still flee, blindness is not applied, and the moon turns a sickly yellow for that second. Anyone who has that chunk loaded can hear it. Hearing that cry grants the secret advancement Sick Moon.
- **Flight.** The phantom keeps its speed: climbing spends it, diving builds it, and wingbeats push only up to cruising speed. Without forward it glides, slowly losing height and speed, until it hovers; back brakes harder. The body banks into turns, and a sharp turn at speed bleeds a little speed. A dive without forward folds the wings back like a falcon's, and the extra speed lasts a few seconds after leveling out. Wing sweep, the tail, and an elytra-like wind sound follow the speed; the wind fades out smoothly. On the ground the phantom walks on its wings, and in water it swims with ray-like strokes.
- **Maneuvers.** Double-tap jump while flying fast enough: the phantom flies a whole loop. Too slow, and it stalls before the top and drops its nose. Double-tap left or right for a quick barrel roll that also slips aside. The camera follows part of a climb or dive and all of a loop.
- **Cruise.** Double-tap forward while flying: the phantom flies on by itself and still turns with the camera. Any movement key, jump, descend, or Shift hands control back. Both changes are shown above the hotbar.
- **Immortal phantoms.** With the server option `immortal_phantoms` on, a full loop grants the secret advancement “Dead Loop!”, and that phantom glows permanently and cannot die. The option is off by default. While it is off, loops grant nothing, and phantoms that are already immortal become ordinary tamed phantoms. The change applies without a restart.
- **Leash.** A tamed phantom can be leashed and unleashed with an empty hand. Click a fence to tie it: every phantom on your lead within 12 blocks is tied, a sitting one too. A click that lands on the phantom with the fence right behind it also ties it. A released phantom drifts aside and snaps the lead.
- **Behavior.** A tamed phantom keeps a comfortable distance from its owner, neither far away nor in your face, and sometimes plays around them. A released phantom has its own temperament and flies loops, rolls, wingovers, and stoops. It reacts to terrain, biome, time of day, moon phase, and weather. Like a dolphin, it comes to play with players: it flies alongside and a little ahead of someone who is running or flying, circles someone who stands still, swoops past, and shows off tricks. Now and then it screams at a mob nearby just to watch it run; that cry does not blind. After leaving water a phantom shakes itself dry, but not in rain; in snowfall or in a desert it shakes as usual.
- **Stats.** Tamed and released phantoms have 40 health, 12 attack damage, and fly faster than a vanilla phantom (1.25 times by default, the server option `flight_speed`). They do not burn in sunlight and do not drown. On death they drop 1–12 phantom membranes.
- **Repel.** Wild phantoms fear tamed and released ones (64 blocks by default): they drop their target, climb away, and circle somewhere else instead of swooping at players nearby.
- **Obstacles.** Wild, tamed, and released phantoms steer around trunks, cacti, and other narrow obstacles, and they climb away from lava, fire, magma, campfires, cobwebs, berry bushes, and wither roses. A ridden phantom still follows the rider.
- **Release.** Right-click with the release item (poisonous potato by default). Ownership ends, the saddle drops, and the eyes turn yellow. A released phantom can be tamed again.
- **Potions.** A drinkable potion, right-clicked on a tamed phantom, applies that potion and returns the bottle. Splash and lingering potions do not. Instant Health angers that one phantom and grants *What Is This Filth?!* Instant Harm heals it and grants *Nasty Healing*. Weakness, only when the owner gives the drinkable potion, flips it onto its back for the potion's duration, drops anyone in the saddle, and grants *That's enough for today...* Night Vision inverts the colors of anyone riding that phantom for the potion's duration and grants *Sensory Overload*. The night vision from riding itself does not invert the picture.
- **Advancements.** “Winged Companion” for taming, “No Longer Responsible” for releasing, “Winged Beast” the first time the owner makes a phantom scream, the secret “Dead Loop!” for a full loop (only with `immortal_phantoms` on), the secret “Sick Moon” for hearing a released phantom’s full-moon cry, plus *What Is This Filth?!*, *Nasty Healing*, *That's enough for today...*, and *Sensory Overload* for the four potion exceptions.
- **Other mods.** If another mod replaces the phantom model, animation, texture, or sound, that replacement stays on wild phantoms. A tamed phantom and a released one keep the model, animation, and sounds this mod uses when it is the only phantom mod. Shift and right-click still toggles sit after a finished dead loop, even when another mod also takes a right-click on the mob.
- **Guidebook (optional).** With [Patchouli](https://modrinth.com/mod/patchouli) on 1.21.1 or [Modonomicon](https://modrinth.com/mod/modonomicon) on 26.3 (Patchouli has no 26.3 build), the first join into a world gives *Phantom Taming Guide* once per player; death and respawn do not give a second copy. Craft it with a book and a phantom membrane. Without a book mod the rest of the mod works as usual.
- **Addons.** [Tamed Phantoms Colors](https://modrinth.com/project/tamed-phantoms-colors) dyes tamed phantoms and their saddles and makes them glow.

## Requirements

| Component        | Version                                     |
| ---------------- | ------------------------------------------- |
| Minecraft        | 1.21.1                                      |
| NeoForge         | 21.1.209 (any 21.1.x should work)           |
| Kotlin for Forge | 5.8.0, **NeoForge** build                   |
| Fabric Loader    | 0.16.10 or newer                            |
| Fabric API       | 0.116.17+1.21.1                             |
| Fabric Language Kotlin | 1.13.2+kotlin.2.1.20                  |
| Cloth Config     | 15.0.140, Fabric build                      |
| Java             | 21                                          |
| Patchouli        | any 1.21.1 build for the same loader, only if you want the book |

## Installation

NeoForge:

1. Install NeoForge 1.21.1.
2. Download the NeoForge jars from [the latest Release](https://github.com/Nergan/tamedphantoms-mod/releases/latest).
3. Put `tamedphantoms-neoforge-1.21.1-1.0.10.jar` and `kotlinforforge-5.8.0-all.jar` in `mods`.
4. Optionally add `Patchouli-1.21.1-93-NEOFORGE.jar` from the same release.

Fabric:

1. Install Fabric Loader 0.16.10 or newer for Minecraft 1.21.1. Loader 0.19.x is fine. Do not look for loader version 0.100.3: that number is not a loader.
2. Download the Fabric jars from the same release. The release replaces its file list on each push, so old names such as `tamedphantoms-1.0.0.jar` and `tamedphantoms-fabric-1.0.0.jar` are not part of the current set.
3. Put all of these in `mods`: `tamedphantoms-fabric-1.21.1-1.0.10.jar`, `fabric-api-0.116.17+1.21.1.jar`, `fabric-language-kotlin-1.13.2+kotlin.2.1.20.jar`, and `cloth-config-15.0.140-fabric.jar`.
4. Optionally add Mod Menu, Placeholder API, and `Patchouli-1.21.1-93-FABRIC.jar`.

If the game says Patchouli requires **fabric** 0.100.3 or that Tamed Phantoms requires **fabric-api**, the `fabric-api-0.116.17+1.21.1.jar` file is missing. Fabric API’s id is `fabric-api`, and it also provides the id `fabric`. That is the jar from this release, not a newer Fabric Loader.

The mod is required on both client and server. Companion mods can also be installed from Modrinth; the Fabric version there is `1.0.10-fabric`.

## Flight controls

Options → Controls → Tamed Phantoms.

| Action                | Default       |
| --------------------- | ------------- |
| Forward, back, strafe | movement keys |
| Ascend                | Space         |
| Descend               | Left Control  |
| Scream                | R             |
| Cruise                | double-tap forward |
| Loop                  | double-tap ascend at speed |
| Barrel roll           | double-tap left or right |

Descend is not bound to Shift on purpose: while riding, Shift dismounts.

Only the owner in the front seat can steer.

## Configuration

NeoForge: Mods → Tamed Phantoms → Config.

Fabric, with Mod Menu: Mods → Tamed Phantoms → the configure button. On a remote server the world rules are read-only. Without Mod Menu, edit the toml files below.

World file: `saves/<world>/serverconfig/tamedphantoms-server.toml`.

Dedicated server: `world/serverconfig/tamedphantoms-server.toml`. This is a `SERVER` config: the server owns the values and syncs them to clients.

Fabric keeps a template at `config/tamedphantoms-server.toml`. A new world copies it into `serverconfig` when that file is not there yet. On a remote server the client receives these values from the server.

| Option              | Default                      | Meaning                                     |
| ------------------- | ---------------------------- | ------------------------------------------- |
| `tame_item`         | `minecraft:cookie`           | taming item                                 |
| `release_item`      | `minecraft:poisonous_potato` | release item                                |
| `tame_chance`       | `1.0`                        | chance to tame per attempt                  |
| `repel_radius`      | `64.0`                       | radius that pushes wild phantoms away       |
| `defend_time_ticks`      | `200`                        | self-defense duration (20 ticks = 1 second) |
| `phantom_size`           | `3`                          | vanilla size of tamed and released phantoms |
| `scream_radius`          | `32.0`                       | blocks; mobs in range flee from the scream |
| `scream_cooldown_seconds`| `4`                          | seconds before the scream can be used again |
| `stacked_screams`        | `true`                       | several screams at once last longer and, from the third, deal damage |
| `immortal_phantoms`      | `false`                      | a full loop makes the phantom immortal; off also takes immortality away |
| `flight_speed`           | `1.25`                       | flight speed of tamed and released phantoms relative to a wild one |
| `insomnia_days`          | `1`                          | days awake before vanilla phantoms can spawn |
| `phantom_group_multiplier` | `2.0`                      | how many times a vanilla phantom group is multiplied |

Client config: `config/tamedphantoms-client.toml` (or Mods → Config in single-player).

| Parameter            | Default | Meaning                             |
| -------------------- | ------- | ----------------------------------- |
| `tamed_sound_volume` | `0.5`   | tamed phantom volume on this client |
| `owned_flight_speed` | `1.25`  | flight speed of your own tamed phantoms; it can only go below the server `flight_speed` |

The client config also has the button *Where are my phantoms*. It asks the server to print your phantoms in chat.

An invalid item id falls back to the default and a warning is written to the log.

## API for addons

`com.tamedphantoms.mod.api.TamedPhantomsApi` runs on the server:

- `registerInteraction` gets a right-click on a tamed or released phantom before the built-in actions, with the clicked part (`BODY` or `SADDLE`). Return `null` to pass.
- `registerReleaseListener` is called when a phantom is released, while the saddle is still on.
- `registerRiderTool` names items that also work from the saddle. A rider cannot target their own mount, so a right-click in the air with such an item goes to `registerInteraction` as a click on the part of the phantom the rider looks at. Register it on both sides: the client decides whether to send the click.

The phantom keeps and syncs its look itself: `bodyDye` and `saddleDye` (a `DyeColor` id or `NO_DYE`), `luminous`, `startShake(style)`, and `provokeBy(player)`. A removed saddle loses its dye.

## License

The code is [MPL-2.0](LICENSE). Vanilla Minecraft textures are not shipped: eye and body recoloring is computed on the client from the installed game.
