package com.shouyun.bigdogbark.item;

import com.shouyun.bigdogbark.BigDogBark;
import com.shouyun.bigdogbark.enchantment.BigDogEnchantmentUtil;
import com.shouyun.bigdogbark.entity.BigDogBarkEntityTypes;
import com.shouyun.bigdogbark.entity.BigDogGrowth;
import com.shouyun.bigdogbark.entity.BigDogWeaponMode;
import com.shouyun.bigdogbark.entity.BigDogWolfUtil;
import com.shouyun.bigdogbark.entity.LaunchedBigDogEntity;
import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * 抱起的大狗物品。
 * <ul>
 *   <li>最大堆叠 1、防火、不可食用;</li>
 *   <li><b>下蹲右键方块</b>:验证数据与主人后,用 EntityType.WOLF 生成大狗并恢复数据;
 *       成功生成后才消耗物品(创造模式也消耗,防止无限复制);</li>
 *   <li><b>普通右键(带“蓄能 I”附魔)</b>:按住蓄力,松开发射大狗炮
 *       (第五阶段,见 {@link #use} / {@link #usageTick} / {@link #onStoppedUsing});</li>
 *   <li>没有蓄能附魔时普通右键保持 PASS,不干扰旧交互;</li>
 *   <li>数据保存在 CUSTOM_DATA 组件({@link CarriedBigDogData}),铁砧重命名、
 *       箱子存放、死亡掉落、重启均不会丢失。</li>
 * </ul>
 *
 * <p>蓄能时间线(统一 Tick 常量,20 Tick = 1 秒):
 * <pre>
 * 0 ～ 15     蓄能不足,松开不发射
 * 16 ～ 39    可以发射,威力较低
 * 40 ～ 69    正常高威力发射(40 Tick 播放一次 ready 音效 + 动作栏)
 * 70 ～ 89    危险过载区间(70 Tick 警告一次 + 警告粒子),仍可松开发射
 * 90         自动炸膛(不发射,大狗留在手中,原版爆炸声/粒子,3 秒冷却)
 * </pre>
 */
public class CarriedBigDogItem extends Item {

	/** 最低有效蓄能 Tick。 */
	public static final int MIN_CHARGE_TICKS = 16;

	/** 蓄能完成 Tick(播放一次 ready 音效与动作栏提示)。 */
	public static final int READY_CHARGE_TICKS = 40;

	/** 危险过载区间起始 Tick(警告一次 + 警告粒子)。 */
	public static final int DANGER_CHARGE_TICKS = 70;

	/** 过载炸膛 Tick(达到即自动炸膛,不等松手)。 */
	public static final int OVERCHARGE_TICKS = 90;

	/** 最大使用时间:保持足够长,由 usageTick/onStoppedUsing 自行控制 90 Tick 炸膛。 */
	public static final int MAX_USE_TIME = 72000;

	/** 炸膛后冷却 Tick(3 秒)。 */
	public static final int MISFIRE_COOLDOWN_TICKS = 60;

	/** 发射后冷却 Tick(1 秒,限制射速;大狗不消耗,可反复蓄能发射)。 */
	public static final int LAUNCH_COOLDOWN_TICKS = 20;

	/** 机枪连射间隔(Tick):蓄能达到 40 Tick 后每 4 Tick 自动射一发。 */
	public static final int MACHINE_GUN_FIRE_INTERVAL = 4;

	/** 机枪每发子弹消耗的蓄能 Tick:消耗速度 > 自然恢复,持续射击会耗尽能量。 */
	public static final int MACHINE_GUN_COST_PER_SHOT = 10;

	/** 每个玩家当前机枪连射已消耗的子弹数(松开/死亡/断线时清除)。 */
	private static final Map<UUID, Integer> MACHINE_GUN_FIRE_COUNT = new HashMap<>();

	/** 炸膛爆炸威力(视觉约 2～3 级,不破坏地形)。 */
	private static final float MISFIRE_EXPLOSION_POWER = 3.0F;

		// 炸膛对玩家的固定伤害(约 3.5 颗心,不会一炸即死)。
	private static final float MISFIRE_PLAYER_DAMAGE = 7.0F;

	/** 炸膛对玩家的击退力度。 */
	private static final double MISFIRE_KNOCKBACK = 1.8D;

	/** 机枪子弹速度(格/tick):比蓄能声波更快,手感更像子弹。 */
	private static final double MACHINE_GUN_BULLET_SPEED = 2.0D;

	public CarriedBigDogItem(Settings settings) {
		super(settings);
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		// 蓄力时长由 usageTick 控制(90 Tick 炸膛),此值只保证使用状态不被提前终止
		return MAX_USE_TIME;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();
		// 防御:count 已为 0 的残留栈(快速连点/同步竞态)不可再次使用,防止重复放下
		if (player == null || stack.isEmpty()) {
			return ActionResult.PASS;
		}
		// 普通右键(不下蹲):PASS,交给 use() 启动蓄力(若带蓄能附魔)
		if (!player.isSneaking()) {
			return ActionResult.PASS;
		}
		World world = context.getWorld();
		if (world.isClient) {
			// 客户端:取消本地方块使用动画并发送数据包,实际放回由服务端权威执行
			return ActionResult.success(true);
		}
		return placeBigDog(context, (ServerWorld) world, player);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (stack.isEmpty()) {
			return TypedActionResult.pass(stack);
		}
		// 下蹲:不启动蓄能(下蹲右键=放狗,由 useOnBlock 处理;下蹲右键空气无操作)
		if (user.isSneaking()) {
			return TypedActionResult.pass(stack);
		}
		// 数据无效 / 非主人:FAIL 让客户端发送交互包,服务端给出明确提示
		if (stack.isOf(BigDogBarkItems.CARRIED_BIG_DOG)) {
			if (!CarriedBigDogData.hasValidData(stack)) {
				if (!world.isClient) {
					sendActionBar((ServerPlayerEntity) user, "action.big_dog_bark.charge_invalid_dog");
				}
				return TypedActionResult.fail(stack);
			}
			Optional<UUID> ownerUuid = CarriedBigDogData.getOwnerUuid(stack);
			if (ownerUuid.isEmpty() || !ownerUuid.get().equals(user.getUuid())) {
				if (!world.isClient) {
					sendActionBar((ServerPlayerEntity) user, "action.big_dog_bark.dog_not_owner");
				}
				return TypedActionResult.fail(stack);
			}
		}
		// 无蓄能附魔 / 观战者 / 冷却中:静默 PASS(不干扰旧交互)
		if (!canStartCharging(world, user, stack)) {
			return TypedActionResult.pass(stack);
		}
		// 与 1.21.1 原版弓相同的起手模式:设置当前手并返回 consume
		user.setCurrentHand(hand);
		if (!world.isClient) {
			// playSoundFromEntity:音效跟随玩家实体移动,不会停留在原地
			world.playSoundFromEntity(null, user, BigDogBarkSoundEvents.DOG_CHARGE,
					SoundCategory.PLAYERS, 1.0F, 1.0F);
		}
		return TypedActionResult.consume(stack);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
			return;
		}
		int elapsed = MAX_USE_TIME - remainingUseTicks;
		// 机枪模式:蓄能 40 Tick 后进入连射,每发消耗能量
		if (BigDogEnchantmentUtil.hasMachineGun(stack, player.getRegistryManager())) {
			int fireCount = MACHINE_GUN_FIRE_COUNT.getOrDefault(player.getUuid(), 0);
			int energy = elapsed - fireCount * MACHINE_GUN_COST_PER_SHOT;
			if (elapsed == READY_CHARGE_TICKS) {
				world.playSoundFromEntity(null, player, BigDogBarkSoundEvents.DOG_CHARGE_READY,
						SoundCategory.PLAYERS, 1.0F, 1.0F);
				sendActionBar(player, "action.big_dog_bark.machine_gun_ready");
			}
			if (elapsed >= READY_CHARGE_TICKS && energy >= MACHINE_GUN_COST_PER_SHOT
					&& (elapsed - READY_CHARGE_TICKS) % MACHINE_GUN_FIRE_INTERVAL == 0) {
				fireMachineGunBullet((ServerWorld) world, player, stack);
				MACHINE_GUN_FIRE_COUNT.put(player.getUuid(), fireCount + 1);
			}
			return; // 机枪模式不触发蓄能/炸膛逻辑
		}
		// 蓄能模式:40 Tick 提示,70 Tick 警告,90 Tick 炸膛
		if (elapsed == READY_CHARGE_TICKS) {
			world.playSoundFromEntity(null, player, BigDogBarkSoundEvents.DOG_CHARGE_READY,
					SoundCategory.PLAYERS, 1.0F, 1.0F);
			sendActionBar(player, "action.big_dog_bark.charge_ready");
		} else if (elapsed == DANGER_CHARGE_TICKS) {
			sendActionBar(player, "action.big_dog_bark.charge_danger");
			((ServerWorld) world).spawnParticles(ParticleTypes.EXPLOSION,
					player.getX(), player.getY() + 1.0D, player.getZ(),
					6, 0.5D, 0.5D, 0.5D, 0.02D);
		} else if (elapsed >= OVERCHARGE_TICKS) {
			misfire((ServerWorld) world, player, stack);
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
			return;
		}
		// 玩家死亡/已移除时不发射
		if (player.isRemoved() || !player.isAlive()) {
			return;
		}
		int elapsed = MAX_USE_TIME - remainingUseTicks;
		// 防双执行:炸膛后返回
		if (elapsed >= OVERCHARGE_TICKS) {
			return;
		}
		// 蓄能不足:不发射
		if (elapsed < MIN_CHARGE_TICKS) {
			sendActionBar(player, "action.big_dog_bark.charge_too_low");
			player.getItemCooldownManager().set(stack.getItem(), LAUNCH_COOLDOWN_TICKS);
			return;
		}
		// 机枪模式:松开即停,清除子弹计数,短冷却再蓄能
		if (BigDogEnchantmentUtil.hasMachineGun(stack, player.getRegistryManager())) {
			MACHINE_GUN_FIRE_COUNT.remove(player.getUuid());
			player.getItemCooldownManager().set(stack.getItem(), LAUNCH_COOLDOWN_TICKS);
			return;
		}
		// 蓄能模式:正常发射单发声波
		launchBigDog((ServerWorld) world, player, stack, elapsed);
	}

	/** 服务端放下流程:验证 → 创建狼 → 恢复数据 → 安全状态 → 空间检查 → 生成 → 消耗物品。 */
	private ActionResult placeBigDog(ItemUsageContext context, ServerWorld world, PlayerEntity player) {
		ItemStack stack = context.getStack();
		// 1. 数据完整
		if (!CarriedBigDogData.hasValidData(stack)) {
			sendActionBar(player, "action.big_dog_bark.carried_dog_invalid_data");
			return ActionResult.SUCCESS;
		}
		// 2. 主人验证(OwnerUuid 是唯一权限依据,OwnerName 仅用于提示)
		Optional<UUID> ownerUuid = CarriedBigDogData.getOwnerUuid(stack);
		if (ownerUuid.isEmpty() || !ownerUuid.get().equals(player.getUuid())) {
			sendActionBar(player, "action.big_dog_bark.dog_not_owner");
			return ActionResult.SUCCESS;
		}
		// 3. 只允许生成 WolfEntity(永远通过 EntityType.WOLF 创建,不接受任意实体 ID)
		WolfEntity wolf = EntityType.WOLF.create(world);
		if (wolf == null) {
			BigDogBark.LOGGER.error("Failed to create WolfEntity while placing carried big dog at {}",
					context.getBlockPos());
			sendActionBar(player, "action.big_dog_bark.carried_dog_invalid_data");
			return ActionResult.SUCCESS;
		}
		// 4. 生成位置:点击方块面的相邻位置
		BlockPos clickedPos = context.getBlockPos();
		Direction side = context.getSide();
		Vec3d spawnPos = Vec3d.ofCenter(clickedPos.offset(side));
		wolf.setPosition(spawnPos);
		// 5. 恢复数据(内部验证 IsBigDog / GrowthPoints,readNbt 触发 Mixin 自动应用缩放)
		if (!CarriedBigDogData.restoreWolfData(stack, wolf)) {
			sendActionBar(player, "action.big_dog_bark.carried_dog_invalid_data");
			return ActionResult.SUCCESS;
		}
		// 5.5 武器模式:物品当前带“蓄能 I” → CHARGE;带“机枪” → MACHINE_GUN;否则 NONE
		if (BigDogEnchantmentUtil.hasMachineGun(stack, player.getRegistryManager())) {
			BigDogWolfUtil.setWeaponMode(wolf, BigDogWeaponMode.MACHINE_GUN);
		} else if (BigDogEnchantmentUtil.hasCharge(stack, player.getRegistryManager())) {
			BigDogWolfUtil.setWeaponMode(wolf, BigDogWeaponMode.CHARGE);
		} else {
			BigDogWolfUtil.setWeaponMode(wolf, BigDogWeaponMode.NONE);
		}
		// 6. 强制安全状态(不能完全信任物品中的主人字段,已通过 OwnerUuid 验证当前玩家)
		wolf.setTamed(true, false);
		wolf.setOwner(player);
		wolf.setSitting(true);
		wolf.setTarget(null);
		wolf.setAngerTime(0);
		wolf.getNavigation().stop();
		// 7. 按完全成长重新应用缩放并重算碰撞箱
		BigDogWolfUtil.applyGrowthScale(wolf);
		wolf.calculateDimensions();
		// 8. 按 2 倍大狗实际碰撞箱检查空间与边界
		Box box = wolf.getBoundingBox();
		if (!world.isSpaceEmpty(box) || !world.getWorldBorder().contains(box.getCenter())) {
			sendActionBar(player, "action.big_dog_bark.not_enough_place_space");
			return ActionResult.SUCCESS;
		}
		// 9. 生成成功才消耗物品(创造模式也消耗,防止无限复制大狗)
		if (!world.spawnEntity(wolf)) {
			sendActionBar(player, "action.big_dog_bark.not_enough_place_space");
			return ActionResult.SUCCESS;
		}
		stack.decrement(1);
		// 立即清空空栈槽位,防止残留 count=0 的栈被再次使用(重复放下/复制)
		if (stack.isEmpty()) {
			player.setStackInHand(context.getHand(), ItemStack.EMPTY);
		}
		// 10. 物品自定义名称(铁砧重命名)优先作为狼的新名称;未重命名则恢复 WolfData 中的原名
		Text itemCustomName = stack.get(DataComponentTypes.CUSTOM_NAME);
		if (itemCustomName != null) {
			wolf.setCustomName(itemCustomName);
		}
		// 11. 反馈:原版羊毛放置声 + 少量粒子 + 动作栏提示(服务端广播,距离衰减)
		world.playSound(null, wolf.getBlockPos(), SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
				wolf.getX(), wolf.getY() + wolf.getHeight() * 0.5D, wolf.getZ(),
				8, 0.5D, 0.5D, 0.5D, 0.1D);
		sendActionBar(player, "action.big_dog_bark.dog_placed");
		return ActionResult.SUCCESS;
	}

	/** 是否满足启动蓄力条件:带蓄能或机枪附魔 + 有效大狗 + 主人 + 非观战 + 不在冷却。 */
	private boolean canStartCharging(World world, PlayerEntity player, ItemStack stack) {
		if (player.isSpectator() || player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
			return false;
		}
		if (!stack.isOf(BigDogBarkItems.CARRIED_BIG_DOG)) {
			return false;
		}
		if (!CarriedBigDogData.hasValidData(stack)) {
			return false;
		}
		Optional<UUID> ownerUuid = CarriedBigDogData.getOwnerUuid(stack);
		if (ownerUuid.isEmpty() || !ownerUuid.get().equals(player.getUuid())) {
			return false;
		}
		return BigDogEnchantmentUtil.getChargeLevel(stack, player.getRegistryManager()) >= 1
				|| BigDogEnchantmentUtil.getMachineGunLevel(stack, player.getRegistryManager()) >= 1;
	}

	/**
	 * 发射大狗炮(服务端权威):从玩家眼前(狗嘴方向)射出<b>声波发射体</b>,
	 * 大狗本体<b>不消耗、不离开玩家手中</b>,可反复蓄能发射。
	 * 声波沿途扇形摧毁方块并对生物造成冲撞伤害;spawnEntity 失败则直接提示(无任何损失)。
	 */
	private void launchBigDog(ServerWorld world, ServerPlayerEntity player, ItemStack stack, int chargeTicks) {
		// 发射前最终校验(竞态防御):数据 / 主人 / 附魔
		if (!CarriedBigDogData.hasValidData(stack)) {
			sendActionBar(player, "action.big_dog_bark.charge_invalid_dog");
			return;
		}
		Optional<UUID> ownerUuid = CarriedBigDogData.getOwnerUuid(stack);
		if (ownerUuid.isEmpty() || !ownerUuid.get().equals(player.getUuid())) {
			sendActionBar(player, "action.big_dog_bark.dog_not_owner");
			return;
		}
		if (BigDogEnchantmentUtil.getChargeLevel(stack, player.getRegistryManager()) < 1) {
			return;
		}
		// 1. 创建声波发射体(从玩家眼睛前方一格射出,即“狗嘴”位置)
		LaunchedBigDogEntity projectile = BigDogBarkEntityTypes.LAUNCHED_BIG_DOG.create(world);
		if (projectile == null) {
			sendActionBar(player, "action.big_dog_bark.launch_failed");
			return;
		}
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1.0F);
		projectile.setPosition(eye.x + look.x, eye.y - 0.1D + look.y, eye.z + look.z);
		projectile.setOwner(player);
		projectile.setChargePower(chargeTicks);
		// 匀速 1 格/tick 直线推进(约 45 格射程),蓄能只影响伤害/击退
		projectile.setVelocity(look.x, look.y, look.z);
		// 2. 生成失败:无任何损失(大狗仍在手中,不复制)
		if (!world.spawnEntity(projectile)) {
			sendActionBar(player, "action.big_dog_bark.launch_failed");
			return;
		}
		// 3. 大狗留在玩家手中;发射后短冷却限制射速
		player.getItemCooldownManager().set(stack.getItem(), LAUNCH_COOLDOWN_TICKS);
		world.playSoundFromEntity(null, player, BigDogBarkSoundEvents.DOG_LAUNCH,
				SoundCategory.PLAYERS, 1.0F, 1.0F);
	}

	/**
	 * 炸膛(90 Tick 自动触发,服务端):
	 * 停止使用 → 原版爆炸(威力 3,不破坏地形/不点火,原版爆炸声 + 粒子)→
	 * 玩家受伤 + 明显击退 → <b>强行发射</b>最大威力声波(蓄能按 89 Tick 结算)→
	 * 动作栏提示 → 3 秒物品冷却。大狗始终留在玩家手中。
	 */
	private void misfire(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		// 先停止使用:触发 onStoppedUsing,其内部用 elapsed >= 90 拦截,保证不二次发射
		player.stopUsingItem();
		// 原版爆炸:ExplosionSourceType.NONE → DestructionType.KEEP → 不破坏方块、不点火
		// 声音与粒子通过 ExplosionS2CPacket 同步给客户端(原版爆炸声 + 原版爆炸粒子)
		Vec3d pos = player.getPos();
		world.createExplosion(player, player.getDamageSources().playerAttack(player), new ExplosionBehavior(),
				pos.x, pos.y + 0.5D, pos.z, MISFIRE_EXPLOSION_POWER, false, World.ExplosionSourceType.NONE,
				ParticleTypes.EXPLOSION_EMITTER, ParticleTypes.EXPLOSION_EMITTER,
				SoundEvents.ENTITY_GENERIC_EXPLODE);
		// 玩家本体伤害(爆炸源被原版逻辑豁免,这里直接结算固定伤害)
		player.damage(player.getDamageSources().explosion(null, player), MISFIRE_PLAYER_DAMAGE);
		// 明显击退:朝玩家视线反方向(炸膛把自己向后炸开)
		Vec3d look = player.getRotationVec(1.0F);
		player.addVelocity(-look.x * MISFIRE_KNOCKBACK, 0.5D, -look.z * MISFIRE_KNOCKBACK);
		player.velocityModified = true;
		// 强行发射:过载能量以最大威力声波射出(蓄能按 89 Tick 结算,伤害最高 16)
		// 与正常发射共用创建逻辑,但不再单独设置发射冷却(炸膛冷却 3 秒优先)
		LaunchedBigDogEntity projectile = BigDogBarkEntityTypes.LAUNCHED_BIG_DOG.create(world);
		if (projectile != null) {
			Vec3d eye = player.getEyePos();
			projectile.setPosition(eye.x + look.x, eye.y - 0.1D + look.y, eye.z + look.z);
			projectile.setOwner(player);
			projectile.setChargePower(OVERCHARGE_TICKS - 1);
			projectile.setVelocity(look.x, look.y, look.z);
			if (world.spawnEntity(projectile)) {
				world.playSoundFromEntity(null, player, BigDogBarkSoundEvents.DOG_LAUNCH,
						SoundCategory.PLAYERS, 1.0F, 1.0F);
			}
		}
		sendActionBar(player, "action.big_dog_bark.charge_misfire");
		// 3 秒内不能再次蓄能
		player.getItemCooldownManager().set(stack.getItem(), MISFIRE_COOLDOWN_TICKS);
	}

	/** 发射一发机枪小冲击波:短射程、不破坏方块、速度快、低伤害。 */
	private void fireMachineGunBullet(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		LaunchedBigDogEntity bullet = BigDogBarkEntityTypes.LAUNCHED_BIG_DOG.create(world);
		if (bullet == null) {
			return;
		}
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1.0F);
		bullet.setPosition(eye.x + look.x, eye.y - 0.1D + look.y, eye.z + look.z);
		bullet.setOwner(player);
		bullet.setChargePower(MIN_CHARGE_TICKS);
		bullet.setMachineGunBulletParams(); // 短射程 + 不破坏方块
		bullet.setVelocity(look.x * MACHINE_GUN_BULLET_SPEED, look.y * MACHINE_GUN_BULLET_SPEED,
				look.z * MACHINE_GUN_BULLET_SPEED);
		world.spawnEntity(bullet);
		world.playSoundFromEntity(null, player, BigDogBarkSoundEvents.DOG_MACHINE_GUN,
				SoundCategory.PLAYERS, 1.0F, 1.0F);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		if (!CarriedBigDogData.hasValidData(stack)) {
			tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.invalid").formatted(Formatting.RED));
			return;
		}
		String ownerName = CarriedBigDogData.getOwnerName(stack);
		if (!ownerName.isEmpty()) {
			tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.owner", ownerName)
					.formatted(Formatting.GRAY));
		}
		int growthPoints = CarriedBigDogData.getWolfData(stack).getInt("BigDogBark.GrowthPoints");
		tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.growth",
				growthPoints, BigDogGrowth.MAX_GROWTH_POINTS).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.stage").formatted(Formatting.GRAY));
		float health = CarriedBigDogData.getSavedHealth(stack);
		float maxHealth = CarriedBigDogData.getSavedMaxHealth(stack);
		tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.health",
				(int) health, (int) maxHealth).formatted(Formatting.GRAY));
		// 只有真正带“蓄能”附魔的大狗才显示蓄能武器说明
		if (BigDogEnchantmentUtil.hasChargeInComponents(stack)) {
			tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.charge_help1")
					.formatted(Formatting.DARK_GRAY));
			tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.charge_help2")
					.formatted(Formatting.DARK_GRAY));
		}
		// 带“机枪”附魔的大狗显示机枪说明
		if (BigDogEnchantmentUtil.hasMachineGunInComponents(stack)) {
			tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.machine_gun_help")
					.formatted(Formatting.DARK_GRAY));
		}
		tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.place").formatted(Formatting.DARK_GRAY));
	}

	private static void sendActionBar(PlayerEntity player, String translationKey) {
		player.sendMessage(Text.translatable(translationKey), true);
	}
}
