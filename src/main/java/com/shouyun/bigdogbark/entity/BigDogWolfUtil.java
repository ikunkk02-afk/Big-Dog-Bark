package com.shouyun.bigdogbark.entity;

import net.minecraft.entity.passive.WolfEntity;

/**
 * 特殊大狗的通用检查/标记/成长入口。
 * 后续的成长、抱起等系统一律通过本类访问,不得直接读取 Mixin 字段或强制转换接口。
 */
public final class BigDogWolfUtil {

	private BigDogWolfUtil() {
	}

	public static boolean isBigDog(WolfEntity wolf) {
		return wolf instanceof BigDogWolfAccess access && access.bigDogBark$isBigDog();
	}

	public static void markBigDog(WolfEntity wolf) {
		if (wolf instanceof BigDogWolfAccess access) {
			access.bigDogBark$setBigDog(true);
		}
	}

	public static int getGrowthPoints(WolfEntity wolf) {
		return wolf instanceof BigDogWolfAccess access ? access.bigDogBark$getGrowthPoints() : 0;
	}

	public static void setGrowthPoints(WolfEntity wolf, int points) {
		if (wolf instanceof BigDogWolfAccess access) {
			access.bigDogBark$setGrowthPoints(points);
		}
	}

	/** 增加 1 点成长进度(不超过最大进度),返回增加后的进度。 */
	public static int addGrowthPoint(WolfEntity wolf) {
		int next = Math.min(getGrowthPoints(wolf) + 1, BigDogGrowth.MAX_GROWTH_POINTS);
		setGrowthPoints(wolf, next);
		return next;
	}

	public static int getGrowthStage(WolfEntity wolf) {
		return BigDogGrowth.stageFor(getGrowthPoints(wolf));
	}

	/** 当前武器模式(0 = 无,1 = 蓄能),见 {@link BigDogWeaponMode}。 */
	public static int getWeaponMode(WolfEntity wolf) {
		return wolf instanceof BigDogWolfAccess access ? access.bigDogBark$getWeaponMode() : BigDogWeaponMode.NONE;
	}

	/** 设置武器模式(实现方负责限制在合法范围)。 */
	public static void setWeaponMode(WolfEntity wolf, int weaponMode) {
		if (wolf instanceof BigDogWolfAccess access) {
			access.bigDogBark$setWeaponMode(weaponMode);
		}
	}

	/**
	 * 是否已长到最大:必须是特殊大狗、进度达到 12、阶段为 4。
	 * 该方法供下一阶段“下蹲右键抱起大狗”系统调用。
	 */
	public static boolean isFullyGrown(WolfEntity wolf) {
		return isBigDog(wolf)
				&& getGrowthPoints(wolf) >= BigDogGrowth.MAX_GROWTH_POINTS
				&& getGrowthStage(wolf) == BigDogGrowth.MAX_GROWTH_STAGE;
	}

	public static double getGrowthScale(WolfEntity wolf) {
		return BigDogGrowth.scaleForStage(getGrowthStage(wolf));
	}

	public static void applyGrowthScale(WolfEntity wolf) {
		BigDogGrowth.applyGrowthScale(wolf);
	}
}
