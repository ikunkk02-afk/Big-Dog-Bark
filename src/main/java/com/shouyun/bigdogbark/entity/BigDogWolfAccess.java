package com.shouyun.bigdogbark.entity;

/**
 * 特殊大狗实体扩展接口,由 {@code WolfEntityMixin} 注入到原版 WolfEntity。
 * 方法名带 {@code bigDogBark$} 前缀,避免与其他模组的接口方法冲突。
 */
public interface BigDogWolfAccess {

	boolean bigDogBark$isBigDog();

	void bigDogBark$setBigDog(boolean bigDog);

	/** 当前成长进度(0～{@link BigDogGrowth#MAX_GROWTH_POINTS}),普通狼默认 0。 */
	int bigDogBark$getGrowthPoints();

	/** 设置成长进度,实现方负责限制在 0～最大进度。 */
	void bigDogBark$setGrowthPoints(int growthPoints);

	/** 当前武器模式(0 = 无,1 = 蓄能,见 {@link BigDogWeaponMode}),普通狼默认 0。 */
	int bigDogBark$getWeaponMode();

	/** 设置武器模式,实现方负责限制在 0～{@link BigDogWeaponMode#MAX_VALUE}。 */
	void bigDogBark$setWeaponMode(int weaponMode);
}
