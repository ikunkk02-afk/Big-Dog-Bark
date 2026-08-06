package com.shouyun.bigdogbark.entity;

/**
 * 特殊大狗实体扩展接口,由 {@code WolfEntityMixin} 注入到原版 WolfEntity。
 * 方法名带 {@code bigDogBark$} 前缀,避免与其他模组的接口方法冲突。
 */
public interface BigDogWolfAccess {

	boolean bigDogBark$isBigDog();

	void bigDogBark$setBigDog(boolean bigDog);
}
