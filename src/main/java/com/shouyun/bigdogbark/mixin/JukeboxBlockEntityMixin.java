package com.shouyun.bigdogbark.mixin;

import com.shouyun.bigdogbark.BigDogBark;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * 追踪正在播放大狗召唤唱片的唱片机位置,供吸引逻辑使用。
 */
@Mixin(JukeboxBlockEntity.class)
public class JukeboxBlockEntityMixin {

	/** 全局活跃的大狗召唤唱片机位置集合(服务端)。 */
	public static final Set<BlockPos> ACTIVE_BIG_DOG_CALLING_JUKEBOXES = new HashSet<>();

	@Inject(method = "setDisc", at = @At("TAIL"))
	private void onSetDisc(ItemStack stack, CallbackInfo ci) {
		// 仅服务端追踪
		BlockEntity self = (BlockEntity) (Object) this;
		if (self.getWorld() == null || self.getWorld().isClient) {
			return;
		}
		if (stack.isOf(BigDogBarkItems.MUSIC_DISC_BIG_DOG_CALLING)) {
			ACTIVE_BIG_DOG_CALLING_JUKEBOXES.add(self.getPos());
			BigDogBark.LOGGER.debug("Big dog calling disc inserted at {}", self.getPos());
		} else {
			ACTIVE_BIG_DOG_CALLING_JUKEBOXES.remove(self.getPos());
		}
	}
}
