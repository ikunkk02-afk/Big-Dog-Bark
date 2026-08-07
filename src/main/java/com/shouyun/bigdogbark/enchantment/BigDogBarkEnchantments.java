package com.shouyun.bigdogbark.enchantment;

import com.shouyun.bigdogbark.BigDogBark;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

/**
 * “大狗叫”“叮咚鸡”与“蓄能”附魔的动态注册表键。
 * 1.21.1 附魔是数据驱动系统,本类只声明 RegistryKey,数据定义见
 * {@code data/big_dog_bark/enchantment/*.json}。
 */
public final class BigDogBarkEnchantments {

	public static final RegistryKey<Enchantment> BIG_DOG_BARK =
			RegistryKey.of(RegistryKeys.ENCHANTMENT, BigDogBark.id("big_dog_bark"));

	public static final RegistryKey<Enchantment> DING_DONG_CHICKEN =
			RegistryKey.of(RegistryKeys.ENCHANTMENT, BigDogBark.id("ding_dong_chicken"));

	/** 蓄能:只能应用到抱起的大狗,用于第五阶段“蓄能大狗炮”。 */
	public static final RegistryKey<Enchantment> CHARGE =
			RegistryKey.of(RegistryKeys.ENCHANTMENT, BigDogBark.id("charge"));

	private BigDogBarkEnchantments() {
	}
}
