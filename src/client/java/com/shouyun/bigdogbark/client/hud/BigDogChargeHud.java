package com.shouyun.bigdogbark.client.hud;

import com.shouyun.bigdogbark.enchantment.BigDogEnchantmentUtil;
import com.shouyun.bigdogbark.item.BigDogBarkItems;
import com.shouyun.bigdogbark.item.CarriedBigDogItem;
import com.shouyun.bigdogbark.sound.BigDogBarkSoundEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * 蓄能进度条(HUD):手持带“蓄能 I”的抱起大狗并按住右键蓄能时,
 * 在准心下方显示一条细进度条(比准心略小),玩家不用猜蓄能到了哪一档。
 *
 * <ul>
 *   <li>绿色 → 蓄能完成(40 Tick);黄色 → 危险过载区间(70 Tick);红色 → 即将炸膛;</li>
 *   <li>进度 = 已蓄能 Tick / 90(炸膛阈值);</li>
 *   <li>炸膛进入冷却后进度条立即隐藏(冷却客户端同步);</li>
 *   <li>同时负责停止蓄能音效:蓄能结束(松开/发射/炸膛冷却)时调用
 *       {@code stopSounds} 停止跟随音效,避免音效残留在原地或一直响。</li>
 * </ul>
 */
public final class BigDogChargeHud {

	/** 进度条宽度(像素),比准心略小。 */
	private static final int BAR_WIDTH = 72;

	/** 进度条高度(像素)。 */
	private static final int BAR_HEIGHT = 5;

	/** 进度条与准心的间距(像素)。 */
	private static final int BAR_OFFSET = 8;

	/** 上一帧是否正在显示进度条(用于检测蓄能结束并停止音效)。 */
	private static boolean wasShowing;

	private BigDogChargeHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(BigDogChargeHud::render);
	}

	private static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;
		boolean showing = false;
		if (player != null && player.isUsingItem()) {
			ItemStack stack = player.getActiveItem();
			// 仅当手持带“蓄能 I”的抱起大狗且不在物品冷却(炸膛/发射后)时显示
			if (stack.isOf(BigDogBarkItems.CARRIED_BIG_DOG)
					&& BigDogEnchantmentUtil.hasChargeInComponents(stack)
					&& !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
				showing = true;
				int elapsed = CarriedBigDogItem.MAX_USE_TIME - player.getItemUseTimeLeft();
				float progress = MathHelper.clamp(
						elapsed / (float) CarriedBigDogItem.OVERCHARGE_TICKS, 0.0F, 1.0F);
				int width = context.getScaledWindowWidth();
				int height = context.getScaledWindowHeight();
				int x = (width - BAR_WIDTH) / 2;
				int y = height / 2 + BAR_OFFSET;
				// 背景(半透明黑框)
				context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xAA000000);
				// 进度填充
				int fill = (int) (BAR_WIDTH * progress);
				if (fill > 0) {
					context.fill(x, y, x + fill, y + BAR_HEIGHT, colorFor(elapsed));
				}
			}
		}
		// 蓄能结束(松开/发射/炸膛):停止跟随蓄能音效,防止音效残留
		if (wasShowing && !showing) {
			client.getSoundManager().stopSounds(BigDogBarkSoundEvents.DOG_CHARGE.getId(), null);
		}
		wasShowing = showing;
	}

	/** 分段颜色:0～39 绿,40～69 黄,70+ 红。 */
	private static int colorFor(int elapsed) {
		if (elapsed >= CarriedBigDogItem.DANGER_CHARGE_TICKS) {
			return 0xFFFF5555;
		}
		if (elapsed >= CarriedBigDogItem.READY_CHARGE_TICKS) {
			return 0xFFFFAA00;
		}
		return 0xFF55FF55;
	}
}
