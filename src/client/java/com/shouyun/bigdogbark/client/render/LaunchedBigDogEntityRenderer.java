package com.shouyun.bigdogbark.client.render;

import com.shouyun.bigdogbark.entity.LaunchedBigDogEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * “发射的大狗”投射物渲染器。
 *
 * <p>直接复用原版 {@code WolfEntityRenderer} 渲染投射物内部保存的
 * carried_big_dog ItemStack(通过共享的 {@link CarriedBigDogRenderCache} 恢复预览狼),
 * 不复制第二套狼模型代码;飞行时狗头朝向速度方向(基于投射物 yaw/pitch,
 * 与 {@code LivingEntityRenderer} 的 180 - yaw 约定对齐),不高速自转。
 *
 * <p>纯客户端类(src/client),独立服务端不加载。
 */
public final class LaunchedBigDogEntityRenderer extends EntityRenderer<LaunchedBigDogEntity> {

	private final CarriedBigDogRenderCache previewCache = new CarriedBigDogRenderCache(false);

	public LaunchedBigDogEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(LaunchedBigDogEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		ItemStack stack = entity.getCarriedStack();
		if (stack.isEmpty()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null) {
			return;
		}
		WolfEntity wolf = previewCache.getOrCreate(stack, world);
		if (wolf == null) {
			return;
		}
		EntityRenderer<? super WolfEntity> renderer = client.getEntityRenderDispatcher().getRenderer(wolf);

		matrices.push();
		// 与 LivingEntityRenderer 的 180 - yaw 约定对齐:预览狼 yaw=0,补 -entity.yaw 即朝向飞行方向
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch()));
		renderer.render(wolf, 0.0F, tickDelta, matrices, vertexConsumers, light);
		matrices.pop();
	}

	@Override
	public Identifier getTexture(LaunchedBigDogEntity entity) {
		// 实际纹理由 WolfEntityRenderer 内部按狼变种解析,此处不需要
		return null;
	}
}
