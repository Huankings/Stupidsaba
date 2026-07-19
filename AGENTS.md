# AGENTS.md

本文件是 StupidExpress 扩展职业、词条、特殊胜利和跨模组联动的本地开发说明。后续在本仓库新增职业、修改词条、调整胜利规则、修复 Wathe/Harpy 兼容问题时，先按这里的顺序读源码，再决定修改范围。

## 固定项目路径

| 项目 | 路径 | 用途 |
| --- | --- | --- |
| StupidExpress 当前开发仓库 | `D:\哈比快车最新源码\stupidexpress\StupidExpress2.1` | 当前主要修改目标。 |
| Wathe | `D:\哈比快车最新源码\wathe\Wathe - 副本1` | 类狼人杀玩法本体，提供角色、商店、经济、任务、回放、胜利、外观、心情、本能等 API。 |
| HarpyModLoader | `D:\哈比快车最新源码\harpymodloader\HarpyModLoader1` | 扩展职业/词条加载、角色分配、强制角色、权重、词条池。 |
| NoellesRoles | `D:\哈比快车最新源码\noellesroles\NoellesRoles - 副本 - 副本 - 副本5.7.1` | 其他扩展职业参考；StupidExpress 的小偷规则也引用了部分 noellesroles 物品 id。 |
| kinssaba | `D:\哈比快车最新源码\kinswathe\kinssaba` | 其他扩展职业参考；小偷规则引用了部分 kinswathe 物品 id。 |
| StarryExpress | `D:\哈比快车最新源码\starryexpress\StarryExpress1.3.2` | 其他扩展职业参考；小偷规则引用了部分 starryexpress 物品 id。 |

`README.md` 里有历史说明，但当前已经偏旧，实际开发以源码和本文件为准。

## 重要前提

- 本仓库使用 `loom.officialMojangMappings()`，源码里的类名/方法名是 Mojang 官方映射风格，例如 `ResourceLocation`、`Component`、`ServerPlayer`。不要直接复制 NoellesRoles 里的 Yarn 命名代码。
- 能用 Wathe 公开 API 时优先用 API，避免写深层 mixin。确实需要 mixin 时，条件要尽量窄，并明确区分服务端、客户端、兼容 mixin。
- 新增或重构代码时，关键逻辑必须写详细中文注释，尤其是胜利仲裁、词条配对、强制分配、同步组件、聊天/语音隔离、回合结束清理这些容易出错的地方。
- 新增玩法数值尽量放入该职业/词条自己的常量类或配置项。旧代码里已有少量散落数值，不要为了整理文档而顺手大重构。

## 先读哪些源码

### 总入口和基础注册

- `src/main/java/pro/fazeclan/river/stupid_express/StupidExpress.java`
- `src/main/java/pro/fazeclan/river/stupid_express/StupidExpressConfig.java`
- `src/main/java/pro/fazeclan/river/stupid_express/constants/SERoles.java`
- `src/main/java/pro/fazeclan/river/stupid_express/constants/SEModifiers.java`
- `src/main/java/pro/fazeclan/river/stupid_express/constants/SEItems.java`
- `src/main/java/pro/fazeclan/river/stupid_express/constants/SEComponents.java`
- `src/main/resources/fabric.mod.json`

`StupidExpress.onInitialize()` 当前初始化顺序是：角色、词条、胜利规则、尸体外观、通讯限制、命令、物品、回放。后续新增初始化入口时，要考虑是否依赖角色/词条已经注册。

### 特殊胜利和独立阵营

- `src/main/java/pro/fazeclan/river/stupid_express/victory/StupidExpressVictoryRules.java`
- `src/main/java/pro/fazeclan/river/stupid_express/victory/StupidExpressVictoryUtil.java`
- `src/main/java/pro/fazeclan/river/stupid_express/modifier/lovers/LoversVictoryRule.java`
- `src/main/java/pro/fazeclan/river/stupid_express/modifier/dual_personality/DualPersonalityVictoryRule.java`
- `src/main/java/pro/fazeclan/river/stupid_express/role/arsonist/ArsonistVictoryRule.java`
- `src/main/java/pro/fazeclan/river/stupid_express/role/convener/ConvenerVictoryRule.java`
- `src/main/java/pro/fazeclan/river/stupid_express/role/convener/ConvenerWinHelper.java`
- `src/main/java/pro/fazeclan/river/stupid_express/role/thief/ThiefVictoryRule.java`

StupidExpress 的“特殊阵营”主要体现在胜利层：某些职业/词条虽然仍挂在 Wathe/Harpy 的角色或词条系统里，但结算时会成为自己的独立胜利分组。

### 角色和词条包

- 角色：`src/main/java/pro/fazeclan/river/stupid_express/role/amnesiac`
- 角色：`src/main/java/pro/fazeclan/river/stupid_express/role/arsonist`
- 角色：`src/main/java/pro/fazeclan/river/stupid_express/role/avaricious`
- 角色：`src/main/java/pro/fazeclan/river/stupid_express/role/convener`
- 角色：`src/main/java/pro/fazeclan/river/stupid_express/role/initiate`
- 角色：`src/main/java/pro/fazeclan/river/stupid_express/role/necromancer`
- 角色：`src/main/java/pro/fazeclan/river/stupid_express/role/thief`
- 词条：`src/main/java/pro/fazeclan/river/stupid_express/modifier/lovers`
- 词条：`src/main/java/pro/fazeclan/river/stupid_express/modifier/dual_personality`

当前核心语义：

- `AMNESIAC`：中立，尸体取职业。
- `ARSONIST`：中立/独立胜利，点燃和保活逻辑在纵火犯包。
- `AVARICIOUS`：杀手阵营，特殊金币结算。
- `NECROMANCER`：杀手阵营，复活相关逻辑。
- `INITIATE`：中立，专属商店和可变身份逻辑。
- `THIEF`：中立/独立胜利，小偷可偷物品和可保活武器在 `ThiefItemRules`。
- `CONVENER`：中立/独立胜利，召集、伪装、锁定、反伤护盾和胜利进度都在召集者包。
- `LOVERS`：词条，成对生命连接，支持独立胜利和配置化共胜。
- `DUAL_PERSONALITY`：词条，主/副人格配对、轮换、双活、独立胜利和配置化共胜。

### 商店、经济和物品

- `src/main/java/pro/fazeclan/river/stupid_express/shop/SEShops.java`
- `src/main/java/pro/fazeclan/river/stupid_express/shop/SEShopRegistry.java`
- `src/main/java/pro/fazeclan/river/stupid_express/shop/RoleShopProvider.java`
- `src/main/java/pro/fazeclan/river/stupid_express/shop/DirectGiveShopEntry.java`
- `src/main/java/pro/fazeclan/river/stupid_express/role/initiate/InitiateShopHandler.java`
- `src/main/java/pro/fazeclan/river/stupid_express/role/avaricious/AvariciousGoldHandler.java`

商店优先走 Wathe `ShopApi` 和本仓库的 `SEShops.provider(...)`。非杀手职业需要直接发物品时优先复用 `DirectGiveShopEntry`，避免 Wathe 原版 `ShopEntry` 的杀手权限判断挡住交付。

### 客户端、外观、通讯、回放

- `src/client/java/pro/fazeclan/river/stupid_express/client/StupidExpressClient.java`
- `src/client/java/pro/fazeclan/river/stupid_express/client/instinct`
- `src/client/java/pro/fazeclan/river/stupid_express/client/appearance`
- `src/client/java/pro/fazeclan/river/stupid_express/client/mixin`
- `src/client/java/pro/fazeclan/river/stupid_express/client/modifier`
- `src/main/java/pro/fazeclan/river/stupid_express/appearance/StupidExpressBodyAppearanceHandlers.java`
- `src/main/java/pro/fazeclan/river/stupid_express/communication/StupidExpressCommunicationManager.java`
- `src/main/java/pro/fazeclan/river/stupid_express/voice/StupidExpressVoiceChatPlugin.java`
- `src/main/java/pro/fazeclan/river/stupid_express/record/StupidExpressReplay.java`
- `src/main/java/pro/fazeclan/river/stupid_express/record/StupidExpressReplayFormatters.java`

聊天限制和语音限制必须一起检查。`StupidExpressCommunicationManager` 处理普通聊天和统一规则判断，`StupidExpressVoiceChatPlugin` 处理 Simple Voice Chat 的声音包过滤和双重人格静态语音补发。

## Harpy 分配、特殊池和强制指定

- 职业最大数在 `SERoles.init()` 里通过 `Harpymodloader.setRoleMaximum(role, max)` 控制。
- 词条最大数在 `SEModifiers.init()` 里通过 `Harpymodloader.MODIFIER_MAX` 控制。
- 恋人和双重人格的真正配对状态分别保存在 `LoversPairComponent` 和 `DualPersonalityComponent` 世界组件里。
- `/stupidexpress setlovers` 和 `/stupidexpress setdual_personality` 只写入下一局强制队列；真正加词条和写组件发生在 Harpy 分配词条阶段。
- `ForceNeutralRoleFixMixin` 专门修 Harpy 强制中立/覆盖角色的分配边界。新增或调整中立替换型职业、强制角色、`OVERWRITE_ROLES` 相关逻辑时，必须重新检查这个 mixin。

## 特殊胜利规则

新增或调整独立胜利时，优先接入 Wathe `VictoryApi`，不要回到旧式 `MurderGameMode` 胜利循环 mixin。

当前注册顺序在 `StupidExpressVictoryRules.init()`：

1. `LoversVictoryRule`：priority 100。恋人和双重人格同时满足独胜时，恋人展示优先。
2. `DualPersonalityVictoryRule`：priority 90。双重人格全部剩余存活者都带词条时独立胜利。
3. `ArsonistVictoryRule`：默认优先级。纵火犯可按配置拖住普通阵营结算，只剩纵火犯时独胜。
4. `ConvenerVictoryRule`：默认优先级。召集者存活时拖住杀手/乘客结算，只剩召集者时独胜；主动达成召集目标时由 `ConvenerWinHelper.declareConvenerWin(...)` 立即结束。
5. `ThiefVictoryRule`：默认优先级。小偷有可用武器目标时拖住普通结算，只剩小偷时独胜。

规则返回值语义：

- 完全不参与本 tick：`VictoryApi.VictoryResult.pass()`。
- 普通杀手/乘客结算要被特殊阵营延后：`VictoryApi.VictoryResult.keepRunning()`。
- 特殊玩家跟随原版阵营共胜：`VictoryApi.VictoryResult.vanillaWin(winStatus, extraWinnerUuids)`。
- 普通职业独立胜利：`StupidExpressVictoryUtil.customWin(roleId, color, livingWinners)`。
- 词条/配对阵营独立胜利：`StupidExpressVictoryUtil.customWinUuids(modifierId, color, winnerUuids)`。

词条胜利尤其要注意：`winnerUuids` 代表“结算页右侧胜利阵营成员”，不只是还活着的触发者。恋人和双重人格都需要把死亡的伴侣/人格也写进去，让 Wathe 客户端用红叉显示在胜利阵营里，而不是被误放到“其他”。

## 组件和同步

- 玩家短期状态放 entity component，例如 `AbilityCooldownComponent`、`DousedPlayerComponent`、`ConvenerPlayerComponent`、`ConvenerDisguiseComponent`、`ConvenerMomentumComponent`。
- 整局共享状态放 world component，例如 `AvariciousPayoutComponent`、`NecromancerComponent`、`LoversPairComponent`、`DualPersonalityComponent`。
- 新增 CCA 组件时同步修改 `SEComponents.java` 和 `fabric.mod.json` 里的 `custom.cardinal-components`。
- 回合开始、停局、结算结束、玩家死亡、玩家掉线都可能留下旧状态；新增状态时必须补清理入口。

## Mixin 分层

- 服务端/common mixin：`src/main/java/pro/fazeclan/river/stupid_express/mixin`，注册在 `stupid_express.mixins.json`。
- 客户端 mixin：`src/client/java/pro/fazeclan/river/stupid_express/client/mixin`，注册在 `stupid_express.client.mixins.json`。
- 跨模组兼容 mixin：`stupid_express_compat.mixins.json`，当前用于恋人与 NoellesRoles executioner 之类的兼容边界。
- 新增 mixin 前先确认没有 Wathe API 或现有事件能完成需求。注入点要写窄条件：当前职业/词条、对局运行状态、玩家存活状态、客户端/服务端环境都要判断。

## 回放、死因和文案

- 新增回放事件先在 `StupidExpressReplay` 声明 `ResourceLocation`，再在 `registerReplayFormatters()` 注册格式化器。
- 格式化器放在 `StupidExpressReplayFormatters`，优先使用 `Component.translatable(...)`，不要把中文/英文句子硬编码进回放数据。
- 新增死因、护盾来源、特殊冷却结束等非瞬时状态，要像打火机冷却那样在服务端 tick 里集中追踪和清理。
- 语言文件在 `src/main/resources/assets/stupid_express/lang/zh_cn.json` 和 `en_us.json`。

## 跨模组物品和兼容

`ThiefItemRules` 用字符串形式引用了 Wathe、NoellesRoles、kinssaba、StarryExpress 的物品 id。修改这些扩展中的物品 id 或删除对应物品时，必须同步检查：

- `KEEP_GAME_GOING`：小偷活着且目标武器仍可用时是否拖住普通结算。
- `CAN_TAKE`：小偷能否偷走目标物品。

因为这些外部物品多数是字符串 id，编译未必能发现写错，必须靠源码比对和实机/集成测试确认。

## 编译顺序

只改 StupidExpress 时：

```powershell
cd "D:\哈比快车最新源码\stupidexpress\StupidExpress2.1"
.\gradlew.bat build
```

如果改了 Wathe，并且 StupidExpress 要使用本地 Wathe 改动：

```powershell
cd "D:\哈比快车最新源码\wathe\Wathe - 副本1"
.\gradlew.bat build
```

然后把 Wathe 输出的 `build\libs\wathe-*.jar` 复制到：

```text
D:\哈比快车最新源码\stupidexpress\StupidExpress2.1\libs
```

再回到 StupidExpress 执行 `.\gradlew.bat build`。当前 `build.gradle` 只对 Wathe 做了 `libs` 本地 jar 优先加载；HarpyModLoader 和 NoellesRoles 默认走依赖坐标。如果你修改了 Harpy/Noelles 的 API 并要让 StupidExpress 立刻编译到本地改动，需要先确认对应依赖版本或本地发布方式已经更新，而不是只复制 jar 后就假定 Gradle 会使用它。

如果只是 NoellesRoles、kinssaba、StarryExpress 的物品 id 发生变化，StupidExpress 可能仍能编译通过，但运行时小偷规则会失效；这种情况要同步改 `ThiefItemRules` 并实测。

## 新增职业/词条检查清单

- `SERoles.java` 或 `SEModifiers.java`：注册角色/词条、颜色、Mood、最大生成数、分配事件。
- `StupidExpressConfig.java`：需要配置开关、共胜开关、人数阈值时补配置。
- `SEComponents.java` 和 `fabric.mod.json`：需要持久/同步状态时注册 CCA 组件。
- `StupidExpressVictoryRules.java`：有独胜、共胜、保活、拖局逻辑时新增规则类并注册。
- `SEShops.java` / `InitiateShopHandler` 风格文件：有专属商店时走 ShopApi provider。
- `SEItems.java`：新增物品、贴图、模型、tooltip。
- `StupidExpressReplay.java` / `StupidExpressReplayFormatters.java`：新增关键事件、死因、护盾来源、冷却追踪。
- `StupidExpressCommunicationManager.java` / `StupidExpressVoiceChatPlugin.java`：有禁言、隔离、特殊语音时两边一起改。
- `StupidExpressClient.java` 和 `src/client/java/...`：HUD、按键、外观、本能、背包界面、客户端状态重置。
- `stupid_express.mixins.json`、`stupid_express.client.mixins.json`、`stupid_express_compat.mixins.json`：只注册确实需要的 mixin，环境要正确。
- `zh_cn.json` / `en_us.json`：职业名、词条名、欢迎公告、HUD、命令反馈、回放、死亡原因、胜利文案都要补齐。

## 后续任务提示词简化

以后可以直接这样提需求：

```text
请按 StupidExpress 的 AGENTS.md 流程，在 StupidExpress 中实现/分析这个职业或词条。
类型：职业 / 词条 / 修 bug / 调胜利
名称和 id：
阵营或独立胜利分组：
是否需要词条胜利、共胜、保活或拖住普通结算：
技能机制：
交互方式：
冷却/范围/金币/人数等数值：
商店/物品：
HUD/本能/外观/语音/聊天：
回放/死因/结算文案：
参考现有角色或词条：
是否先给方案：
编译要求：
```

如果涉及特殊胜利，必须明确：只剩自己是否独胜、是否允许与杀手/乘客共胜、是否需要把死亡队友/伴侣写进胜利阵营、是否要拦截 TIME 超时结算。
