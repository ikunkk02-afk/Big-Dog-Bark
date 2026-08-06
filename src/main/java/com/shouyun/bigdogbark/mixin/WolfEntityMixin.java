package com.shouyun.bigdogbark.mixin;

import com.shouyun.bigdogbark.entity.BigDogGrowth;
import com.shouyun.bigdogbark.entity.BigDogWolfAccess;
import com.shouyun.bigdogbark.entity.BigDogWolfUtil;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为原版狼注入“特殊大狗”布尔标记与成长进度,并随实体 NBT 持久化:
 * 存档读写、服务端重启、跨维度传送、区块卸载重载均经过 writeCustomDataToNbt /
 * readCustomDataFromNbt,因此数据在以上场景全部保留。
 * NBT 键带模组前缀,避免与其他模组冲突。
 *
 * <p>成长进度读取时强制限制在 0～{@link BigDogGrowth#MAX_GROWTH_POINTS},
 * 旧存档缺省为 0;读取完成后按进度重新应用缩放修饰符(移除+添加,幂等),
 * 加载多少次都不会重复叠加,客户端与服务端共用同一逻辑(属性包会同步临时修饰符)。
 */
@Mixin(WolfEntity.class)
public abstract class WolfEntityMixin implements BigDogWolfAccess {

	private static final String NBT_KEY = "BigDogBark.IsBigDog";
	private static final String GROWTH_NBT_KEY = "BigDogBark.GrowthPoints";

	@Unique
	private boolean bigDogBark$isBigDog;

	@Unique
	private int bigDogBark$growthPoints;

	@Override
	public boolean bigDogBark$isBigDog() {
		return this.bigDogBark$isBigDog;
	}

	@Override
	public void bigDogBark$setBigDog(boolean bigDog) {
		this.bigDogBark$isBigDog = bigDog;
	}

	@Override
	public int bigDogBark$getGrowthPoints() {
		return this.bigDogBark$growthPoints;
	}

	@Override
	public void bigDogBark$setGrowthPoints(int growthPoints) {
		this.bigDogBark$growthPoints = MathHelper.clamp(growthPoints, 0, BigDogGrowth.MAX_GROWTH_POINTS);
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void bigDogBark$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		if (this.bigDogBark$isBigDog) {
			nbt.putBoolean(NBT_KEY, true);
		}
		if (this.bigDogBark$growthPoints > 0) {
			nbt.putInt(GROWTH_NBT_KEY, this.bigDogBark$growthPoints);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void bigDogBark$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		this.bigDogBark$isBigDog = nbt.getBoolean(NBT_KEY);
		this.bigDogBark$growthPoints = MathHelper.clamp(nbt.getInt(GROWTH_NBT_KEY), 0,
				BigDogGrowth.MAX_GROWTH_POINTS);
		// 加载完成后按成长进度重新应用缩放(幂等:先移除旧修饰符再添加)
		BigDogWolfUtil.applyGrowthScale((WolfEntity) (Object) this);
	}
}
