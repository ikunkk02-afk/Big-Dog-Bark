package com.shouyun.bigdogbark.client.render;

import com.shouyun.bigdogbark.item.CarriedBigDogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;

/**
 * 物品/投射物共用的“预览狼”缓存。
 *
 * <p>carried_big_dog 的物品动态渲染与发射的大狗投射物渲染都复用<b>原版
 * {@code WolfEntityRenderer}</b>(WolfEntityModel + 变种纹理 + 项圈层),本类只负责
 * 根据物品中保存的 WolfData 恢复一只客户端预览狼并缓存(按栈内容 + 世界缓存,
 * 同一栈不会每帧重建实体)。这不是第二套狼模型实现。
 */
public final class CarriedBigDogRenderCache {

	private final boolean sitting;

	private ClientWorld cachedWorld;
	private ItemStack cachedStack = ItemStack.EMPTY;
	private WolfEntity previewWolf;

	public CarriedBigDogRenderCache(boolean sitting) {
		this.sitting = sitting;
	}

	/** 获取(或按需创建并缓存)该物品对应的预览狼。数据无效时也使用真实狼模型作安全预览。 */
	public WolfEntity getOrCreate(ItemStack stack, ClientWorld world) {
		if (previewWolf != null && cachedWorld == world && ItemStack.areEqual(cachedStack, stack)) {
			return previewWolf;
		}

		WolfEntity wolf = EntityType.WOLF.create(world);
		if (wolf == null) {
			return null;
		}

		if (!CarriedBigDogData.restoreWolfData(stack, wolf)) {
			// 无效测试物品也使用真正的狼模型作为安全预览,但绝不从其生成实体
			wolf.setTamed(true, false);
		}
		wolf.setSitting(sitting);
		wolf.setTarget(null);
		wolf.setCustomName(null);
		wolf.calculateDimensions();

		cachedWorld = world;
		cachedStack = stack.copy();
		previewWolf = wolf;
		return wolf;
	}
}
