# Alexia Chronicles (Unreal Engine 5)

3D akčná RPG v štýle HIT2: otvorený svet, combo boj, 4 postavy na výber, monštrá podľa biómov, 4 bossovia, loot a level-up. Celá logika je v C++.

## Spustenie
1. Nainštaluj **Unreal Engine 5.4 alebo novší** (Epic Games Launcher) a **Visual Studio 2022** s balíkom „Game development with C++“.
2. Otvor `AlexiaChronicles.uproject` → na otázku „Rebuild modules?“ daj **Yes**.
3. Stlač **Play**. Hra si sama vygeneruje svet, svetlo a oblohu.

Ovládanie na PC: WASD pohyb, ľavé tlačidlo myši / J útok, Q a E schopnosti, R elixír, I taška, koliesko zoom.
Na mobile: joystick vľavo, tlačidlá útoku a schopností vpravo.

## Postavy
| Postava | Štýl | Schopnosti |
|---|---|---|
| Bojovník | meč a štít, veľa života | combo 3 úderov, Vír, Úskok |
| Vrah | dve dýky, kritické zásahy | Tieňový úder, Skok |
| Mág | ohnivé gule na diaľku | Meteor, Teleport |
| Lukostrelec | šípy na diaľku | Salva 7 šípov, Úskok |

## Grafika zadarmo z Fab.com
Bez modelov sa všetko kreslí zo základných tvarov. Pre vzhľad ako v HIT2:
1. Na **fab.com** si zadarmo pridaj napr. *Paragon* postavy (Greystone, Kallari, Gideon, Sparrow), *Infinity Blade* nepriateľov a prostredia, alebo *Quixel Megascans*.
2. V editore: **Edit → Project Settings → Game → Alexia Chronicles**.
3. Pri každej postave vyplň `Mesh`, `Anim Class` a 3 `Attack Montages` pre combo. Pri nepriateľoch rovnako, plus `Death Montage`.
4. V časti **Svet** môžeš priradiť stromy, skaly a materiály zeme pre každý bióm.
5. Ak chceš vlastnú mapu s krajinou, vypni `Generate World`.

## APK pre Android
Unreal APK nevie zostaviť GitHub zadarmo, robí sa na PC:
**Platforms → Android → Package Project** (editor ti ponúkne doinštalovať Android SDK/NDK).
