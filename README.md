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
- **当前阶段尚未实现**“喂给大狗后成长”,为后续功能。

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
/give @s big_dog_bark:ding_dong_chicken_meat
```

使用方式:铁砧左侧放普通小麦种子、右侧放“叮咚鸡 I”附魔书,取出附魔种子;
手持附魔种子右键未转化的鸡(每次右键消耗一颗种子,创造模式不消耗),转化成功后
播放自定义音效并冒爱心;击杀叮咚鸡会额外掉落 1 个叮咚鸡肉并播放自定义死亡音效。

> 上述命令语法已在 Minecraft 1.21.1 独立服务端控制台中实际执行验证(2026-08):
> `/give` 的 1.21.1 物品组件格式(含 `big_dog_bark` 附魔 ID)解析通过;错误命令会被正常拒绝。
