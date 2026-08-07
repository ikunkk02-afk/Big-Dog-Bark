package com.shouyun.bigdogbark.mixin;

import com.shouyun.bigdogbark.BigDogBark;
import com.shouyun.bigdogbark.entity.BigDogCallingHandler;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 追踪正在播放大狗召唤唱片的唱片机位置,供吸引逻辑使用。
 */
@Mixin(JukeboxBlockEntity.class)
public class JukeboxBlockEntityMixin {

	@Inject(method = "setDisc", at = @At("TAIL"))
	private void onSetDisc(ItemStack stack, CallbackInfo ci) {
		BlockEntity self = (BlockEntity) (Object) this;
		if (self.getWorld() == null || self.getWorld().isClient) {
			return;
		}
		if (stack.isOf(BigDogBarkItems.MUSIC_DISC_BIG_DOG_CALLING)) {
			BigDogCallingHandler.ACTIVE_JUKEBOXES.add(self.getPos());
			BigDogBark.LOGGER.debug("Big dog calling disc inserted at {}", self.getPos());
		} else {
			BigDogCallingHandler.ACTIVE_JUKEBOXES.remove(self.getPos());
		}
	}
}
