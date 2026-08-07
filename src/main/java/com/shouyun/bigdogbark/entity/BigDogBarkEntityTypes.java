package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.BigDogBark;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * 模组实体类型注册入口。
 * 当前仅注册“发射的大狗”投射物 {@code big_dog_bark:launched_big_dog}。
 */
public final class BigDogBarkEntityTypes {

	/**
	 * 大狗炮投射物:飞行中携带完整 carried_big_dog ItemStack,
	 * 命中/超时/掉出世界后掉落物品并消失,服务端权威。
	 */
	public static final EntityType<LaunchedBigDogEntity> LAUNCHED_BIG_DOG = Registry.register(
			Registries.ENTITY_TYPE, BigDogBark.id("launched_big_dog"),
			EntityType.Builder.create(LaunchedBigDogEntity::new, SpawnGroup.MISC)
					// 碰撞箱按大狗炮视觉大小(约 1.2 格)设置,命中判定更合理
					.dimensions(1.2F, 1.2F)
					.maxTrackingRange(8)
					.trackingTickInterval(2)
					.build("launched_big_dog"));

	private BigDogBarkEntityTypes() {
	}

	/** 触发静态字段初始化完成注册;在 {@code BigDogBark#onInitialize} 中调用。 */
	public static void register() {
	}
}
