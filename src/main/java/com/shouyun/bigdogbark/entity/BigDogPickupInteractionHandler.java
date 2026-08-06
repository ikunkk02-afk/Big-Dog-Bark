package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.BigDogBark;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import com.shouyun.bigdogbark.item.CarriedBigDogData;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * “主人下蹲空手右键完全成长大狗,抱起为物品”的交互处理。
 * 使用 Fabric 的 {@link UseEntityCallback},服务端权威:
 * 客户端只取消本地原版处理并发送交互包,不创建物品、不删除实体、不序列化权威数据。
 *
 * <p>注册顺序在成长交互之后:手持叮咚鸡肉时由成长处理器先行消费,
 * 空手 + 下蹲 + 完全成长才进入抱起,一次交互不可能同时喂食并抱起。
 */
public final class BigDogPickupInteractionHandler {

	/** 原版实体交互距离上限 6 格(平方距离 36)。 */
	private static final double MAX_INTERACTION_DISTANCE_SQUARED = 36.0D;

	private BigDogPickupInteractionHandler() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(BigDogPickupInteractionHandler::onInteractEntity);
	}

	private static ActionResult onInteractEntity(PlayerEntity player, World world, Hand hand, Entity entity,
			@Nullable EntityHitResult hitResult) {
		if (!(entity instanceof WolfEntity wolf) || player.isSpectator() || entity.isRemoved() || wolf.isDead()) {
			return ActionResult.PASS;
		}
		// 只处理:已驯服 + 特殊大狗(普通狼、野生狼保持原版交互)
		if (!wolf.isTamed() || !BigDogWolfUtil.isBigDog(wolf)) {
			return ActionResult.PASS;
		}
		// 非主人的玩家不能抱起其他玩家的大狗:交还原版,不消耗不删除
		if (!wolf.isOwner(player)) {
			return ActionResult.PASS;
		}
		if (player.squaredDistanceTo(wolf) > MAX_INTERACTION_DISTANCE_SQUARED) {
			return ActionResult.PASS;
		}
		// 未完全成长(含幼年):下蹲时给出提示,不下蹲保留原版坐下/站起交互
		if (!BigDogWolfUtil.isFullyGrown(wolf)) {
			if (!player.isSneaking()) {
				return ActionResult.PASS;
			}
			if (world.isClient) {
				return ActionResult.success(true);
			}
			sendActionBar(player, "action.big_dog_bark.dog_not_fully_grown");
			return ActionResult.SUCCESS;
		}
		// 完全成长但不下蹲:保留原版坐下/站起交互
		if (!player.isSneaking()) {
			return ActionResult.PASS;
		}
		// 拴绳 / 乘客 / 骑乘状态不可抱起:不删除实体、不生成物品
		if (wolf.isLeashed() || wolf.hasPassengers() || wolf.hasVehicle()) {
			if (world.isClient) {
				return ActionResult.success(true);
			}
			sendActionBar(player, wolf.isLeashed()
					? "action.big_dog_bark.dog_is_leashed"
					: "action.big_dog_bark.dog_cannot_be_picked_up");
			return ActionResult.SUCCESS;
		}
		// 实际交互手(主手/副手)必须为空;手持其他物品保留原版交互(如喂食、染色)
		ItemStack stack = player.getStackInHand(hand);
		if (!stack.isEmpty()) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			// 客户端:取消本地原版处理 + 触发手部动画 + 发送交互数据包,不执行任何权威操作
			return ActionResult.success(true);
		}
		return pickupBigDog((ServerWorld) world, (ServerPlayerEntity) player, hand, wolf);
	}

	/**
	 * 服务端抱起流程:序列化并验证物品 → 放入实际交互手 → 成功后才移除实体(不触发死亡)。
	 * 失败时狼保留、不生成空物品、不刷屏日志。
	 */
	private static ActionResult pickupBigDog(ServerWorld world, ServerPlayerEntity player, Hand hand, WolfEntity wolf) {
		ItemStack stack = CarriedBigDogData.createFromWolf(wolf, player);
		if (!CarriedBigDogData.hasValidData(stack)) {
			BigDogBark.LOGGER.error("Failed to serialize carried big dog data for wolf at {}", wolf.getBlockPos());
			sendActionBar(player, "action.big_dog_bark.carried_dog_invalid_data");
			return ActionResult.SUCCESS;
		}
		player.setStackInHand(hand, stack);
		if (!player.getStackInHand(hand).isOf(BigDogBarkItems.CARRIED_BIG_DOG)) {
			BigDogBark.LOGGER.error("Failed to place carried big dog item into hand of {}", player.getName().getString());
			sendActionBar(player, "action.big_dog_bark.carried_dog_invalid_data");
			return ActionResult.SUCCESS;
		}
		// 清理临时战斗/寻路状态后以无死亡方式移除实体(discard 不触发死亡掉落与死亡音效)
		wolf.getNavigation().stop();
		wolf.setTarget(null);
		wolf.setAngerTime(0);
		wolf.discard();
		// 反馈:原版拾取声 + 少量粒子 + 动作栏提示(服务端广播,距离衰减)
		world.playSound(null, wolf.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
		world.spawnParticles(ParticleTypes.CLOUD,
				wolf.getX(), wolf.getY() + wolf.getHeight() * 0.5D, wolf.getZ(),
				8, 0.4D, 0.4D, 0.4D, 0.05D);
		sendActionBar(player, "action.big_dog_bark.dog_picked_up");
		return ActionResult.SUCCESS;
	}

	private static void sendActionBar(PlayerEntity player, String translationKey) {
		player.sendMessage(Text.translatable(translationKey), true);
	}
}
