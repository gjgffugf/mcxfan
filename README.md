# AllItemsTotem

一个 Paper 1.21.11 插件：**让背包里的所有物品都拥有不死图腾的效果**。

- 受到致命伤害时自动触发，和原版不死图腾一样原地复活；
- 触发时**消耗该物品 1 个**（物品原来的功能，如放置、食用、合成、附魔等完全不受影响）；
- **无冷却**：只要身上还有物品就能连续触发（可自行配置复活后的短暂无敌时间）；
- 完整的不死图腾表现：`TOTEM_OF_UNDYING` 粒子、`ITEM_TOTEM_USE` 音效、再生 II / 伤害吸收 II / 防火效果；
- 触发前会抛出 `EntityResurrectEvent`，其它插件仍然可以否决这次复活；
- 兼容 Folia（不使用全局调度器）。

## 触发与消耗顺序

主手 → 副手 → 背包（含快捷栏）→ 盔甲栏（默认关闭）。

默认不消耗盔甲槽物品（`include-armor: false`），避免出现"被打一下头盔没了"的情况。盔甲栏的优先级也最低，开启后只在其它格子全空时才会用到。

## 安装

1. 到 Actions 里下载构建好的 `AllItemsTotem-1.0.0.jar`（或自己 `mvn clean package`）；
2. 放进服务器的 `plugins/` 目录；
3. 重启服务器，首次启动会生成 `plugins/AllItemsTotem/config.yml`；
4. 修改配置后用 `/allitemstotem reload`（别名 `/ait`）重载，无需重启。

## 配置（config.yml）

| 配置项 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | true | 总开关 |
| `consume-item` | true | 是否消耗物品；`false` 则无限复活 |
| `include-armor` | false | 是否允许消耗盔甲栏物品 |
| `heal-health` | 1.0 | 复活后恢复到的血量 |
| `clear-effects` | true | 复活时清除已有药水效果（原版行为） |
| `extinguish` | true | 复活时熄灭火焰 |
| `invulnerable-ticks` | 0 | 复活后无敌时间；`0` = 完全无冷却 |
| `require-permission` | false | 开启后需要 `allitemstotem.use` 权限 |
| `excluded-causes` | VOID, KILL | 不触发复活的伤害类型 |
| `blacklist` | AIR | 不能用来复活的材质（填材质英文名，如 `DIAMOND`） |
| `effects` | 再生II / 吸收II / 防火 | 复活后给予的效果，`amplifier` 从 0 开始（0 = I 级） |
| `particles` / `sound` | 开启 | 粒子数量与音效开关 |

权限：`allitemstotem.use`（默认所有人，仅在 `require-permission: true` 时生效）、`allitemstotem.admin`（默认 OP，用于重载配置）。

## 本地构建

需要 **JDK 21** 与 Maven：

```bash
mvn clean package
```

产物在 `target/AllItemsTotem-1.0.0.jar`。

## GitHub Actions

`.github/workflows/build.yml` 已在 push / PR 时自动构建并上传 jar；推送 `v*` 标签时会自动创建 Release 并附带 jar：

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 调整目标版本

修改 `pom.xml` 里的 `paper.version`（如 `1.21.11-R0.1-SNAPSHOT`）与 `src/main/resources/plugin.yml` 的 `api-version` 即可适配其它 Paper 版本。

## 已知行为

- 与原版一致：虚空伤害、`/kill` 造成的伤害不会触发复活。
- 创造 / 旁观模式不触发。
- 工具、武器等不可堆叠物品按"整件消耗"处理（消耗 1 件，不会只扣耐久）。
