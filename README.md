# 🌴 Green Hell Mobile Survival 3D

[![Android Build](https://img.shields.io/badge/Android-Game-brightgreen.svg)](https://github.com/havetkin-cyber/GreenHellMobileSurvival)
[![Platform](https://img.shields.io/badge/Platform-Android%2013%2B-blue.svg)](https://github.com/havetkin-cyber/GreenHellMobileSurvival)
[![OpenGL ES](https://img.shields.io/badge/Graphics-OpenGL%20ES%203.0-orange.svg)](https://github.com/havetkin-cyber/GreenHellMobileSurvival)

Plnohodnotná 3D mobilná hra na štýl **Green Hell** vyvíjaná v rodom prostredí **Android (Kotlin + OpenGL ES 3.0 + Jetpack Compose)**.

---

## 📲 Stiahnutie APK (Download)

Súbor s hotovou hrou sa nachádza priamo v repozitári:
📦 **[`GreenHellSurvivalGame.apk.zip`](GreenHellSurvivalGame.apk.zip)** *(Stiahni, rozbal .zip a nainštaluj .apk do svojho Android zariadenia)*.

---

## 🎮 Herné Funkcie & Mechaniky (Green Hell Features)

### ⌚ 1. Nudzové Inteligentné Hodinky (Green Hell Smartwatch)
* **Makroživiny (Macros)**:
  * 🟡 **Sacharidy (Carbohydrates)**: Dopĺňajú sa ovocím (banány, kokosy).
  * 🔴 **Bielkoviny (Proteins)**: Dopĺňajú sa pečeným mäsom z lovu.
  * 🟢 **Tuky (Fats)**: Dopĺňajú sa kokosmi a tukom zo zveri.
  * 🔵 **Hydratácia (Hydration)**: Dopĺňa sa pitím z čutory alebo rieky.
* **Kompas & Čas**: Zobrazuje presný čas, deň prežitia a tep srdca ❤️.

### 🩸 2. Kontrola Tela & Rán (Body Inspection System)
* Interaktívna kontrola končatín: **Ľavá ruka**, **Pravá ruka**, **Ľavá noha**, **Pravá noha**.
* Detekcia zranení: **Pijavice (Leeches)**, **Rany (Cuts)**, **Uštipnutia hadom**.
* Možnosť manuálneho odtrhnutia pijavíc ✋ alebo priloženia bylinkového obväzu z listu Molineria 🩹.

### 🪓 3. Lišta Rýchleho Prístupu (Hotbar)
* 6 rýchlych slotov v spodnej časti obrazovky pre plynulé prepínanie nástrojov a zbraní.
* Vybraný nástroj sa okamžite vykreslí v 3D ruke postavy s animáciou švihu/útoku.

### 🎒 4. Batoh & Výroba (Inventory & Crafting Notebook)
* **Recepty (Crafting Recipes)**:
  * 🪓 **Kamená sekera**: 2x Palica + 1x Kameň + 1x Vlákno
  * 🗡️ **Drevená kopija**: 1x Dlhá palica + 1x Ostrý kameň (určená na lov)
  * 🔥 **Hooriaca fakľa**: 1x Palica + 1x Vlákno + 1x Živica
  * 🏕️ **Ohnisko**: 4x Palice + 4x Kamene
  * 🛖 **Prístrešok**: 4x Dlhé palice + 6x Palmové listy
  * 🩹 **Bylinkový obväz**: 2x Listy Molinerie
  * 🥥 **Kokosová čutora**: 1x Kokos + 1x Vlákno

### 🌴 5. 3D Svet & Atmosféra (3D Engine)
* Vykresľovanie v **OpenGL ES 3.0** pri 60 FPS.
* Hustá amazonská džungľa: Palmy, stromy, bambusy, kríky, rieka, skaly a voľne sa pohybujúca zver.
* Dynamický cyklus dňa a noci (Svit, Deň, Západ slnka, Tmavočierna noc).
* Osvetlenie fakľou a ohňom v noci.

### 🎵 6. Zvukový Syntetizátor (Procedural Audio Engine)
* Procedurálne generované zvuky pre rúbanie dreva, útoky, kroky v tráve, pitie vody a stravovanie.

---

## 🏗️ Technická Štruktúra
* **Jazyk**: Kotlin
* **Grafické rozhranie UI**: Jetpack Compose (Material 3)
* **3D Renderer**: Custom OpenGL ES 3.0 (`GLES30`)
* **Zvukový Engine**: Native AudioTrack Procedural Synthesizer
* **Ukladanie**: Automatické ukladanie do lokálneho úložiska (SharedPreferences)

---

Developed for **havetkin-cyber** 🚀
