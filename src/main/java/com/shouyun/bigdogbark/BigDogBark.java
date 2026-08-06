package com.shouyun.bigdogbark;

import com.shouyun.bigdogbark.entity.BigDogWolfTamingHandler;
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
		// 注册“附魔骨头驯服狼”的实体交互事件(仅逻辑注册,事件内部自行分流客户端/服务端)。
		BigDogWolfTamingHandler.register();

		LOGGER.info("Big Dog Bark (大狗叫) initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
