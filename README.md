# Big Dog Bark (大狗叫)

一个 Minecraft 1.21.1 (Fabric) 娱乐模组:使用附魔骨头驯服“特殊大狗”,为后续的成长与武器系统打下基础。

- 中文名:大狗叫
- 英文名:Big Dog Bark
- Mod ID:`big_dog_bark`
- Minecraft:1.21.1 / Fabric Loader 0.19.3 / Fabric API 0.116.15+1.21.1 / Java 21 (Yarn 映射)
- License:MIT

## 功能

- 数据驱动附魔 **大狗叫 (Big Dog Bark)**:`big_dog_bark:big_dog_bark`,最高 1 级,只能应用到原版骨头 (`minecraft:bone`)。
- 拿着带“大狗叫 I”的骨头右键未驯服的原版狼,使用**原版驯服概率 (1/3)** 进行驯服。
- 每次尝试消耗一根附魔骨头(创造模式不消耗);驯服成功后狼会被永久标记为**特殊大狗** (NBT 键 `BigDogBark.IsBigDog`,随世界存档持久化,跨维度、重进世界、重启服务端均保留)。
- 普通骨头、已驯服的狼、幼年狼、其他生物全部保持原版行为,不受影响。

## 构建与运行

```bat
gradlew.bat clean build   :: 编译打包
gradlew.bat runClient     :: 启动开发客户端
gradlew.bat runServer     :: 启动独立服务端(首次需在 run/eula.txt 中设置 eula=true)
gradlew.bat runDatagen    :: Fabric 数据生成(预留)
```

## 测试命令 (Minecraft 1.21.1)

在创建世界时开启**创造模式**并**允许作弊**,进入游戏后按 `T` 打开聊天框输入:

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

> 上述命令语法已在 Minecraft 1.21.1 独立服务端控制台中实际执行验证(2026-08):
> `/give` 的 1.21.1 物品组件格式(含 `big_dog_bark` 附魔 ID)解析通过;错误命令会被正常拒绝。
