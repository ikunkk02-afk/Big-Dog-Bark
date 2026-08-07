package com.shouyun.bigdogbark.network;

import com.shouyun.bigdogbark.BigDogBark;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** 机枪满蓄能/能量耗尽自动停止后，客户端检测到真实右键松开时发送的握手。 */
public record MachineGunUseKeyReleasedPayload() implements CustomPayload {

	public static final Id<MachineGunUseKeyReleasedPayload> ID =
			new Id<>(BigDogBark.id("machine_gun_use_key_released"));
	public static final PacketCodec<RegistryByteBuf, MachineGunUseKeyReleasedPayload> CODEC =
			PacketCodec.unit(new MachineGunUseKeyReleasedPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
