<div align="center">
  <img src="assets/logo.png" alt="AstraAuction logo" width="220" />

# 💫 AstraAuction β
**面向 [Lumi](https://github.com/koshakminedev/lumi) 的市场（拍卖）插件。**

**[EN](README.md)** | **[RU](README_ru.md)** | **[UA](README_ua.md)** | **[JA](README_ja.md)** | **ZH**

</div>

## 介绍 📖
AstraAuction 是 [Lumi](https://github.com/koshakminedev/lumi) 的玩家间交易市场插件。
插件完整保留物品 NBT，将数据存入数据库，并提供包含搜索、排序与上架管理的简洁 GUI。

## 功能 🌟
- **保留 NBT** — 物品按卖家原样出售。
- **异步数据库** — 所有查询在后台执行，不阻塞主线程。
- **交易手续费** — 可配置的税率百分比。
- **退回仓库（claims）** — 未售出物品与离线卖家的款项会保存至过期。
- **GUI 市场** — 购买、确认对话框与翻页。
- **排序与搜索** — 按价格排序、按名称搜索。
- **管理你的上架** — 通过 GUI 查看并取消当前上架。
- **灵活的数据库支持** — SQLite / MySQL / PostgreSQL。
- **本地化** — 内置 `ru` *(русский)*、`en` *(English)*、`ua` *(українська)*、`ja` *(日本語)*、`zh` *(中文)*，并支持添加自定义语言。

## 截图 🖼️
| **市场主页面** | **我的上架** |
|---|---|
| ![](assets/base.jpg) | ![](assets/my_lots.jpg) |

| **退回仓库页面** | **购买确认页面** |
|---|---|
| ![](assets/claims.jpg) | ![](assets/confirm.jpg) |

## 命令 ♿
| **命令** | **说明** | **权限** |
|---|---|---|
| `/ah` | 打开市场 | `astraauction.use` |
| `/ah open,gui [page: int]` | 在指定页打开市场 | `astraauction.use` |
| `/ah sell <price: int>` | 上架手中物品 | `astraauction.use` |
| `/ah view <nickname: str> [page: int]` | 查看指定玩家的上架 | `astraauction.use` |
| `/ah search <query: str> [page: int]` | 按名称搜索上架 | `astraauction.use` |
| `/ah force_buy <id: int>` | 强制购买上架（流程与普通购买一致） | `astraauction.force` |
| `/ah force_expire <id: int>` | 强制结束上架 | `astraauction.force` |

**命令别名:** `/auction`, `/auc`.

## 权限 🔐
- `astraauction.use` — 基础命令权限（默认所有人可用）。
- `astraauction.force` — 强制命令权限（默认仅 OP）。

## 配置 ⚙️
主配置文件: [config.yml](src/main/resources/config.yml)

| 参数 | 说明 | 默认值 |
|---|---|---|
| `database.type` | 数据库类型: `sqlite` / `mysql` / `postgres` | `sqlite` |
| `language` | 消息语言 | `ru` |
| `auction.duration-seconds` | 上架时长（秒） | `172800` (48h) |
| `auction.tax-percent` | 交易手续费（%） | `10.0` |
| `auction.max-slots` | 最大同时上架数（0 = 不限） | `6` |
| `auction.claim-expire-seconds` | 仓库保留时间（秒） | `604800` (7d) |
| `auction.gui.page-size` | GUI 每页数量（最多 45） | `45` |
| `auction.gui.sort-default` | 默认排序 | `price_asc` |

## 依赖 🔌
- **[EconomyAPI](https://cloudburstmc.org/resources/economyapi.14/)** — 购买与结算必需。
- **[FakeInventories](https://github.com/JkqzDev/FakeInventories-MOT)** — GUI 必需。
- **[sql2o-nukkit](https://github.com/hteppl/sql2o-nukkit)** *(前身 **[DataManager](https://cloudburstmc.org/resources/datamanager.892/)**)* — 数据库支持必需。

## 从源码构建 🔨
1. 安装 **[JDK 21+](https://www.google.com/search?q=jdk+21)**。
2. [克隆仓库](https://www.google.com/search?q=how+to+clone+git+repository)并打开项目目录。
3. 运行构建命令: `./gradlew build`
4. 编译后的 JAR 位于 `build/libs`。

### 构建注意事项 ⚠️
仓库中提供了两个 `build.gradle` 文件:
一个是主文件（使用 Lumi 仓库），另一个带 `.old` 后缀（不使用 Lumi 仓库，全部通过 `jitpack.io`）。

当 Lumi 仓库暂时不可用（例如 HTTP 500）导致构建失败时，请使用 `build.gradle.old`。
只需临时删除（或移动）当前的 `build.gradle`，并将 `build.gradle.old` 去掉 `.old` 后缀即可。
