<div align="center">
  <img src="assets/logo.png" alt="AstraAuction logo" width="220" />

# 💫 AstraAuction β
**Market (Auction) plugin for [Lumi](https://github.com/koshakminedev/lumi).**

**EN** | **[RU](README_ru.md)** | **[UA](README_ua.md)** | **[JA](README_ja.md)** | **[ZH](README_zh.md)**

</div>

## Description 📖
AstraAuction is a player-to-player marketplace plugin for [Lumi](https://github.com/koshakminedev/lumi).
The plugin preserves full item NBT, stores data in a database, and provides a clean and convenient GUI with search, sorting, and lot management.

## Features 🌟
- **NBT preservation** — items are sold exactly as they were owned by the seller.
- **Asynchronous database** — all queries run in the background without blocking the main thread.
- **Transaction fee** — configurable tax percentage.
- **Return storage (claims)** — unsold lots and payments for offline sellers are stored until expiration.
- **GUI market** — buying, confirmation dialogs, and page navigation.
- **Sorting and search** — sort by price and search by item name.
- **Manage your lots** — view and cancel active lots via GUI.
- **Flexible database support** — SQLite / MySQL / PostgreSQL.
- **Localization** — built-in support for `ru`, `en`, `ua`, `ja`, `zh`, with the ability to add custom languages.

## Screenshots 🖼️
| **Main market page** | **My lots page** |
|---|---|
| ![](assets/base.jpg) | ![](assets/my_lots.jpg) |

| **Return storage page** | **Purchase confirmation page** |
|---|---|
| ![](assets/claims.jpg) | ![](assets/confirm.jpg) |

## Commands ♿
| **Command** | **Description** | **Permission** |
|---|---|---|
| `/ah` | Open the market | `astraauction.use` |
| `/ah open,gui [page: int]` | Open the market at a specific page | `astraauction.use` |
| `/ah sell <price: int>` | List the item in hand | `astraauction.use` |
| `/ah view <nickname: str> [page: int]` | View lots of a specific player | `astraauction.use` |
| `/ah search <query: str> [page: int]` | Search lots by name | `astraauction.use` |
| `/ah force_buy <id: int>` | Force-buy a lot (processed like a normal purchase) | `astraauction.force` |
| `/ah force_expire <id: int>` | Force-expire a lot | `astraauction.force` |

**Command aliases:** `/auction`, `/auc`.

## Permissions 🔐
- `astraauction.use` — access to basic commands (granted to everyone by default).
- `astraauction.force` — access to force commands (granted to operators only by default).

## Configuration ⚙️
Main configuration file: [config.yml](src/main/resources/config.yml)

| Parameter | Description | Default |
|---|---|---|
| `database.type` | Database type: `sqlite` / `mysql` / `postgres` | `sqlite` |
| `language` | Message language | `ru` |
| `auction.duration-seconds` | Lot lifetime (seconds) | `172800` (48h) |
| `auction.tax-percent` | Transaction fee (%) | `10.0` |
| `auction.max-slots` | Max active lots (0 = unlimited) | `6` |
| `auction.claim-expire-seconds` | Claims storage duration (seconds) | `604800` (7d) |
| `auction.gui.page-size` | GUI page size (up to 45) | `45` |
| `auction.gui.sort-default` | Default sorting | `price_asc` |

## Dependencies 🔌
- **[EconomyAPI](https://cloudburstmc.org/resources/economyapi.14/)** — required for purchases and payouts.
- **[FakeInventories](https://github.com/JkqzDev/FakeInventories-MOT)** — required for GUI support.
- **[sql2o-nukkit](https://github.com/hteppl/sql2o-nukkit)** *(formerly **[DataManager](https://cloudburstmc.org/resources/datamanager.892/)**)* — required for database support.

## Building from Source 🔨
1. Install **[JDK 21+](https://www.google.com/search?q=jdk+21)**.
2. [Clone the repository](https://www.google.com/search?q=how+to+clone+git+repository) and open the project directory.
3. Run the build command: `./gradlew build`
4. The compiled JAR will be located in `build/libs`.

### Build Warning ⚠️
The repository contains two `build.gradle` files:
one main file (using fallback repositories via `jitpack.io`) and another with the `.old` suffix (with original Lumi repositories).

If Lumi repositories are temporarily unavailable (e.g. HTTP 500 errors) and the build fails, just use the current `build.gradle` as-is.
Alternatively, to use the original Lumi repositories, remove or move the current `build.gradle`, and rename `build.gradle.old` by removing the `.old` suffix.
