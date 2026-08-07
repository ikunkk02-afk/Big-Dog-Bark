package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.item.CarriedBigDogItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 大狗炮投射物:“发射的大狗”。
 *
 * <p>设计要点(第五阶段):
 * <ul>
 *   <li><b>不是</b>把普通 WolfEntity 甩出去当炮弹——投射物内部保存<b>完整</b>的
 *       {@code carried_big_dog} ItemStack(含 OwnerUuid / WolfData / 蓄能附魔 /
 *       自定义名称等全部组件),与 {@link ItemEntity} 相同的序列化方式
 *       ({@code ItemStack.encode} + {@code ItemStack.fromNbtOrEmpty});</li>
 *   <li>客户端通过 DataTracker 同步该 ItemStack,渲染时复用原版 WolfEntityRenderer
 *       (见客户端 {@code LaunchedBigDogEntityRenderer}),不复制第二套狼模型;</li>
 *   <li>服务端权威碰撞:命中实体造成冲撞伤害与击退,命中方块不破坏;任何命中
 *       路径(实体/方块/超时/掉出世界)只会掉落一次大狗({@link #resolved});</li>
 *   <li>发射时 chargePower 持久化,伤害/击退/速度均可由 chargePower 重算;</li>
 *   <li>Owner UUID 由 {@link ProjectileEntity} 原版 NBT 键 {@code Owner} 保存;</li>
 *   <li>超时(200 Tick)就地掉落;掉出世界底部时优先安全回收(在线主人背包
 *       {@code offerOrDrop},否则世界出生点),绝不静默删除独一无二的大狗。</li>
 * </ul>
 */
public class LaunchedBigDogEntity extends ProjectileEntity {

	/** 最大飞行寿命(Tick),10 秒。 */
	public static final int MAX_LIFE_TICKS = 200;

	/** 世界底部回收判定下探距离(方块)。 */
	private static final double OUT_OF_WORLD_MARGIN = 32.0D;

	/** 掉落物拾取延迟(Tick),防止发射者命中瞬间立刻捡回。 */
	private static final int DROP_PICKUP_DELAY = 10;

	private static final TrackedData<ItemStack> CARRIED_STACK = DataTracker.registerData(
			LaunchedBigDogEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

	/** 发射时的蓄能 Tick 数(16～89),唯一事实来源,伤害/击退由此重算。 */
	private int chargePower;

	/** 一次性掉落保护:任何处理路径(命中/超时/回收)只会掉落一次大狗。 */
	private boolean resolved;

	public LaunchedBigDogEntity(EntityType<? extends LaunchedBigDogEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		// 1.21.1 的 ProjectileEntity 不覆写 initDataTracker(主人是普通字段,
		// 由 EntitySpawnS2CPacket 的 entityData 同步),因此这里不能调 super,
		// 直接注册自己的追踪数据;与 ItemEntity 相同,整栈(含全部数据组件)同步到客户端
		builder.add(CARRIED_STACK, ItemStack.EMPTY);
	}

	/** 服务端发射时设置:保存完整的大狗物品(复制,防外部栈被修改)。 */
	public void setCarriedStack(ItemStack stack) {
		this.getDataTracker().set(CARRIED_STACK, stack.copy());
	}

	/** 客户端/服务端读取投射物携带的大狗物品。 */
	public ItemStack getCarriedStack() {
		return this.getDataTracker().get(CARRIED_STACK);
	}

	/** 设置发射蓄能 Tick 数(限制在有效发射区间内)。 */
	public void setChargePower(int chargeTicks) {
		this.chargePower = MathHelper.clamp(chargeTicks, CarriedBigDogItem.MIN_CHARGE_TICKS,
				CarriedBigDogItem.OVERCHARGE_TICKS - 1);
	}

	public int getChargePower() {
		return this.chargePower;
	}

	/** 归一化蓄能 0(16 Tick)～1(89 Tick),伤害/击退/速度的公共插值因子。 */
	public float getNormalizedCharge() {
		return MathHelper.clamp((this.chargePower - CarriedBigDogItem.MIN_CHARGE_TICKS)
				/ (float) (CarriedBigDogItem.OVERCHARGE_TICKS - 1 - CarriedBigDogItem.MIN_CHARGE_TICKS),
				0.0F, 1.0F);
	}

	/** 冲撞伤害:最低有效蓄能 6 点,接近过载 16 点,平滑变化。 */
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
		// 客户端:仅跟随速度移动做视觉同步,不处理碰撞/掉落(服务端权威)
		if (world.isClient) {
			this.setPosition(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
			return;
		}
		// 飞行寿命超时:就地掉落大狗后消失(绝不静默删除)
		if (this.age >= MAX_LIFE_TICKS) {
			this.resolveAt(this.getPos());
			this.discard();
			return;
		}
		// 掉出世界底部以下:优先安全回收(在线主人背包,否则世界出生点)
		if (this.getY() < world.getBottomY() - OUT_OF_WORLD_MARGIN) {
			this.recoverOutOfWorld();
			return;
		}
		// 服务端碰撞检测(含方块与实体),canHit 默认排除主人及其坐骑直到离手
		HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
		if (hitResult.getType() != HitResult.Type.MISS) {
			this.onCollision(hitResult);
			if (this.isRemoved()) {
				return;
			}
		}
		// 匀速直线飞行,无重力;朝向跟随速度方向(渲染用)
		this.updateRotation();
		this.setPosition(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		if (this.resolved) {
			return;
		}
		super.onCollision(hitResult);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		if (this.resolved || this.getWorld().isClient) {
			return;
		}
		this.resolved = true;
		Entity target = entityHitResult.getEntity();
		LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
		// 冲撞伤害:服务端唯一伤害权威(蓄能附魔大狗是纯撞击,不破坏方块/不点燃/不爆炸)
		if (target.damage(this.getDamageSources().mobProjectile(this, owner), this.getLaunchDamage())) {
			Vec3d dir = this.getVelocity().normalize();
			float knockback = this.getLaunchKnockback();
			target.addVelocity(dir.x * knockback, 0.3D * knockback + 0.2D, dir.z * knockback);
			target.velocityModified = true;
		}
		// 命中后:投射物消失,在命中位置掉落原来的大狗物品(保留全部数据)
		this.resolveAt(entityHitResult.getPos());
		this.discard();
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		if (this.resolved || this.getWorld().isClient) {
			return;
		}
		this.resolved = true;
		// 不破坏方块,直接掉落大狗
		this.resolveAt(blockHitResult.getPos());
		this.discard();
	}

	/**
	 * 在指定位置掉落内部保存的大狗物品(唯一执行点,由 {@link #resolved} 保证只掉一次)。
	 * 直接转移原完整栈,绝不新建空物品(否则丢失 OwnerUuid/WolfData/附魔/名称)。
	 */
	private void resolveAt(Vec3d pos) {
		World world = this.getWorld();
		ItemStack stack = this.getCarriedStack();
		if (stack.isEmpty()) {
			return;
		}
		// 先清空追踪数据再掉落:同一投射物不可能再掉第二次
		this.getDataTracker().set(CARRIED_STACK, ItemStack.EMPTY);
		ItemEntity itemEntity = new ItemEntity(world, pos.x, pos.y + 0.3D, pos.z, stack);
		itemEntity.setVelocity(world.random.nextTriangular(0.0D, 0.2D), 0.2D,
				world.random.nextTriangular(0.0D, 0.2D));
		itemEntity.setPickupDelay(DROP_PICKUP_DELAY);
		world.spawnEntity(itemEntity);
	}

	/**
	 * 掉出世界底部时的安全回收:独一无二的大狗不能因投射物超出世界而丢失。
	 * 主人仍在线 → 放回背包(背包满自动掉在脚下);主人离线/不存在 → 世界出生点安全生成。
	 */
	private void recoverOutOfWorld() {
		ServerWorld world = (ServerWorld) this.getWorld();
		ItemStack stack = this.getCarriedStack();
		if (stack.isEmpty()) {
			this.discard();
			return;
		}
		this.resolved = true;
		this.getDataTracker().set(CARRIED_STACK, ItemStack.EMPTY);
		Entity owner = this.getOwner();
		if (owner instanceof ServerPlayerEntity player && !player.isRemoved()) {
			player.getInventory().offerOrDrop(stack);
		} else {
			BlockPos spawnPos = world.getSpawnPos();
			world.spawnEntity(new ItemEntity(world, spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D,
					spawnPos.getZ() + 0.5D, stack));
		}
		this.discard();
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		ItemStack stack = this.getCarriedStack();
		if (!stack.isEmpty()) {
			// 与 ItemEntity 相同的原版序列化,不丢任何数据组件
			nbt.put("CarriedStack", stack.encode(this.getRegistryManager()));
		}
		nbt.putInt("ChargePower", this.chargePower);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.getDataTracker().set(CARRIED_STACK,
				ItemStack.fromNbtOrEmpty(this.getRegistryManager(), nbt.getCompound("CarriedStack")));
		this.chargePower = MathHelper.clamp(nbt.getInt("ChargePower"),
				CarriedBigDogItem.MIN_CHARGE_TICKS, CarriedBigDogItem.OVERCHARGE_TICKS - 1);
	}
}
