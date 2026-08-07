package com.shouyun.bigdogbark.enchantment;

import com.shouyun.bigdogbark.BigDogBark;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

/**
 * 模组附魔的统一检测/应用工具。
 *
 * <p>1.21.1 附魔是数据驱动系统,等级检测必须通过动态注册表拿到附魔的
 * {@code RegistryEntry},禁止解析物品名/Lore/附魔光效。所有涉及 CHARGE 附魔的
 * 逻辑(蓄能检测、发射校验、放下→抱起恢复)一律走本类,后续机枪附魔直接在此扩展。
 */
public final class BigDogEnchantmentUtil {

	private BigDogEnchantmentUtil() {
	}

	/** 从动态注册表获取“蓄能”附魔条目(数据包缺失时返回空,调用方按 0 级处理)。 */
	private static RegistryWrapper.Impl<Enchantment> chargeRegistry(RegistryWrapper.WrapperLookup registryManager) {
		return registryManager.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
	}

	/** 蓄能附魔等级(0 = 没有)。 */
	public static int getChargeLevel(ItemStack stack, RegistryWrapper.WrapperLookup registryManager) {
		return chargeRegistry(registryManager)
				.getOptional(BigDogBarkEnchantments.CHARGE)
				.map(entry -> EnchantmentHelper.getLevel(entry, stack))
				.orElse(0);
	}

	/** 是否拥有“蓄能 I”(需要动态注册表,服务端权威路径使用)。 */
	public static boolean hasCharge(ItemStack stack, RegistryWrapper.WrapperLookup registryManager) {
		return getChargeLevel(stack, registryManager) >= 1;
	}

	/** 是否拥有“蓄能 I”(无需注册表的组件直查,仅用于 Tooltip 等展示场景)。 */
	public static boolean hasChargeInComponents(ItemStack stack) {
		return stack.getEnchantments().getEnchantments().stream()
				.anyMatch(entry -> entry.matchesKey(BigDogBarkEnchantments.CHARGE));
	}

	/** 给物品应用 1 级“蓄能”附魔(通过真实附魔组件 API,不用 Lore 伪造)。 */
	public static void applyCharge(ItemStack stack, RegistryWrapper.WrapperLookup registryManager) {
		chargeRegistry(registryManager)
				.getOptional(BigDogBarkEnchantments.CHARGE)
				.ifPresent(entry -> stack.addEnchantment(entry, 1));
	}
}
