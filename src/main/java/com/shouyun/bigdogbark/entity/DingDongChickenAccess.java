package com.shouyun.bigdogbark.entity;

/**
 * 叮咚鸡实体扩展接口,由 {@code ChickenEntityMixin} 注入到原版 ChickenEntity。
 * 方法名带 {@code bigDogBark$} 前缀,避免与其他模组的接口方法冲突。
 */
public interface DingDongChickenAccess {

	boolean bigDogBark$isDingDongChicken();

	void bigDogBark$setDingDongChicken(boolean value);
}
