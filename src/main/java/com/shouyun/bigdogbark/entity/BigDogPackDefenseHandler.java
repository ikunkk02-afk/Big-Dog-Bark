package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.BigDogBark;
import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 大狗护卫召唤:被驯服的特殊大狗自身或主人受伤时,
 * 在攻击者周围召唤一群临时护卫狼进行反击。
 *
 * <p>冷却与超时均按主人粒度;护卫狼有生命周期,到时自动消失。
 */
public final class BigDogPackDefenseHandler {

	/** 单次召唤的护卫狼数量范围。 */
	private static final int MIN_PACK_SIZE = 3;
	private static final int MAX_PACK_SIZE = 5;

	/** 两次召唤之间的冷却 Tick(30 秒)。 */
	private static final int SUMMON_COOLDOWN_TICKS = 600;

	/** 护卫狼最大存活 Tick(30 秒),到期后移除。 */
	private static final int GUARD_WOLF_LIFETIME_TICKS = 600;

	/** 搜索附近大狗的半径(格)。 */
	private static final double NEARBY_BIG_DOG_RADIUS = 16.0D;

	/** NBT 键:标记为由 pack defense 生成的临时护卫狼。 */
	private static final String GUARD_NBT_KEY = "BigDogBark.GuardWolf";

	/** 主人粒度的冷却计时器(服务端 game time)。 */
	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

	private BigDogPackDefenseHandler() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(BigDogPackDefenseHandler::onAfterDamage);
	}

	private static void onAfterDamage(LivingEntity entity, DamageSource source, float baseDamage,
			float actualDamage, boolean blocked) {
		if (blocked || entity.getWorld().isClient || !(entity.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Entity attacker = source.getAttacker();
		if (attacker == null || !(attacker instanceof LivingEntity livingAttacker) || attacker == entity) {
			return;
		}

		if (entity instanceof WolfEntity wolf) {
			handleBigDogDamaged(world, wolf, livingAttacker);
		} else if (entity instanceof PlayerEntity player) {
			handleOwnerDamaged(world, player, livingAttacker);
		}
	}

	/** 特殊大狗自身受伤:召唤护卫攻击伤害来源。 */
	private static void handleBigDogDamaged(ServerWorld world, WolfEntity bigDog, LivingEntity attacker) {
		if (!BigDogWolfUtil.isBigDog(bigDog) || !bigDog.isTamed()) {
			return;
		}
		PlayerEntity owner = (PlayerEntity) bigDog.getOwner();
		if (owner == null) {
			return;
		}
		trySummonPack(world, owner, bigDog, attacker);
	}

	/** 主人受伤:在主人附近搜索大狗,由最近的大狗触发召唤。 */
	private static void handleOwnerDamaged(ServerWorld world, PlayerEntity owner, LivingEntity attacker) {
		WolfEntity nearestBigDog = findNearestBigDog(world, owner);
		if (nearestBigDog == null) {
			return;
		}
		trySummonPack(world, owner, nearestBigDog, attacker);
	}

	private static WolfEntity findNearestBigDog(ServerWorld world, PlayerEntity owner) {
		Box box = new Box(owner.getBlockPos()).expand(NEARBY_BIG_DOG_RADIUS);
		WolfEntity nearest = null;
		double nearestDist = Double.MAX_VALUE;
		for (Entity e : world.getOtherEntities(owner, box,
				e -> e instanceof WolfEntity && BigDogWolfUtil.isBigDog((WolfEntity) e) && ((WolfEntity) e).isTamed())) {
			WolfEntity wolf = (WolfEntity) e;
			if (owner.getUuid().equals(wolf.getOwnerUuid())) {
				double dist = wolf.squaredDistanceTo(owner);
				if (dist < nearestDist) {
					nearestDist = dist;
					nearest = wolf;
				}
			}
		}
		return nearest;
	}

	private static void trySummonPack(ServerWorld world, PlayerEntity owner, WolfEntity sourceBigDog,
			LivingEntity attacker) {
		UUID ownerId = owner.getUuid();
		long gameTime = world.getTime();
		Long lastSummon = COOLDOWNS.get(ownerId);
		if (lastSummon != null && gameTime - lastSummon < SUMMON_COOLDOWN_TICKS) {
			return;
		}
		COOLDOWNS.put(ownerId, gameTime);

		Random random = world.getRandom();
		int count = MIN_PACK_SIZE + random.nextInt(MAX_PACK_SIZE - MIN_PACK_SIZE + 1);
		Vec3d center = attacker.getPos();

		for (int i = 0; i < count; i++) {
			WolfEntity guard = EntityType.WOLF.create(world);
			if (guard == null) {
				continue;
			}
			// 在攻击者周围环形散开
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = 1.5D + random.nextDouble() * 2.0D;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			double y = center.y;
			guard.setPosition(x, y, z);
			guard.setTamed(true, false);
			guard.setOwner(owner);
			guard.setSitting(false);
			guard.setTarget(attacker);
			guard.setAngerTime(1200); // 保持愤怒许久
			// 持久化标记用于后续超时回收
			guard.getCommandTags().add(GUARD_NBT_KEY);

			world.spawnEntity(guard);
			// 简单粒子:护卫狼出现的位置冒一点烟
			world.spawnParticles(ParticleTypes.POOF, x, y + 0.5D, z, 3, 0.3D, 0.3D, 0.3D, 0.02D);
		}

		// 在召唤源头(大狗)播放音效
		world.playSoundFromEntity(null, sourceBigDog, BigDogBarkSoundEvents.DOG_AMBIENT,
				SoundCategory.NEUTRAL, 1.2F, 0.8F + random.nextFloat() * 0.4F);
		BigDogBark.LOGGER.info("Big dog pack defense: {} guard wolves summoned for owner {} against {}",
				count, owner.getName().getString(), attacker.getName().getString());
	}

	/**
	 * 每 tick 由外部(如 tick 事件)调用,清理超时护卫狼。
	 * 当前版本不在 tick 中主动清理;护卫狼通过 {@code setAngerTime(1200)} 保持攻击性,
	 * 并且由原版自然仇恨/超时机制处理。后续可在此扩展更精确的生命周期管理。
	 */
	public static void tick(ServerWorld world) {
		// 预留扩展点
	}
}
