package com.shouyun.bigdogbark.item;

import com.shouyun.bigdogbark.entity.BigDogGrowth;
import com.shouyun.bigdogbark.entity.BigDogWeaponMode;
import com.shouyun.bigdogbark.entity.BigDogWolfUtil;
import com.shouyun.bigdogbark.enchantment.BigDogEnchantmentUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 抱起的大狗物品数据工具:所有 CUSTOM_DATA 读写统一经本类,禁止在交互处理器中散落 NBT 键名。
 *
 * <p>数据结构(物品 CUSTOM_DATA 组件内,独立根节点):
 * <pre>
 * BigDogBark.CarriedDog
 * ├── Version      (int, 当前 1)
 * ├── OwnerUuid    (UUID, 逻辑校验用,唯一权限依据)
 * ├── OwnerName    (String, 仅用于提示文本,不能作为权限判断)
 * └── WolfData     (Compound, 狼实体数据快照,已移除身份/位置/移动/维度/乘客/拴绳等危险字段)
 * </pre>
 */
public final class CarriedBigDogData {

	public static final String ROOT_KEY = "BigDogBark.CarriedDog";
	public static final String VERSION_KEY = "Version";
	public static final String OWNER_UUID_KEY = "OwnerUuid";
	public static final String OWNER_NAME_KEY = "OwnerName";
	public static final String WOLF_DATA_KEY = "WolfData";
	public static final int DATA_VERSION = 1;

	/** 快照中必须移除的临时身份/危险字段。 */
	private static final Set<String> DANGEROUS_KEYS = Set.of(
			"id", "UUID", "Pos", "Motion", "Rotation", "Dimension", "Passengers",
			"Leash", "LeashUuid", "HurtByTimestamp", "AngerTime", "AngryAt",
			"FallDistance", "DeathTime", "HurtTime", "PortalCooldown", "Brain",
			"Sitting", "SleepingX", "SleepingY", "SleepingZ");

	private CarriedBigDogData() {
	}

	/** 由狼与主人创建抱起的大狗物品(服务端调用)。 */
	public static ItemStack createFromWolf(WolfEntity wolf, ServerPlayerEntity owner) {
		ItemStack stack = new ItemStack(BigDogBarkItems.CARRIED_BIG_DOG);
		// CUSTOM_DATA 顶层为独立根节点 BigDogBark.CarriedDog
		NbtCompound root = new NbtCompound();
		NbtCompound carriedDog = new NbtCompound();
		carriedDog.putInt(VERSION_KEY, DATA_VERSION);
		carriedDog.putUuid(OWNER_UUID_KEY, owner.getUuid());
		carriedDog.putString(OWNER_NAME_KEY, owner.getGameProfile().getName());
		NbtCompound wolfData = new NbtCompound();
		wolf.writeNbt(wolfData);
		for (String key : DANGEROUS_KEYS) {
			wolfData.remove(key);
		}
		carriedDog.put(WOLF_DATA_KEY, wolfData);
		root.put(ROOT_KEY, carriedDog);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
		// 狼的自定义名称应用到物品显示名(放下时用于恢复或覆盖)
		if (wolf.hasCustomName()) {
			stack.set(DataComponentTypes.CUSTOM_NAME, wolf.getCustomName());
		}
		// 武器模式恢复:蓄能大狗 / 机机大狗重新抱起时通过真实附魔 API 重新添加附魔
		if (BigDogWolfUtil.getWeaponMode(wolf) == BigDogWeaponMode.MACHINE_GUN) {
			BigDogEnchantmentUtil.applyMachineGun(stack, wolf.getRegistryManager());
		} else if (BigDogWolfUtil.getWeaponMode(wolf) == BigDogWeaponMode.CHARGE) {
			BigDogEnchantmentUtil.applyCharge(stack, wolf.getRegistryManager());
		}
		return stack;
	}

	/** 数据是否完整有效(版本、主人、狼数据、IsBigDog、完全成长)。 */
	public static boolean hasValidData(ItemStack stack) {
		NbtCompound root = getRoot(stack);
		if (root == null || root.getInt(VERSION_KEY) != DATA_VERSION) {
			return false;
		}
		if (!root.containsUuid(OWNER_UUID_KEY)) {
			return false;
		}
		NbtCompound wolfData = root.getCompound(WOLF_DATA_KEY);
		if (wolfData.isEmpty() || !wolfData.getBoolean("BigDogBark.IsBigDog")) {
			return false;
		}
		return wolfData.getInt("BigDogBark.GrowthPoints") >= BigDogGrowth.MAX_GROWTH_POINTS;
	}

	public static Optional<UUID> getOwnerUuid(ItemStack stack) {
		NbtCompound root = getRoot(stack);
		if (root == null || !root.containsUuid(OWNER_UUID_KEY)) {
			return Optional.empty();
		}
		return Optional.of(root.getUuid(OWNER_UUID_KEY));
	}

	public static String getOwnerName(ItemStack stack) {
		NbtCompound root = getRoot(stack);
		return root == null ? "" : root.getString(OWNER_NAME_KEY);
	}

	public static NbtCompound getWolfData(ItemStack stack) {
		NbtCompound root = getRoot(stack);
		return root == null ? new NbtCompound() : root.getCompound(WOLF_DATA_KEY);
	}

	/**
	 * 将物品中的狼数据恢复到目标狼实体。
	 * 返回 false 表示数据无效(不恢复)。内部验证 IsBigDog 与完全成长,
	 * readNbt 会触发 Mixin 的 readCustomDataFromNbt,自动按成长进度应用缩放。
	 */
	public static boolean restoreWolfData(ItemStack stack, WolfEntity wolf) {
		NbtCompound root = getRoot(stack);
		if (root == null || root.getInt(VERSION_KEY) != DATA_VERSION) {
			return false;
		}
		NbtCompound wolfData = root.getCompound(WOLF_DATA_KEY);
		if (wolfData.isEmpty() || !wolfData.getBoolean("BigDogBark.IsBigDog")) {
			return false;
		}
		if (wolfData.getInt("BigDogBark.GrowthPoints") < BigDogGrowth.MAX_GROWTH_POINTS) {
			return false;
		}
		wolf.readNbt(wolfData);
		return BigDogWolfUtil.isBigDog(wolf)
				&& BigDogWolfUtil.getGrowthPoints(wolf) >= BigDogGrowth.MAX_GROWTH_POINTS;
	}

	/** 快照中保存的当前生命值(Tooltip 用)。 */
	public static float getSavedHealth(ItemStack stack) {
		return getWolfData(stack).getFloat("Health");
	}

	/** 快照中保存的最大生命值(Tooltip 用,从 attributes 解析,兜底 40)。 */
	public static float getSavedMaxHealth(ItemStack stack) {
		NbtCompound wolfData = getWolfData(stack);
		NbtList attributes = wolfData.getList("attributes", NbtElement.COMPOUND_TYPE);
		for (NbtElement element : attributes) {
			NbtCompound attribute = (NbtCompound) element;
			if ("minecraft:generic.max_health".equals(attribute.getString("id"))) {
				return attribute.getFloat("base");
			}
		}
		return 40.0F;
	}

	private static NbtCompound getRoot(ItemStack stack) {
		NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (component == null || component.isEmpty()) {
			return null;
		}
		NbtCompound nbt = component.copyNbt();
		return nbt.contains(ROOT_KEY) ? nbt.getCompound(ROOT_KEY) : null;
	}
}
