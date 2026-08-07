package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.item.CarriedBigDogItem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 大狗炮声波发射体:蓄能大狗从手中射出的音波冲击(狗本体不离开玩家)。
 *
 * <p>视觉与伤害参考原版 1.21.1 坚守者声波(1.21 起音波不再是实体,
 * 由 {@code sonic_boom} 粒子 + {@code sonicBoom} 伤害源构成):
 * 本实体以匀速波前推进,每 tick 在波前生成 sonic_boom 粒子(粒子扩散成环,
 * 路径上留下一串波环,形似激光/声波通道);
 * 沿途<b>摧毁</b>爆炸抗性低于阈值的方块(石头/木头/玻璃等建筑方块,流体、基岩、
 * 黑曜石等高抗性方块保留),对路径上的生物造成冲撞伤害与击退(每个实体只结算一次)。
 *
 * <p>发射时大狗<b>不消耗、不离开玩家手中</b>,可反复蓄能发射(发射后有短冷却);
 * 不存在物品复制/掉落回收问题。蓄能 Tick 数持久化,伤害/击退由 chargePower 重算。
 */
public class LaunchedBigDogEntity extends ProjectileEntity {

	/** 最大飞行寿命(Tick):约 45 格射程。 */
	public static final int MAX_LIFE_TICKS = 45;

	/** 可被声波摧毁的方块爆炸抗性上限(石头 6 / 木头 2 / 玻璃 0.3;黑曜石 1200、基岩保留)。 */
	private static final double DESTROY_BLAST_RESISTANCE_LIMIT = 1000.0D;

	/** 锥形扩散半角(度):绕飞行方向轴的三维锥体,水平/垂直/斜向均等扩散。 */
	private static final double SPREAD_HALF_ANGLE_DEG = 18.0D;

	/** 锥底基础半径(格):确保近距离也能破坏,连 tick 重叠无间隙。 */
	private static final double BASE_RADIUS = 2.0D;

	/** 波前薄层厚度(格):每 tick 只处理投影距离在此范围的方块,累积成完整锥形。 */
	private static final double LAYER_THICKNESS = 2.0D;

	/** 发射时的蓄能 Tick 数(16～89),唯一事实来源,伤害/击退由此重算。 */
	private int chargePower;

	/** 发射起点(扇形扩散的顶点,首次 tick 记录;存档加载后从当前位置重新扩散)。 */
	private Vec3d origin;

	/** 已结算伤害的实体(波前经过同一实体多个 tick 只结算一次)。 */
	private final Set<UUID> damagedEntities = new HashSet<>();

	public LaunchedBigDogEntity(EntityType<? extends LaunchedBigDogEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		// 声波发射体没有需要同步的追踪数据(Entity.initDataTracker 为抽象方法,必须实现)
	}

	/** 设置发射蓄能 Tick 数(限制在有效发射区间内)。 */
	public void setChargePower(int chargeTicks) {
		this.chargePower = MathHelper.clamp(chargeTicks, CarriedBigDogItem.MIN_CHARGE_TICKS,
				CarriedBigDogItem.OVERCHARGE_TICKS - 1);
	}

	public int getChargePower() {
		return this.chargePower;
	}

	/** 归一化蓄能 0(16 Tick)～1(89 Tick),伤害/击退的公共插值因子。 */
	public float getNormalizedCharge() {
		return MathHelper.clamp((this.chargePower - CarriedBigDogItem.MIN_CHARGE_TICKS)
				/ (float) (CarriedBigDogItem.OVERCHARGE_TICKS - 1 - CarriedBigDogItem.MIN_CHARGE_TICKS),
				0.0F, 1.0F);
	}

	/** 冲撞伤害:最低有效蓄能 6 点,接近过载 16 点,平滑变化(无视护甲的音波伤害)。 */
	public float getLaunchDamage() {
		return 6.0F + 10.0F * getNormalizedCharge();
	}

	/** 击退强度:0.5～2.0(速度倍数),不会把目标击飞几百格。 */
	public float getLaunchKnockback() {
		return 0.5F + 1.5F * getNormalizedCharge();
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isRemoved()) {
			return;
		}
		World world = this.getWorld();
		Vec3d velocity = this.getVelocity();
		// 客户端:仅跟随速度移动做视觉同步(粒子由服务端广播)
		if (world.isClient) {
			this.setPosition(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
			return;
		}
		ServerWorld serverWorld = (ServerWorld) world;
		// 射程耗尽:声波消失(大狗一直在玩家手中,无需掉落/回收)
		if (this.age >= MAX_LIFE_TICKS) {
			this.discard();
			return;
		}
		// 首次 tick 记录发射起点(扇形扩散的顶点)
		if (this.origin == null) {
			this.origin = this.getPos();
		}
		// 1. 圆锥形摧毁路径方块(见 destroyBlocksInFan)
		this.destroyBlocksInFan((ServerWorld) world, velocity);
		// 2. 圆锥形伤害生物:锥体内所有生物各结算一次(3D 判定,与方块破坏同一锥体)
		this.damageEntitiesInCone((ServerWorld) world, velocity);
		// 3. 波前粒子:原版声波扩散环,连续生成形成移动的波环/激光通道
		serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM,
				this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
		// 4. 匀速直线推进,朝向跟随速度方向(渲染预留)
		this.updateRotation();
		this.setPosition(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
	}

	/**
	 * 三维圆锥形摧毁:以发射起点为顶点、飞行方向为轴、半角 18° 的圆锥体,
	 * 每 tick 处理波前前后 LAYER_THICKNESS 厚的锥段,累积覆盖整条路径。
	 */
	private void destroyBlocksInFan(ServerWorld world, Vec3d dir) {
		Vec3d pos = this.getPos();
		double distance = this.origin.distanceTo(pos);
		double tanHalfAngle = Math.tan(Math.toRadians(SPREAD_HALF_ANGLE_DEG));
		double coneRadius = BASE_RADIUS + distance * tanHalfAngle;
		int r = (int) Math.ceil(coneRadius) + 1;
		BlockPos center = BlockPos.ofFloored(pos);
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos bp = center.add(dx, dy, dz);
					Vec3d rel = Vec3d.ofCenter(bp).subtract(this.origin);
					double t = rel.dotProduct(dir); // 沿飞行方向的投影距离
					if (t < 0.0D || t > distance + LAYER_THICKNESS) {
						continue; // 在发射点后面或超出波前太远
					}
					// 到轴线的垂直距离
					double dSq = rel.lengthSquared() - t * t;
					if (dSq < 0.0D) dSq = 0.0D;
					double d = Math.sqrt(dSq);
					double radiusAtT = BASE_RADIUS + t * tanHalfAngle;
					if (d > radiusAtT + 0.5D) {
						continue; // 在锥体外
					}
					// 薄层:每 tick 只处理波前附近新增的方块
					if (t < distance - LAYER_THICKNESS) {
						continue;
					}
					BlockState state = world.getBlockState(bp);
					if (!state.isAir() && state.getFluidState().isEmpty()
							&& state.getBlock().getBlastResistance() < DESTROY_BLAST_RESISTANCE_LIMIT) {
						world.breakBlock(bp, false, this, 0);
					}
				}
			}
		}
	}

	/**
	 * 圆锥体内全体生物伤害:以发射点为顶点、飞行方向为轴的三维圆锥判定,
	 * 锥体内的每个生物各结算一次(同一个生物在锥体中被扫描到多个 tick 也只伤一次)。
	 * 不伤害发射者本人。
	 */
	private void damageEntitiesInCone(ServerWorld world, Vec3d dir) {
		double distance = this.origin.distanceTo(this.getPos());
		double tanHalfAngle = Math.tan(Math.toRadians(SPREAD_HALF_ANGLE_DEG));
		double coneRadius = BASE_RADIUS + distance * tanHalfAngle;
		// 锥体包围盒(扫描范围内的生物)
		Box box = new Box(
				this.origin.getX() - coneRadius, this.origin.getY() - coneRadius, this.origin.getZ() - coneRadius,
				this.origin.getX() + coneRadius, this.origin.getY() + coneRadius, this.origin.getZ() + coneRadius)
				.offset(dir.multiply(distance * 0.5)); // 盒体居中于锥体中间
		for (Entity entity : world.getOtherEntities(this, box, e -> e instanceof LivingEntity)) {
			if (entity == this.getOwner()) {
				continue;
			}
			if (!this.damagedEntities.add(entity.getUuid())) {
				continue; // 已结算过的生物不再重复伤害
			}
			LivingEntity living = (LivingEntity) entity;
			Vec3d rel = living.getPos().subtract(this.origin);
			double t = rel.dotProduct(dir);
			if (t < 0.0D || t > distance + 2.0D) {
				continue; // 在发射点后面或超出波前
			}
			double dSq = Math.max(rel.lengthSquared() - t * t, 0.0D);
			double d = Math.sqrt(dSq);
			double radiusAtT = BASE_RADIUS + t * tanHalfAngle;
			if (d > radiusAtT + living.getWidth() * 0.5D) {
				continue; // 生物中心到轴的距离大于锥体半径,在锥体外
			}
			if (living.damage(this.getDamageSources().sonicBoom(this), this.getLaunchDamage())) {
				float knockback = this.getLaunchKnockback();
				living.addVelocity(dir.x * knockback, 0.3D * knockback + 0.2D, dir.z * knockback);
				living.velocityModified = true;
			}
		}
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("ChargePower", this.chargePower);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.chargePower = MathHelper.clamp(nbt.getInt("ChargePower"),
				CarriedBigDogItem.MIN_CHARGE_TICKS, CarriedBigDogItem.OVERCHARGE_TICKS - 1);
	}
}
