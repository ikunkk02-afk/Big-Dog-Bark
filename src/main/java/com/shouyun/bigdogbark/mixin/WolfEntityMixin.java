package com.shouyun.bigdogbark.mixin;

import com.shouyun.bigdogbark.entity.BigDogWolfAccess;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.nbt.NbtCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为原版狼注入“特殊大狗”布尔标记,并随实体 NBT 持久化:
 * 存档读写、服务端重启、跨维度传送均经过 writeCustomDataToNbt /
 * readCustomDataFromNbt,因此标记在以上场景全部保留。
 * NBT 键带模组前缀,避免与其他模组冲突。
 */
@Mixin(WolfEntity.class)
public abstract class WolfEntityMixin implements BigDogWolfAccess {

	private static final String NBT_KEY = "BigDogBark.IsBigDog";

	@Unique
	private boolean bigDogBark$isBigDog;

	@Override
	public boolean bigDogBark$isBigDog() {
		return this.bigDogBark$isBigDog;
	}

	@Override
	public void bigDogBark$setBigDog(boolean bigDog) {
		this.bigDogBark$isBigDog = bigDog;
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void bigDogBark$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		if (this.bigDogBark$isBigDog) {
			nbt.putBoolean(NBT_KEY, true);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void bigDogBark$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		this.bigDogBark$isBigDog = nbt.getBoolean(NBT_KEY);
	}
}
