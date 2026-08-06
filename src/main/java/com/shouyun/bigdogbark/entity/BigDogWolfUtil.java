package com.shouyun.bigdogbark.entity;

import net.minecraft.entity.passive.WolfEntity;

/**
 * 特殊大狗的通用检查/标记入口。
 * 后续的成长、武器等系统一律通过本类访问,不得直接读取 Mixin 字段。
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
}
