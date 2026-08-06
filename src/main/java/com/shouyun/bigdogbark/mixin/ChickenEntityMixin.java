package com.shouyun.bigdogbark.mixin;

import com.shouyun.bigdogbark.entity.DingDongChickenAccess;
import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为原版鸡注入“叮咚鸡”布尔标记,并随实体 NBT 持久化:
 * 存档读写、服务端重启、跨维度传送均经过 writeCustomDataToNbt /
 * readCustomDataFromNbt,因此标记在以上场景全部保留,区块卸载不丢失。
 * NBT 键带模组前缀,避免与其他模组冲突。
 *
 * <p>同时只针对被标记的叮咚鸡替换原版死亡音效(getDeathSound),普通鸡继续使用
 * 原版 ENTITY_CHICKEN_DEATH。替换后原版死亡流程只播放一次自定义声音,
 * 不在死亡事件中额外播放,不会与原版死亡声重叠或重复。
 */
@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin implements DingDongChickenAccess {

	private static final String NBT_KEY = "BigDogBark.IsDingDongChicken";

	@Unique
	private boolean bigDogBark$isDingDongChicken;

	@Override
	public boolean bigDogBark$isDingDongChicken() {
		return this.bigDogBark$isDingDongChicken;
	}

	@Override
	public void bigDogBark$setDingDongChicken(boolean value) {
		this.bigDogBark$isDingDongChicken = value;
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void bigDogBark$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		if (this.bigDogBark$isDingDongChicken) {
			nbt.putBoolean(NBT_KEY, true);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void bigDogBark$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		this.bigDogBark$isDingDongChicken = nbt.getBoolean(NBT_KEY);
	}

	@Inject(method = "getDeathSound", at = @At("TAIL"), cancellable = true)
	private void bigDogBark$replaceDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
		if (this.bigDogBark$isDingDongChicken) {
			cir.setReturnValue(BigDogBarkSoundEvents.DING_DONG_CHICKEN_DEATH);
		}
	}
}
