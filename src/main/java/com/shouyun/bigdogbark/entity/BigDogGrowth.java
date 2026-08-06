package com.shouyun.bigdogbark.entity;

import com.shouyun.bigdogbark.BigDogBark;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.Identifier;

/**
 * 特殊大狗成长系统核心:常量、阶段计算、Scale 计算与属性修饰符应用。
 *
 * <p>设计原则:
 * <ul>
 *   <li>成长进度 NBT({@code BigDogBark.GrowthPoints})是唯一事实来源,阶段只由进度推导,
 *       不保存可能冲突的“阶段”字段;</li>
 *   <li>实体缩放使用原版 {@code EntityAttributes.GENERIC_SCALE} 上的<b>临时属性修饰符</b>
 *       {@code big_dog_bark:growth_scale},每次应用先移除旧修饰符再添加,天然幂等,
 *       不会重复叠加,也不覆盖其他模组/命令对 scale 基础值的修改;</li>
 *   <li>修饰符不需要额外持久化:实体加载时根据 GrowthPoints 重新应用。</li>
 * </ul>
 */
public final class BigDogGrowth {

	/** 最大成长进度(达到后为阶段 4 武器大狗)。 */
	public static final int MAX_GROWTH_POINTS = 12;

	/** 每多少点成长进度提升一个阶段。 */
	public static final int POINTS_PER_STAGE = 3;

	/** 最大成长阶段。 */
	public static final int MAX_GROWTH_STAGE = 4;

	/** 每个阶段增加的 Scale 倍率步长(阶段 n 的倍率 = 1.0 + n * 0.25)。 */
	public static final double SCALE_STEP = 0.25D;

	/** 成长缩放修饰符 ID(带模组命名空间,避免与其他模组冲突)。 */
	public static final Identifier GROWTH_SCALE_MODIFIER_ID = BigDogBark.id("growth_scale");

	private BigDogGrowth() {
	}

	/** 由成长进度计算阶段:0～2→0,3～5→1,6～8→2,9～11→3,12→4。 */
	public static int stageFor(int growthPoints) {
		return Math.min(Math.max(growthPoints, 0) / POINTS_PER_STAGE, MAX_GROWTH_STAGE);
	}

	/** 由阶段计算倍率:阶段 0/1/2/3/4 → 1.00/1.25/1.50/1.75/2.00。 */
	public static double scaleForStage(int stage) {
		return 1.0D + SCALE_STEP * Math.max(0, Math.min(stage, MAX_GROWTH_STAGE));
	}

	/**
	 * 根据狼当前的成长进度应用 GENERIC_SCALE 临时修饰符。
	 * 幂等:先按模组 ID 移除旧修饰符,再按当前阶段添加(阶段 0 不添加)。
	 * 属性实例不可用(理论上的初始化边界)时安全跳过。
	 */
	public static void applyGrowthScale(WolfEntity wolf) {
		EntityAttributeInstance scale = wolf.getAttributes().getCustomInstance(EntityAttributes.GENERIC_SCALE);
		if (scale == null) {
			return;
		}
		int stage = stageFor(BigDogWolfUtil.getGrowthPoints(wolf));
		scale.removeModifier(GROWTH_SCALE_MODIFIER_ID);
		if (stage > 0) {
			// ADD_MULTIPLIED_BASE:value = base * (1 + amount),阶段 1 传 0.25 → 1.25 倍
			double amount = scaleForStage(stage) - 1.0D;
			scale.addTemporaryModifier(
					new EntityAttributeModifier(GROWTH_SCALE_MODIFIER_ID, amount,
							EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		// 立即按新尺寸重算碰撞箱/命中箱(渲染尺寸由 getDimensions 基于 scale 自动派生)
		wolf.calculateDimensions();
	}
}
