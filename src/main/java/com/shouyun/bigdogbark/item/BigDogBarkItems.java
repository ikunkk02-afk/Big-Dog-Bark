package com.shouyun.bigdogbark.item;

import com.shouyun.bigdogbark.BigDogBark;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 模组物品注册入口。
 * 当前阶段仅注册“叮咚鸡肉”,作为后续“大狗成长系统”的专用材料,
 * 不替代也不覆盖原版 minecraft:chicken。
 */
public final class BigDogBarkItems {

	/**
	 * 叮咚鸡肉:普通 Item 即可,最大堆叠 64,当前阶段不可食用。
	 * 使用匿名子类仅用于追加灰色提示行(提示文本走语言键,不硬编码中文)。
	 */
	public static final Item DING_DONG_CHICKEN_MEAT = new Item(new Item.Settings()) {
		@Override
		public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip,
				net.minecraft.item.tooltip.TooltipType type) {
			tooltip.add(Text.translatable("item.big_dog_bark.ding_dong_chicken_meat.tooltip")
					.formatted(Formatting.GRAY));
		}
	};

	private BigDogBarkItems() {
	}

	public static void register() {
		Registry.register(Registries.ITEM, BigDogBark.id("ding_dong_chicken_meat"), DING_DONG_CHICKEN_MEAT);
		// 加入原版“原材料”创造模式物品栏
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
				.register(entries -> entries.add(DING_DONG_CHICKEN_MEAT));
	}
}
