package com.shouyun.bigdogbark;

import com.shouyun.bigdogbark.entity.BigDogBarkEntityTypes;
import com.shouyun.bigdogbark.entity.BigDogCallingHandler;
import com.shouyun.bigdogbark.entity.BigDogPackDefenseHandler;
import com.shouyun.bigdogbark.entity.BigDogPickupInteractionHandler;
import com.shouyun.bigdogbark.entity.BigDogWolfTamingHandler;
import com.shouyun.bigdogbark.entity.DingDongChickenDeathHandler;
import com.shouyun.bigdogbark.entity.DingDongChickenInteractionHandler;
import com.shouyun.bigdogbark.entity.BigDogGrowthInteractionHandler;
import com.shouyun.bigdogbark.entity.BigDogPackDefenseHandler;
import com.shouyun.bigdogbark.entity.BigDogPickupInteractionHandler;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import com.shouyun.bigdogbark.network.BigDogBarkNetworking;
import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigDogBark implements ModInitializer {
	public static final String MOD_ID = "big_dog_bark";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 机枪满蓄能/能量耗尽后的“真实松开右键”握手。
		BigDogBarkNetworking.register();

		// 第一阶段:附魔骨头驯服狼(保持原有注册不变)
		BigDogWolfTamingHandler.register();

		// 第二阶段:叮咚鸡附魔、附魔种子转化鸡、叮咚鸡死亡掉落特殊鸡肉
		BigDogBarkSoundEvents.register();
		BigDogBarkItems.register();
		DingDongChickenInteractionHandler.register();
		DingDongChickenDeathHandler.register();

		// 第三阶段:叮咚鸡肉喂食特殊大狗成长(物品先注册,再注册交互处理器)
		BigDogGrowthInteractionHandler.register();

		// 第四阶段:抱起与放下完全成长大狗(注册在成长交互之后,手持鸡肉时优先喂食)
		BigDogPickupInteractionHandler.register();

		// 第五阶段:发射的大狗投射物实体类型
		BigDogBarkEntityTypes.register();

		// 第六阶段:被驯服大狗自身/主人受伤时召唤护卫狼群
		BigDogPackDefenseHandler.register();

		// 第七阶段:大狗召唤唱片播放时吸引附近狼和鸡
		BigDogCallingHandler.register();

		LOGGER.info("Big Dog Bark (大狗叫) initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
