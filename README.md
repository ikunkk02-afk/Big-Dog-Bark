# Big Dog Bark (大狗叫)

一个 Minecraft 1.21.1 (Fabric) 娱乐模组:使用附魔骨头驯服“特殊大狗”、使用附魔种子转化“叮咚鸡”,为后续的成长与武器系统打下基础。

- 中文名:大狗叫
- 英文名:Big Dog Bark
- Mod ID:`big_dog_bark`
- Minecraft:1.21.1 / Fabric Loader 0.19.3 / Fabric API 0.116.15+1.21.1 / Java 21 (Yarn 映射)
- License:MIT

## 功能

### 第一阶段:大狗叫附魔与附魔骨头驯服狼

- 数据驱动附魔 **大狗叫 (Big Dog Bark)**:`big_dog_bark:big_dog_bark`,最高 1 级,只能应用到原版骨头 (`minecraft:bone`)。
- 拿着带“大狗叫 I”的骨头右键未驯服的原版狼,使用**原版驯服概率 (1/3)** 进行驯服。
- 每次尝试消耗一根附魔骨头(创造模式不消耗);驯服成功后狼会被永久标记为**特殊大狗** (NBT 键 `BigDogBark.IsBigDog`,随世界存档持久化,跨维度、重进世界、重启服务端均保留)。
- 普通骨头、已驯服的狼、幼年狼、其他生物全部保持原版行为,不受影响。

### 第二阶段:叮咚鸡附魔与叮咚鸡肉

- 数据驱动附魔 **叮咚鸡 (Ding Dong Chicken)**:`big_dog_bark:ding_dong_chicken`,最高 1 级,只能应用到原版小麦种子 (`minecraft:wheat_seeds`),不能应用到其他种子、骨头、武器、工具、盔甲、弓、弩、食物等其他物品。
- 铁砧用法:左侧放普通小麦种子,右侧放“叮咚鸡 I”附魔书,取出带附魔的小麦种子。
- 拿着带“叮咚鸡 I”的小麦种子(主手或副手)右键原版鸡,**100% 将该鸡转化为叮咚鸡**:
  - 每次成功转化消耗一颗种子(创造模式不消耗);
  - 转化时播放自定义音效并冒出爱心粒子,鸡短暂停止当前寻路,但 AI 不被永久破坏;
  - 鸡仍可移动、下蛋、繁殖、受伤和死亡;
  - 已经是叮咚鸡的鸡不能再次转化,交互交还原版种子逻辑;
  - 普通种子保留原版繁殖与幼年成长行为,对其他动物使用附魔种子没有特殊效果;
  - 附魔是否有效通过动态附魔注册表查询附魔等级,不解析物品名/Lore/附魔光效。
- 叮咚鸡会被永久标记 (NBT 键 `BigDogBark.IsDingDongChicken`,随世界存档持久化,跨维度、重进世界、重启服务端、区块卸载均保留)。
- 叮咚鸡死亡时(任何死亡方式,包括创造模式玩家击杀):
  - 额外掉落 **1 个叮咚鸡肉** `big_dog_bark:ding_dong_chicken_meat`,原版羽毛和其他正常掉落保留;
  - 播放一次自定义死亡音效(替换原版鸡死亡声,不会重复播放);
  - Looting 附魔不增加叮咚鸡肉数量;普通鸡不掉落叮咚鸡肉、不播放自定义死亡音效。
- 新物品 **叮咚鸡肉**:最大堆叠 64,当前阶段**不可食用**,暂作为后续“大狗成长系统”的专用材料;出现在原版“原材料”创造模式物品栏;当前复用原版生鸡肉纹理。物品提示:“似乎可以喂给某种大狗”。
- 自定义音效文件路径:
  - `assets/big_dog_bark/sounds/entity/ding_dong_chicken_convert.ogg`(转化)
  - `assets/big_dog_bark/sounds/entity/ding_dong_chicken_death.ogg`(死亡)
  - `assets/big_dog_bark/sounds/entity/ding_dong_chicken_ambient.ogg`(日常叫声,替换原版鸡叫声,所有鸡生效)

### 第三阶段:特殊大狗成长系统

- 只有**特殊大狗**(已驯服且 `BigDogBark.IsBigDog = true`)能够成长;普通狼、野生狼、其他玩家的狼都不能成长。
- 只有**大狗的主人**能够喂食,其他玩家不能消耗物品或增加进度。
- 使用**叮咚鸡肉**右键喂食(主手/副手均可),每喂食 1 个增加 **1 点成长进度**,共 **12 点**。
- 每 3 点提升一个阶段,共 5 个阶段:

| 阶段 | 进度 | 名称 | Scale |
|---|---|---|---|
| 0 | 0～2 | 普通大狗 | 1.00 |
| 1 | 3～5 | 稍大大狗 | 1.25 |
| 2 | 6～8 | 大型大狗 | 1.50 |
| 3 | 9～11 | 巨型大狗 | 1.75 |
| 4 | 12 | 武器大狗 | 2.00 |

- 幼年特殊大狗不能成长(提示“大狗还没有成年”,不消耗物品),成年后可以正常培养。
- 达到最大进度后继续右键不消耗物品、不增加进度、不重复播放成长音效,提示“这只大狗已经长到最大了”。
- 每次成功喂食播放原版进食音效与少量爱心粒子;跨越阶段时播放自定义成长音效、更明显的粒子并显示阶段提示。
- 成长进度 NBT 键:`BigDogBark.GrowthPoints`(0～12,随存档持久化;旧存档缺省 0)。
- 体型缩放通过原版 `minecraft:generic.scale` 属性的临时修饰符 `big_dog_bark:growth_scale` 实现,同时影响模型、碰撞箱、命中箱与服务端碰撞检测,不会重复叠加;阶段 4(2.00 倍)代表已达到以后可以抱起的体型。
- 变大的大狗保留原版全部行为:跟随、坐下/站起、攻击、保护、传送、游泳、狼铠、项圈颜色、自定义名称、繁殖(后代不继承大狗/成长状态)。
- 自定义成长音效路径:`assets/big_dog_bark/sounds/entity/dog_growth_stage.ogg`。
- **暂未实现**抱起大狗;下一阶段计划是“下蹲 + 右键”抱起最大体型(阶段 4)大狗。

### 第四阶段:抱起与放下大狗

- 只有**主人**可以抱起大狗(其他玩家不能抱起,捡到物品也不能放下)。
- 只有**完全成长**(成长进度 12、阶段 4 武器大狗)的大狗可以被抱起;普通狼、未完全成长的大狗、幼年大狗不能被抱起。
- **操作**:主人**空手 + 下蹲 + 右键**大狗 → 大狗被抱起为物品 `big_dog_bark:carried_big_dog`(抱起的大狗)。
- **放下**:手持抱起的大狗,**下蹲 + 右键方块** → 大狗被放回世界(生成在点击方块面的相邻位置)。
- **普通右键**(不下蹲)为后续蓄能和机枪附魔的右键操作预留:不放下、无特殊效果;空手普通右键大狗仍为原版坐下/站起交互。
- 大狗物品:
  - 最大堆叠 1、防火、不可食用、当前无耐久与主动右键攻击;
  - **不加入普通创造模式物品栏**(`/give` 获得的空物品没有有效数据,不能生成狼);
  - 数据保存在物品 `CUSTOM_DATA` 组件(`BigDogBark.CarriedDog` 根节点):`Version` / `OwnerUuid` / `OwnerName` / `WolfData`(狼实体数据快照);
  - 权限判断只依据 `OwnerUuid`(`OwnerName` 仅用于提示文本,不能作为身份验证依据);
  - 狼数据快照已移除 UUID、坐标、移动、维度、乘客、拴绳、愤怒、导航等危险字段,只恢复永久属性;
  - 放下时强制恢复:已驯服、主人为当前玩家、特殊大狗(`IsBigDog`)、成长进度 12、Scale 2.0、默认坐下、无攻击/愤怒目标;
  - 狼铠、项圈颜色、变种、生命值、状态效果、自定义名称均保留;铁砧重命名物品后放下,狼使用新名称,且 OwnerUuid/WolfData/成长数据不丢失;
  - **创造模式放下也会消耗物品**(该物品代表一只具体实体,防止无限复制);
  - 放下位置空间不足时物品不消耗、大狗不生成(按 2 倍实际碰撞箱检查);
  - 当前使用**立体狼模型**作为物品显示(JSON 模型:头/身体/四条腿/尾巴,复用狼纹理),后续武器阶段会补充手持实体渲染。
- 测试命令:
  - `/give @s big_dog_bark:carried_big_dog`(空物品,仅用于无效数据测试)
  - `/data get item @s carried_big_dog.components`(查看物品数据组件)
- **尚未实现**机枪附魔、机枪子弹、连射、弹药/能量系统与武器伤害(本阶段只实现抱起/放下)。

### 第五阶段:蓄能大狗炮

- 数据驱动附魔 **蓄能 (Charge)**:`big_dog_bark:charge`,最高 1 级,只能应用到抱起的大狗
  (`big_dog_bark:carried_big_dog`),不能用于武器、工具、骨头、种子、盔甲等任何其他物品。
- **如何附魔**:铁砧左侧放抱起的大狗、右侧放“蓄能 I”附魔书,取出带“蓄能 I”的抱起的大狗。
  - `/give @s minecraft:enchanted_book[enchantments={levels:{"big_dog_bark:charge":1}}]`
- **普通右键蓄能、松开发射**(只有主人、带蓄能附魔、不在冷却、非观战者才能启动;
  其他玩家拿到大狗不能蓄能/发射/放下,提示“这只大狗不属于你”):
  - **下蹲 + 右键方块**仍是放狗(第四阶段),与蓄能操作不冲突。
- 蓄能时间线(20 Tick = 1 秒,统一 Tick 常量):
  - `0 ～ 15 Tick`(0 ～ 0.75 秒):**蓄能不足**,松开发射会提示“蓄能不足”,大狗留在手中、不消耗;
  - `16 ～ 39 Tick`(0.8 ～ 1.95 秒):可以发射,威力较低(速度约 1.5,伤害约 6);
  - `40 ～ 69 Tick`(2.0 ～ 3.45 秒):**蓄能完成**,播放一次 ready 音效 + 动作栏“蓄能完成”,正常高威力(速度约 2.5,伤害约 10);
  - `70 ～ 89 Tick`(3.5 ～ 4.45 秒):**危险过载区间**,70 Tick 时警告一次(“警告:蓄能即将过载!”+ 警告粒子),仍可松开发射(速度约 3.5,伤害约 13.4);
  - `90 Tick`(4.5 秒):**自动炸膛**,不需要松手——不发射大狗、大狗仍留在手中,玩家附近产生**原版爆炸**(威力 3,不破坏地形、不点火),播放原版爆炸声与爆炸粒子,玩家受到约 7 点伤害和明显击退,进入 **3 秒冷却**,动作栏提示“蓄能过载,大狗炸膛了!”。
- 发射速度按蓄能平滑插值(16 Tick ≈ 1.5 → 89 Tick ≈ 4.0),伤害平滑变化(6 → 16),命中造成冲撞伤害 + 合理击退;
  **不破坏方块、不产生爆炸、不点燃目标**(纯撞击)。
- 发射时创建专用投射物实体 `big_dog_bark:launched_big_dog`(不是把狼实体甩出去当炮弹):
  - 内部保存**完整的 carried_big_dog 物品**(OwnerUuid / WolfData / 蓄能附魔 / 自定义名称等全部组件),
    客户端通过 DataTracker 同步,渲染复用原版狼实体模型(真实大狗外观,狗头朝向飞行方向);
  - 命中实体:造成伤害与击退,随后在命中位置**掉落原来的大狗物品**(需要跑过去捡回来,这是玩法成本);
  - 命中方块:不破坏方块,直接掉落大狗物品;超时(10 秒)就地掉落;掉落只发生一次(防重复生成大狗);
  - 掉出世界底部时安全回收:主人在线放回背包(背包满掉在脚下),主人离线在世界出生点生成物品——**独一无二的大狗不会因投射物超时/出界而静默删除**;
  - 投射物 NBT 持久化完整物品栈(与 ItemEntity 相同的原版序列化),服务器保存/重启/区块卸载不丢数据、不复制物品。
- 放下 → 再抱起保留附魔:大狗实体新增轻量永久状态 `BigDogBark.WeaponMode`(0 = 无,1 = 蓄能,随狼 NBT 持久化):
  - 放下带“蓄能 I”的大狗时写入 `WeaponMode = 1`;重新抱起时按 WeaponMode 通过真实附魔 API 重新添加“蓄能 I”;
  - 普通狼为 0;该状态本身不能让普通狼变成特殊大狗;旧存档无字段默认 0。
- Tooltip:带“蓄能 I”的大狗额外显示“按住右键蓄能,松开发射 / 蓄能过久会炸膛”。
- 自定义音效:
  - `assets/big_dog_bark/sounds/entity/dog_charge.ogg`(开始蓄能)
  - `assets/big_dog_bark/sounds/entity/dog_charge_ready.ogg`(40 Tick 蓄能完毕)
  - `assets/big_dog_bark/sounds/entity/dog_launch.ogg`(松开发射)
- **当前没有机枪**;下一阶段将加入机枪附魔、子弹与连射。

## 构建与运行

```bat
gradlew.bat clean build   :: 编译打包
gradlew.bat runClient     :: 启动开发客户端
gradlew.bat runServer     :: 启动独立服务端(首次需在 run/eula.txt 中设置 eula=true)
gradlew.bat runDatagen    :: Fabric 数据生成(预留)
```

## 测试命令 (Minecraft 1.21.1)

在创建世界时开启**创造模式**并**允许作弊**,进入游戏后按 `T` 打开聊天框输入:

### 第一阶段(大狗叫)

```mcfunction
# 获得一本带“大狗叫 I”的附魔书(建议的获取方式)
/give @s minecraft:enchanted_book[enchantments={levels:{"big_dog_bark:big_dog_bark":1}}]

# (调试用)直接获得带“大狗叫 I”的骨头
/give @s minecraft:bone[enchantments={levels:{"big_dog_bark:big_dog_bark":1}}]

# 召唤一只未驯服的成年狼
/summon minecraft:wolf ~ ~ ~

# 手持骨头时直接附魔(验证附魔只支持骨头)
/enchant @s big_dog_bark:big_dog_bark

# 查看狼的 NBT(确认 Owner 与 BigDogBark.IsBigDog)
/data get entity @e[type=minecraft:wolf,limit=1,sort=nearest]
```

使用方式:拿着“大狗叫 I”的附魔骨头右键未驯服的狼,直到成功(每次右键 1/3 概率,
失败会冒烟、消耗一根骨头,狼保持未驯服;成功会冒爱心、狼坐下并认主)。
铁砧用法:左侧放普通骨头,右侧放大狗叫附魔书,取出带附魔的骨头。

### 第二阶段(叮咚鸡)

```mcfunction
# 获得一本带“叮咚鸡 I”的附魔书(建议的获取方式)
/give @s minecraft:enchanted_book[enchantments={levels:{"big_dog_bark:ding_dong_chicken":1}}]

# (调试用)直接获得带“叮咚鸡 I”的小麦种子
/give @s minecraft:wheat_seeds[enchantments={levels:{"big_dog_bark:ding_dong_chicken":1}}]

# 召唤一只原版鸡
/summon minecraft:chicken ~ ~ ~

# 查看鸡的 NBT(确认 BigDogBark.IsDingDongChicken)
/data get entity @e[type=minecraft:chicken,limit=1,sort=nearest]

# 获得叮咚鸡肉
/give @s big_dog_bark:ding_dong_chicken_meat 64

# 获得附魔骨头(驯服特殊大狗用)
/give @s minecraft:bone[enchantments={levels:{"big_dog_bark:big_dog_bark":1}}] 64

# 召唤狼并驯服为特殊大狗后,喂食叮咚鸡肉成长(每 3 点升一阶段)
# 查看最近狼 NBT(确认 BigDogBark.IsBigDog 与 BigDogBark.GrowthPoints)
/data get entity @e[type=minecraft:wolf,limit=1,sort=nearest]

# 查看狼的缩放属性(验证阶段倍率)
/attribute @e[type=minecraft:wolf,limit=1,sort=nearest] minecraft:generic.scale get
```

使用方式:铁砧左侧放普通小麦种子、右侧放“叮咚鸡 I”附魔书,取出附魔种子;
手持附魔种子右键未转化的鸡(每次右键消耗一颗种子,创造模式不消耗),转化成功后
播放自定义音效并冒爱心;击杀叮咚鸡会额外掉落 1 个叮咚鸡肉并播放自定义死亡音效。

> 上述命令语法已在 Minecraft 1.21.1 独立服务端控制台中实际执行验证(2026-08):
> `/give` 的 1.21.1 物品组件格式(含 `big_dog_bark` 附魔 ID)解析通过;错误命令会被正常拒绝。
