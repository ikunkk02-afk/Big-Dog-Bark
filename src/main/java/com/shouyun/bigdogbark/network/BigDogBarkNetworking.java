package com.shouyun.bigdogbark.network;

import com.shouyun.bigdogbark.item.CarriedBigDogItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** 注册机枪两阶段输入所需的最小 C2S 释放握手。 */
public final class BigDogBarkNetworking {

	private BigDogBarkNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(
				MachineGunUseKeyReleasedPayload.ID,
				MachineGunUseKeyReleasedPayload.CODEC
		);
		ServerPlayNetworking.registerGlobalReceiver(
				MachineGunUseKeyReleasedPayload.ID,
				(payload, context) -> CarriedBigDogItem.handleServerMachineGunUseKeyReleased(context.player())
		);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				CarriedBigDogItem.clearServerMachineGunState(handler.player));
	}
}
