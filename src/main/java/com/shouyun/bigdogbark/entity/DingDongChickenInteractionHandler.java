package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.enchantment.BigDogBarkEnchantments;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * “附魔种子转化叮咚鸡”的交互处理。
 * 使用 Fabric 的 {@link UseEntityCallback} 事件在服务端接管“未转化的原版鸡 +
 * 带叮咚鸡附魔的小麦种子”这一种交互,其余情况(普通种子、已转化的鸡、其他实体)
 * 一律返回 PASS 交还原版流程。
 *
 * <p>客户端分支只负责:取消本地原版处理、触发手部动画并向服务端发送交互数据包,
 * 实际消耗、标记与转化结果全部以服务端为准,不会客户端/服务端各消耗一次。
 */
public final class DingDongChickenInteractionHandler {

	/** 原版实体交互距离上限 6 格(平方距离 36)。 */
	private static final double MAX_INTERACTION_DISTANCE_SQUARED = 36.0D;

	private DingDongChickenInteractionHandler() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(DingDongChickenInteractionHandler::onInteractEntity);
	}

	private static ActionResult onInteractEntity(PlayerEntity player, World world, Hand hand, Entity entity,
			@Nullable EntityHitResult hitResult) {
		// 事件挂载点早于原版的观战者检查,这里自行过滤;已移除/死亡的实体不再处理
		if (!(entity instanceof ChickenEntity chicken) || player.isSpectator() || entity.isRemoved()) {
			return ActionResult.PASS;
		}
		// 超出原版交互距离时不触发
		if (player.squaredDistanceTo(chicken) > MAX_INTERACTION_DISTANCE_SQUARED) {
			return ActionResult.PASS;
		}
		// 实际交互手(主手或副手)必须是小麦种子,且带等级 ≥ 1 的叮咚鸡附魔
		// (动态注册表查询,不解析 Lore/物品名/附魔光效)
		ItemStack stack = player.getStackInHand(hand);
		if (!stack.isOf(Items.WHEAT_SEEDS) || getDingDongChickenLevel(chicken, stack) < 1) {
			return ActionResult.PASS; // 普通种子保留原版繁殖与幼年成长行为
		}
		// 已经是叮咚鸡的鸡不能再次转化,返回 PASS 交还原版种子交互逻辑
		if (DingDongChickenUtil.isDingDongChicken(chicken)) {
			return ActionResult.PASS;
		}

		if (world.isClient) {
			// 客户端:SUCCESS = 取消本地原版处理 + 触发手部动画 + 发送交互数据包,
			// 不执行任何消耗与状态修改,全部以服务端为准
			return ActionResult.success(true);
		}
		return convertChicken((ServerWorld) world, player, stack, chicken);
	}

	/**
	 * 服务端转化:消耗一颗种子(创造模式不消耗)、永久标记叮咚鸡、
	 * 短暂停止当前寻路(不永久破坏 AI),并播放转化反馈。
	 */
	private static ActionResult convertChicken(ServerWorld world, PlayerEntity player, ItemStack seeds,
			ChickenEntity chicken) {
		seeds.decrementUnlessCreative(1, player);
		DingDongChickenUtil.markDingDongChicken(chicken);
		// 让鸡短暂停止当前寻路,鸡仍可移动、下蛋、繁殖、受伤和死亡
		chicken.getNavigation().stop();
		DingDongChickenFeedback.playConversionFeedback(world, chicken);
		return ActionResult.SUCCESS;
	}

	/**
	 * 通过 1.21.1 动态注册表查询“叮咚鸡”附魔在物品上的等级。
	 * 客户端与服务端均可调用(实体注册表管理器中都包含附魔注册表)。
	 */
	private static int getDingDongChickenLevel(Entity entity, ItemStack stack) {
		Optional<RegistryEntry.Reference<Enchantment>> entry = entity.getRegistryManager()
				.get(RegistryKeys.ENCHANTMENT)
				.getEntry(BigDogBarkEnchantments.DING_DONG_CHICKEN);
		return entry.map(e -> EnchantmentHelper.getLevel(e, stack)).orElse(0);
	}
}
