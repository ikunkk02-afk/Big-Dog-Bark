package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.item.BigDogBarkItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

/**
 * 叮咚鸡死亡掉落处理。
 * 使用 Fabric API 的 {@link ServerLivingEntityEvents.ENTITY_DEATH}(仅服务端触发,
 * 客户端不会生成掉落物),每只叮咚鸡死亡时只回调一次,额外掉落 1 个叮咚鸡肉;
 * 普通鸡不受影响,原版羽毛与其他掉落全部保留,Looting 不增加叮咚鸡肉数量。
 *
 * <p>死亡音效不在此处播放:由 {@code ChickenEntityMixin} 替换 getDeathSound,
 * 在原版死亡流程中播放一次,与掉落逻辑分离且各自只执行一次。
 */
public final class DingDongChickenDeathHandler {

	private DingDongChickenDeathHandler() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ChickenEntity chicken && DingDongChickenUtil.isDingDongChicken(chicken)) {
				ServerWorld world = (ServerWorld) chicken.getWorld();
				world.spawnEntity(new ItemEntity(world, chicken.getX(), chicken.getY(), chicken.getZ(),
						new ItemStack(BigDogBarkItems.DING_DONG_CHICKEN_MEAT)));
			}
		});
	}
}
