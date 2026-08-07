package com.shouyun.bigdogbark.client;

import com.shouyun.bigdogbark.client.hud.BigDogChargeHud;
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
		// 第五阶段:发射的大狗声波发射体渲染(实体本身隐形,视觉主体是 sonic_boom 粒子)
		EntityRendererRegistry.register(
				BigDogBarkEntityTypes.LAUNCHED_BIG_DOG,
				LaunchedBigDogEntityRenderer::new
		);
		// 第五阶段:蓄能进度条 HUD(准心下方)
		BigDogChargeHud.register();
	}
}
