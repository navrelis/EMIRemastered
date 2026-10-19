# Credits and licences

EMI Remastered is built on the work of the authors below. Every project was merged from the exact commit listed here;
the sources of the add-ons were ported to the mappings and APIs of this mod (Yarn, Fabric API, EMI's own config) and then
extended. Thank you to all of them.

The owner of EMI Remastered (navrelis) asked the authors of the merged mods for permission to use and change their code as
much as needed.

## Upstream projects

| Part of EMI Remastered | Authors | Repository | Ref used | Licence |
|---|---|---|---|---|
| EMI core (`dev.emi.emi`) 1.1.24 | Emi (emilyploszaj) | https://github.com/emilyploszaj/emi | tag `1.1.24+1.21.1` (322171e) | MIT, Copyright (c) 2022 Emi |
| Module `distraction_free`: Distraction Free Recipes 1.2.2 | Toni (txnimc) | https://github.com/txnimc/DistractionFreeRecipes | branch `template`, commit 742836b | MIT, Copyright (c) 2024 Toni |
| Module `effects`: EMIffect 2.1.5 | Flamarine (original author), Prismwork | https://github.com/Prismwork/EMIffect | branch `stonecutter`, commit 5340688 | MIT, Copyright (c) 2022-2023 Flamarine, 2023-2025 Prismwork |
| Module `professions`: EMI Professions 1.0.3 | Mrbysco, ShyNieke | https://github.com/Mrbysco/EMIProfessions | branch `main`, commit 9c2a7ac | MIT, Copyright (c) 2024 Mrbysco |
| Module `enchanting`: EMI Enchanting 0.1.2 | fzzyhmstrs | https://github.com/fzzyhmstrs/EMI_enchanting | branch `master`, commit 40e631d | MIT, Copyright (c) 2023 fzzyhmstrs |
| Module `ores`: EMI Ores 1.3 | Abbie | https://github.com/Abbie5/emi-ores | tag `v1.3+1.21.1` (41c3537) | Code: LGPL-3.0-only. Biome icons: CC BY-NC-SA 3.0 (see below) |
| Module `letsdo`: Let's Do EMI Compat 2.0.3 | Pandaismyname1 | https://github.com/Pandaismyname1/Let-s-Do-EMI-Compat | branch `1.21.1`, commit 5c8dad1 | MIT, Copyright 2024 Pandaismyname1 |
| Module `loot`: EMI Loot 0.7.9 | fzzyhmstrs; art lxly9; ports unilock | https://github.com/fzzyhmstrs/EMI_loot | branch `architectury/1.21`, commit 6ba879f | MIT (README badge and `fabric.mod.json` "MIT License"; the repository has no LICENSE file) |

Notes per project:

- **EMI** is the base of this mod. The `dev.emi.emi` packages, class names and members are kept so that other mods that
  use EMI's internals keep working. The fork history starts at EMI's tag `1.1.24+1.21.1`.
- **Distraction Free Recipes**: the jar metadata of 1.2.2 says "Tonis MMC License", the repository ships an MIT
  `LICENSE.md` (Copyright (c) 2024 Toni). Its behaviour is built into EMI's screen code instead of mixins.
- **EMIffect**: the build metadata names Flamarine as the author, the repository belongs to Prismwork and the licence file
  lists both names. The status effect stack type id `emiffect:effect` is kept.
- **EMI Loot**: the condition icon art is by lxly9 (https://github.com/lxly9), the platform ports are by unilock, as listed
  in the upstream `fabric.mod.json`. The upstream configuration library (fzzy_config) is replaced by EMI's own config.
- **Let's Do EMI Compat**: the upstream `fabric.mod.json` lists no authors; the licence file names Pandaismyname1. The
  recipe layouts are for the mods of the Let's Do family (Vinery, Meadow, Herbal Brews, Beach Party, Bakery).

## Files with LGPL headers (module `ores`)

All Java files of `dev.emi.remastered.ores` that come from EMI Ores carry the header

```
Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
```

This covers every file under `src/main/java/dev/emi/remastered/ores/` except `OresEmiPlugin.java` and
`mixin/package-info.java` (framework files written for EMI Remastered; `OresModule.java` names the licence in its class
comment). New classes added to the module later (for example the sync classes in `ores/sync`) carry the same header. The licence text is in `assets/emi_ores/LICENSE-LGPL-3.0.txt` (in the jar and in the repository).

The LGPL applies to the code of the ores module, not to the rest of the mod. The rest of EMI Remastered is MIT. The
complete source code of EMI Remastered is public at https://github.com/navrelis/EMIRemastered.

## Biome icons (CC BY-NC-SA 3.0)

The vanilla biome icons (`assets/minecraft/textures/biome/*.png`, 64 files) are licensed under
[CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/) by the Minecraft Fandom Wiki contributors and were
taken from the [Biome wiki page](https://minecraft.fandom.com/wiki/Biome). They were added to EMI Ores by Abbie and are
carried over unchanged. They are **not** covered by the MIT or LGPL licence of the code: they stay under
CC BY-NC-SA 3.0 (attribution, non-commercial, share-alike). The same notice is in `assets/emi_ores/textures/CREDITS.txt`
inside the jar.

## MIT licence

The MIT projects above are used under this licence, with the copyright lines listed in the table. The jar contains this
file and the licence texts at its root (`CREDITS.md`, `LICENSE`, `LICENSE-LGPL-3.0.txt`).

```
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

`LICENSE` in the repository root is the original EMI licence (Copyright (c) 2022 Emi).

## EMI Remastered

Maintained by navrelis, https://github.com/navrelis/EMIRemastered. New code of the modules `info`, `mobs`, `tools`,
`stats` and `world` and the changes listed in `CHANGELOG.md` are MIT-licensed like the rest of the mod, as declared in
`fabric.mod.json` (`MIT`, plus `LGPL-3.0-only` for the ores module).

Built with Fabric Loader, Fabric API and Fabric Loom (FabricMC), Yarn mappings (FabricMC) and Mixin (SpongePowered).
