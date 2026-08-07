package com.shouyun.bigdogbark.client;

import com.shouyun.bigdogbark.client.hud.BigDogChargeHud;
import com.shouyun.bigdogbark.client.render.CarriedBigDogItemRenderer;
import com.shouyun.bigdogbark.client.render.LaunchedBigDogEntityRenderer;
import com.shouyun.bigdogbark.entity.BigDogBarkEntityTypes;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import com.shouyun.bigdogbark.item.CarriedBigDogItem;
import com.shouyun.bigdogbark.network.MachineGunUseKeyReleasedPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

		// 原版在 useKey 持续按住时会每 4 Tick 自动重试 doItemUse。
		// 机枪满蓄能/能量耗尽自动停止后，必须等真实松开右键，避免自动重试直接开火或重新蓄能。
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null
					|| client.options.useKey.isPressed()
					|| !CarriedBigDogItem.isClientMachineGunAwaitingRelease(client.player)) {
				return;
			}
			if (!ClientPlayNetworking.canSend(MachineGunUseKeyReleasedPayload.ID)) {
				CarriedBigDogItem.clearClientMachineGunState(client.player);
				return;
			}
			if (CarriedBigDogItem.handleClientMachineGunUseKeyReleased(client.player)) {
				ClientPlayNetworking.send(new MachineGunUseKeyReleasedPayload());
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (client.player != null) {
				CarriedBigDogItem.clearClientMachineGunState(client.player);
			}
		});
	}
}
