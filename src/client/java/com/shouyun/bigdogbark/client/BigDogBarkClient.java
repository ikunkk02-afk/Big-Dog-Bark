package com.shouyun.bigdogbark.client;

import com.shouyun.bigdogbark.client.render.CarriedBigDogItemRenderer;
import com.shouyun.bigdogbark.client.render.LaunchedBigDogEntityRenderer;
import com.shouyun.bigdogbark.entity.BigDogBarkEntityTypes;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BigDogBarkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 第四阶段:抱起的大狗物品使用真实 WolfEntityModel 动态渲染
		BuiltinItemRendererRegistry.INSTANCE.register(
				BigDogBarkItems.CARRIED_BIG_DOG,
				new CarriedBigDogItemRenderer()
		);
		// 第五阶段:发射的大狗投射物渲染(复用原版狼实体渲染器)
		EntityRendererRegistry.register(
				BigDogBarkEntityTypes.LAUNCHED_BIG_DOG,
				LaunchedBigDogEntityRenderer::new
		);
	}
}
