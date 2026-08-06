package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.item.BigDogBarkItems;
import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
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
 * “叮咚鸡肉喂食特殊大狗成长”的交互处理。
 * 使用 Fabric 的 {@link UseEntityCallback},服务端权威:
 * 成长进度、物品消耗、Scale 属性、声音与粒子全部由服务端执行,
 * 客户端分支只取消本地原版处理并发送交互数据包,不会重复增加进度或消耗。
 *
 * <p>成长条件(全部满足):原版 WolfEntity、已驯服、特殊大狗(IsBigDog)、
 * 操作玩家是主人、已成年、手持叮咚鸡肉、未达到最大进度。
 */
public final class BigDogGrowthInteractionHandler {

	/** 原版实体交互距离上限 6 格(平方距离 36)。 */
	private static final double MAX_INTERACTION_DISTANCE_SQUARED = 36.0D;

	private BigDogGrowthInteractionHandler() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(BigDogGrowthInteractionHandler::onInteractEntity);
	}

	private static ActionResult onInteractEntity(PlayerEntity player, World world, Hand hand, Entity entity,
			@Nullable EntityHitResult hitResult) {
		if (!(entity instanceof WolfEntity wolf) || player.isSpectator() || entity.isRemoved()) {
			return ActionResult.PASS;
		}
		if (player.squaredDistanceTo(wolf) > MAX_INTERACTION_DISTANCE_SQUARED) {
			return ActionResult.PASS;
		}
		// 不是特殊大狗(普通狼、野生狼):交还原版交互
		if (!BigDogWolfUtil.isBigDog(wolf)) {
			return ActionResult.PASS;
		}
		// 未驯服的狼(理论上大狗必已驯服,防御性检查):交还原版
		if (!wolf.isTamed()) {
			return ActionResult.PASS;
		}
		// 非主人的玩家不能培养或偷养大狗:不消耗、不增加进度,交还原版(原版非主人无法交互已驯服狼)
		if (!wolf.isOwner(player)) {
			return ActionResult.PASS;
		}
		// 幼年特殊大狗不能成长:不消耗,给主人提示,阻止无意义重复交互
		if (wolf.isBaby()) {
			if (world.isClient) {
				return ActionResult.success(true);
			}
			sendActionBar(player, "action.big_dog_bark.big_dog_too_young");
			return ActionResult.SUCCESS;
		}
		// 实际交互手(主手/副手)必须是叮咚鸡肉
		ItemStack stack = player.getStackInHand(hand);
		if (!stack.isOf(BigDogBarkItems.DING_DONG_CHICKEN_MEAT)) {
			return ActionResult.PASS;
		}
		// 已达到最大进度:不消耗、不播放成长音效,给主人提示
		if (BigDogWolfUtil.isFullyGrown(wolf)) {
			if (world.isClient) {
				return ActionResult.success(true);
			}
			sendActionBar(player, "action.big_dog_bark.big_dog_max_growth");
			return ActionResult.SUCCESS;
		}
		if (world.isClient) {
			// 客户端:取消本地原版处理 + 触发手部动画 + 发送交互数据包,不执行消耗与进度修改
			return ActionResult.success(true);
		}
		return feedBigDog((ServerWorld) world, player, stack, wolf);
	}

	/**
	 * 服务端喂食:消耗 1 个叮咚鸡肉(创造模式不消耗)、进度 +1、按需更新缩放,
	 * 播放进食音效与粒子;跨越阶段时额外播放成长音效、更明显的粒子与阶段提示。
	 */
	private static ActionResult feedBigDog(ServerWorld world, PlayerEntity player, ItemStack meat, WolfEntity wolf) {
		int beforeStage = BigDogWolfUtil.getGrowthStage(wolf);
		meat.decrementUnlessCreative(1, player);
		int after = BigDogWolfUtil.addGrowthPoint(wolf);
		int afterStage = BigDogGrowth.stageFor(after);
		BigDogWolfUtil.applyGrowthScale(wolf);

		// 每次成功喂食:原版进食音效,服务端广播、距离衰减
		world.playSound(null, wolf.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL,
				0.9F, 1.0F);

		if (afterStage > beforeStage) {
			// 阶段提升:明显粒子 + 自定义成长音效 + 阶段提示(只发一条,不覆盖)
			world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
					wolf.getX(), wolf.getY() + wolf.getHeight() * 0.5D, wolf.getZ(),
					12, 0.5D, 0.5D, 0.5D, 0.1D);
			world.playSound(null, wolf.getBlockPos(), BigDogBarkSoundEvents.DOG_GROWTH_STAGE,
					SoundCategory.NEUTRAL, 1.0F, 1.0F);
			sendActionBar(player, "action.big_dog_bark.growth_stage_" + afterStage);
		} else {
			// 普通喂食:少量爱心粒子 + 成长进度
			world.sendEntityStatus(wolf, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
			sendActionBar(player, Text.translatable("action.big_dog_bark.growth_progress",
					after, BigDogGrowth.MAX_GROWTH_POINTS));
		}
		return ActionResult.SUCCESS;
	}

	private static void sendActionBar(PlayerEntity player, String translationKey) {
		player.sendMessage(Text.translatable(translationKey), true);
	}

	private static void sendActionBar(PlayerEntity player, Text message) {
		player.sendMessage(message, true);
	}
}
