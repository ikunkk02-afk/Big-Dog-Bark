package com.shouyun.bigdogbark.client;

import com.shouyun.bigdogbark.client.render.CarriedBigDogItemRenderer;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;

public class BigDogBarkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BuiltinItemRendererRegistry.INSTANCE.register(
				BigDogBarkItems.CARRIED_BIG_DOG,
				new CarriedBigDogItemRenderer()
		);
	}
}
