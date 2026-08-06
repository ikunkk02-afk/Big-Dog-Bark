package com.shouyun.bigdogbark.item;

import com.shouyun.bigdogbark.BigDogBark;
import com.shouyun.bigdogbark.entity.BigDogGrowth;
import com.shouyun.bigdogbark.entity.BigDogWolfUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 抱起的大狗物品。
 * <ul>
 *   <li>最大堆叠 1、防火、不可食用、当前无耐久与主动右键攻击;</li>
 *   <li>不加入普通创造模式物品栏(空物品无有效数据,不能生成狼);</li>
 *   <li><b>下蹲右键方块</b>:验证数据与主人后,用 EntityType.WOLF 生成大狗并恢复数据;
 *       成功生成后才消耗物品(创造模式也消耗,防止无限复制);空间不足/数据损坏/非主人均不消耗;</li>
 *   <li><b>普通右键</b>:返回 PASS,为后续蓄能和机枪附魔的右键操作预留;</li>
 *   <li>数据保存在 CUSTOM_DATA 组件({@link CarriedBigDogData}),铁砧重命名、
 *       箱子/潜影盒存放、死亡掉落、重启均不会丢失(物品组件由 Minecraft 正常保存)。</li>
 * </ul>
 */
public class CarriedBigDogItem extends Item {

	public CarriedBigDogItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();
		// 防御:count 已为 0 的残留栈(快速连点/同步竞态)不可再次使用,防止重复放下
		if (player == null || stack.isEmpty()) {
			return ActionResult.PASS;
		}
		// 普通右键(不下蹲):PASS,为后续蓄能和机枪功能预留
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
		tooltip.add(Text.translatable("item.big_dog_bark.carried_big_dog.place").formatted(Formatting.DARK_GRAY));
	}

	private static void sendActionBar(PlayerEntity player, String translationKey) {
		player.sendMessage(Text.translatable(translationKey), true);
	}
}
