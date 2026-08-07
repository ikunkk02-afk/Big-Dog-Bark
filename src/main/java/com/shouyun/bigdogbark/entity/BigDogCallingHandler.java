package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.mixin.JukeboxBlockEntityMixin;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * 大狗召唤唱片吸引逻辑:当唱片机播放 big_dog_bark:big_dog_calling 时,
 * 每 40 tick(2 秒)吸引附近 32 格内的所有狼与鸡向唱片机靠拢。
 */
public final class BigDogCallingHandler {

	private static final double ATTRACT_RADIUS = 32.0D;
	private static final double ATTRACT_SPEED = 1.2D;

	private BigDogCallingHandler() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(BigDogCallingHandler::onWorldTick);
	}

	private static void onWorldTick(ServerWorld world) {
		if (JukeboxBlockEntityMixin.ACTIVE_BIG_DOG_CALLING_JUKEBOXES.isEmpty()) {
			return;
		}
		// 每 40 tick(2 秒) 执行一次吸引,降低开销
		if (world.getTime() % 40 != 0) {
			return;
		}

		List<BlockPos> toRemove = new ArrayList<>();
		for (BlockPos pos : JukeboxBlockEntityMixin.ACTIVE_BIG_DOG_CALLING_JUKEBOXES) {
			if (!world.isChunkLoaded(pos)) {
				continue;
			}
			// 检查唱片机是否仍在该位置(可能被破坏)
			if (!(world.getBlockEntity(pos) instanceof net.minecraft.block.entity.JukeboxBlockEntity jukebox)) {
				toRemove.add(pos);
				continue;
			}
			if (!jukebox.getManager().isPlaying()) {
				toRemove.add(pos);
				continue;
			}

			Box area = new Box(pos).expand(ATTRACT_RADIUS);
			List<WolfEntity> wolves = world.getEntitiesByClass(WolfEntity.class, area, w -> true);
			List<ChickenEntity> chickens = world.getEntitiesByClass(ChickenEntity.class, area, c -> true);

			for (WolfEntity wolf : wolves) {
				attractEntity(wolf, pos);
			}
			for (ChickenEntity chicken : chickens) {
				attractEntity(chicken, pos);
			}
		}
		for (BlockPos pos : toRemove) {
			JukeboxBlockEntityMixin.ACTIVE_BIG_DOG_CALLING_JUKEBOXES.remove(pos);
		}
	}

	private static void attractEntity(Entity entity, BlockPos target) {
		if (!(entity instanceof net.minecraft.entity.mob.MobEntity mob)) {
			return;
		}
		if (mob.getNavigation().isIdle()) {
			Path path = mob.getNavigation().findPathTo(target, 0);
			if (path != null) {
				mob.getNavigation().startMovingAlong(path, ATTRACT_SPEED);
			}
		}
	}
}
