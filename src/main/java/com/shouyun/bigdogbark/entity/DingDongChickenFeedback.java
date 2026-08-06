package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

/**
 * 叮咚鸡转化/死亡反馈集中入口。
 * <ul>
 *   <li>转化反馈:爱心粒子 + 自定义转化音效,由服务端 {@code playSound(null, ...)}
 *       向附近玩家广播,具有原版距离衰减,远距离玩家不会全图听见。</li>
 *   <li>死亡音效:由 {@code ChickenEntityMixin} 替换 {@code getDeathSound} 完成,
 *       原版死亡流程只播放一次,不在死亡事件中额外播放,避免声音重复。</li>
 * </ul>
 */
public final class DingDongChickenFeedback {

	private DingDongChickenFeedback() {
	}

	/** 转化成功反馈:少量爱心粒子 + 叮咚鸡转化音效(服务端广播)。 */
	public static void playConversionFeedback(ServerWorld world, ChickenEntity chicken) {
		world.sendEntityStatus(chicken, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
		world.playSound(null, chicken.getBlockPos(), BigDogBarkSoundEvents.DING_DONG_CHICKEN_CONVERT,
				SoundCategory.NEUTRAL, 1.0F, 1.0F);
	}
}
