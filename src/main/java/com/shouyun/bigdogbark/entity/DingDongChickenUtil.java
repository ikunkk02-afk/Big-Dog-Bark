package com.shouyun.bigdogbark.entity;

import net.minecraft.entity.passive.ChickenEntity;

/**
 * 叮咚鸡的通用检查/标记入口。
 * 后续系统一律通过本类访问,不得直接强制转换 Mixin 接口。
 */
public final class DingDongChickenUtil {

	private DingDongChickenUtil() {
	}

	public static boolean isDingDongChicken(ChickenEntity chicken) {
		return chicken instanceof DingDongChickenAccess access && access.bigDogBark$isDingDongChicken();
	}

	public static void markDingDongChicken(ChickenEntity chicken) {
		if (chicken instanceof DingDongChickenAccess access) {
			access.bigDogBark$setDingDongChicken(true);
		}
	}
}
