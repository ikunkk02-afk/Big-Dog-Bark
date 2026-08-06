package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.enchantment.BigDogBarkEnchantments;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
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
 * “附魔骨头驯服狼”的交互处理。
 * 使用 Fabric 的 {@link UseEntityCallback} 事件在服务端接管“未驯服的狼 +
 * 带大狗叫附魔的骨头”这一种交互,其余情况(普通骨头、已驯服、愤怒、其他实体)
 * 一律返回 PASS 交还原版流程,不对原版交互方法做整段覆盖。
 *
 * <p>逻辑与 1.21.1 原版 WolfEntity.interactMob / tryTame 对齐(1/3 概率、
 * decrementUnlessCreative 消耗、setOwner / navigation.stop / setTarget(null) /
 * setSitting(true) / 实体状态粒子)。客户端分支只负责:取消本地原版处理、
 * 触发手部动画并向服务端发送交互数据包,实际消耗与驯服结果以服务端为准。
 */
public final class BigDogWolfTamingHandler {

	/** 原版实体交互距离上限 6 格(平方距离 36)。 */
	private static final double MAX_INTERACTION_DISTANCE_SQUARED = 36.0D;

	private BigDogWolfTamingHandler() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(BigDogWolfTamingHandler::onInteractEntity);
	}

	private static ActionResult onInteractEntity(PlayerEntity player, World world, Hand hand, Entity entity,
			@Nullable EntityHitResult hitResult) {
		// 事件挂载点早于原版的观战者检查,这里自行过滤;已移除的实体不再处理
		if (!(entity instanceof WolfEntity wolf) || player.isSpectator() || entity.isRemoved()) {
			return ActionResult.PASS;
		}
		// 已驯服的狼(喂食/坐下切换/护甲等)与愤怒的狼(原版不可驯服)交还原版逻辑,
		// 附魔骨头不会在它们身上被消耗
		if (wolf.isTamed() || wolf.hasAngerTime()) {
			return ActionResult.PASS;
		}
		// 超出原版交互距离时不触发
		if (player.squaredDistanceTo(wolf) > MAX_INTERACTION_DISTANCE_SQUARED) {
			return ActionResult.PASS;
		}
		// 主手或实际交互手必须是普通骨头,且带等级 ≥ 1 的大狗叫附魔(动态注册表查询,不解析 Lore)
		ItemStack stack = player.getStackInHand(hand);
		if (!stack.isOf(Items.BONE) || getBigDogBarkLevel(wolf, stack) < 1) {
			return ActionResult.PASS; // 普通骨头保留原版驯服行为
		}

		if (world.isClient) {
			// 客户端:SUCCESS = 取消本地原版处理 + 触发手部动画 + 发送交互数据包,
			// 不执行任何消耗与随机,全部以服务端为准
			return ActionResult.success(true);
		}
		return tryTameWithEnchantedBone((ServerWorld) world, player, stack, wolf);
	}

	/**
	 * 服务端驯服尝试,对齐原版 WolfEntity.tryTame:概率 1/3、消耗方式、实体行为
	 * 全部与原版一致,仅额外标记“特殊大狗”并统一走反馈入口。
	 */
	private static ActionResult tryTameWithEnchantedBone(ServerWorld world, PlayerEntity player, ItemStack bone,
			WolfEntity wolf) {
		// 每次尝试消耗一根附魔骨头,创造模式不消耗
		bone.decrementUnlessCreative(1, player);
		BigDogWolfFeedback.playFeedFeedback(world, wolf);

		// 原版驯服概率 1/3(WolfEntity.tryTame 使用 random.nextInt(3) == 0)
		if (wolf.getRandom().nextInt(3) == 0) {
			// 驯服成功:与原版一致地设置主人、停止寻路、清除攻击目标、坐下
			wolf.setOwner(player);
			wolf.getNavigation().stop();
			wolf.setTarget(null);
			wolf.setSitting(true);
			// 永久标记为特殊大狗(随实体 NBT 持久化)
			BigDogWolfUtil.markBigDog(wolf);
			BigDogWolfFeedback.playTameSuccessFeedback(world, wolf);
		} else {
			// 驯服失败:保留原版概率,狼保持未驯服
			BigDogWolfFeedback.playTameFailureFeedback(world, wolf);
		}
		return ActionResult.SUCCESS;
	}

	/**
	 * 通过 1.21.1 动态注册表查询“大狗叫”附魔在物品上的等级。
	 * 客户端与服务端均可调用(实体注册表管理器中都包含附魔注册表)。
	 */
	private static int getBigDogBarkLevel(Entity entity, ItemStack stack) {
		Optional<RegistryEntry.Reference<Enchantment>> entry = entity.getRegistryManager()
				.get(RegistryKeys.ENCHANTMENT)
				.getEntry(BigDogBarkEnchantments.BIG_DOG_BARK);
		return entry.map(e -> EnchantmentHelper.getLevel(e, stack)).orElse(0);
	}
}
