package com.shouyun.bigdogbark.entity;

/**
 * 大狗的武器模式(轻量永久状态,随狼实体 NBT 持久化)。
 *
 * <p>值定义:
 * <ul>
 *   <li>{@link #NONE} = 0:无武器模式(普通大狗);</li>
 *   <li>{@link #CHARGE} = 1:蓄能模式(抱起状态下物品带“蓄能 I”);</li>
 *   <li>下一阶段将扩展 {@code MACHINE_GUN} = 2。</li>
 * </ul>
 *
 * <p>设计原则:该状态只是“武器模式”标记,本身不能把普通狼变成特殊大狗
 * (特殊大狗身份仍由 {@code BigDogBark.IsBigDog} 决定)。
 */
public final class BigDogWeaponMode {

	/** 无武器模式。 */
	public static final int NONE = 0;

	/** 蓄能模式(carried_big_dog 带“蓄能 I”附魔)。 */
	public static final int CHARGE = 1;

	/** 机枪模式(carried_big_dog 带“机枪”附魔)。 */
	public static final int MACHINE_GUN = 2;

	/** 当前允许的最大值,读取非法 NBT 值时用于 clamp。 */
	public static final int MAX_VALUE = MACHINE_GUN;

	private BigDogWeaponMode() {
	}
}
