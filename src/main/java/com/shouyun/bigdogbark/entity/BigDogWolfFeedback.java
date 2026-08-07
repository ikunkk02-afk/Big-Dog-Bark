package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.WorldEvents;

/**
 * 驯服流程的反馈集中入口(粒子/事件)。
 * 当前阶段全部使用原版粒子与事件;后续接入自定义音效时,只需在对应方法内
 * 追加播放 {@code dog_bone_feed.ogg} / {@code dog_tame_success.ogg},
 * 并补充 sounds.json 条目,不必改动驯服逻辑。
 */
public final class BigDogWolfFeedback {

	private BigDogWolfFeedback() {
	}

	/** 投喂尝试反馈:原版骨粉粒子事件(后续追加 dog_bone_feed.ogg)。 */
	public static void playFeedFeedback(ServerWorld world, WolfEntity wolf) {
		world.syncGlobalEvent(WorldEvents.BONE_MEAL_USED, wolf.getBlockPos(), 0);
	}

	/** 驯服成功反馈:原版爱心粒子 + 自定义驯服成功音效。 */
	public static void playTameSuccessFeedback(ServerWorld world, WolfEntity wolf) {
		world.sendEntityStatus(wolf, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
		world.playSoundFromEntity(null, wolf, BigDogBarkSoundEvents.DOG_TAME_SUCCESS,
				SoundCategory.NEUTRAL, 1.0F, 1.0F);
	}

	/** 驯服失败反馈:原版失败粒子(后续追加失败音效)。 */
	public static void playTameFailureFeedback(ServerWorld world, WolfEntity wolf) {
		world.sendEntityStatus(wolf, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES);
	}
}
