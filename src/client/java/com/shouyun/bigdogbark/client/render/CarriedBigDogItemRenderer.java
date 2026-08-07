package com.shouyun.bigdogbark.client.render;

import com.shouyun.bigdogbark.item.CarriedBigDogData;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * 使用原版 {@link WolfEntity} 的实体渲染器绘制“抱起的大狗”物品。
 *
 * <p>这不是平面物品贴图或手写 JSON 方块模型：物品栏、手持、掉落状态均由
 * Minecraft 原版狼实体模型及其特性层（毛色、项圈等）实时渲染。物品 JSON
 * 仅负责声明 {@code builtin/entity} 和不同显示场景的基础变换。</p>
 */
public final class CarriedBigDogItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

	private ClientWorld cachedWorld;
	private ItemStack cachedStack = ItemStack.EMPTY;
	private WolfEntity previewWolf;

	@Override
	public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light, int overlay) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null) {
			return;
		}

		WolfEntity wolf = getOrCreatePreviewWolf(stack, world);
		if (wolf == null) {
			return;
		}

		matrices.push();
		// builtin/entity 的原点在物品模型空间左下角；将狼脚底移到中心并转向镜头。
		matrices.translate(0.5D, 0.02D, 0.5D);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		// 有效抱起物品保存的是 2 倍完全成长大狗；缩放后完整模型恰好装入物品空间。
		matrices.scale(0.45F, 0.45F, 0.45F);

		EntityRenderer<? super WolfEntity> renderer = client.getEntityRenderDispatcher().getRenderer(wolf);
		// 直接调用原版狼 EntityRenderer：包含实体模型、实际狼变种纹理和项圈渲染层，
		// 但不会绘制世界阴影、火焰或牵绳等与物品无关的效果。
		renderer.render(wolf, 0.0F, client.getRenderTickCounter().getTickDelta(true),
				matrices, vertexConsumers, light);
		matrices.pop();
	}

	private WolfEntity getOrCreatePreviewWolf(ItemStack stack, ClientWorld world) {
		if (previewWolf != null && cachedWorld == world && ItemStack.areEqual(cachedStack, stack)) {
			return previewWolf;
		}

		WolfEntity wolf = EntityType.WOLF.create(world);
		if (wolf == null) {
			return null;
		}

		if (!CarriedBigDogData.restoreWolfData(stack, wolf)) {
			// 无效测试物品也使用真正的狼模型作为安全预览，但绝不从其生成实体。
			wolf.setTamed(true, false);
		}
		wolf.setSitting(true);
		wolf.setTarget(null);
		wolf.setCustomName(null);
		wolf.calculateDimensions();

		cachedWorld = world;
		cachedStack = stack.copy();
		previewWolf = wolf;
		return wolf;
	}
}
