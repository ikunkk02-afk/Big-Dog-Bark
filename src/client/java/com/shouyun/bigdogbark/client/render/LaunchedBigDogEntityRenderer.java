package com.shouyun.bigdogbark.client.render;

import com.shouyun.bigdogbark.entity.LaunchedBigDogEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * “发射的大狗”声波发射体渲染器。
 *
 * <p>声波实体本身<b>隐形</b>:视觉主体是服务端每 tick 在波前生成的
 * {@code sonic_boom} 粒子(原版坚守者声波同款粒子,扩散成环,连续生成形成
 * 移动的波环/激光通道)。本渲染器只占位,防止客户端因缺少渲染器而崩溃。
 *
 * <p>纯客户端类(src/client),独立服务端不加载。
 */
public final class LaunchedBigDogEntityRenderer extends EntityRenderer<LaunchedBigDogEntity> {

	public LaunchedBigDogEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(LaunchedBigDogEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		// 实体隐形:不渲染任何模型(粒子由服务端广播)
	}

	@Override
	public Identifier getTexture(LaunchedBigDogEntity entity) {
		return null;
	}
}
