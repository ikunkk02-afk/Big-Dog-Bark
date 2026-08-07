package com.shouyun.bigdogbark.sound;

import com.shouyun.bigdogbark.BigDogBark;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * 模组音效注册入口。
 * 调用 {@link #register()} 触发静态字段初始化即完成注册,全部为公共注册表条目,
 * 服务端可安全加载(不引用任何客户端类)。
 */
public final class BigDogBarkSoundEvents {

	/** 叮咚鸡转化音效(附魔种子右键鸡时播放)。 */
	public static final SoundEvent DING_DONG_CHICKEN_CONVERT = register("ding_dong_chicken_convert");

	/** 叮咚鸡死亡音效(仅被标记的叮咚鸡播放,普通鸡保持原版死亡声)。 */
	public static final SoundEvent DING_DONG_CHICKEN_DEATH = register("ding_dong_chicken_death");

	/** 鸡的日常叫声(替换原版鸡 ambient 叫声,所有鸡生效)。 */
	public static final SoundEvent DING_DONG_CHICKEN_AMBIENT = register("ding_dong_chicken_ambient");

	/** 特殊大狗阶段成长音效(仅在成长阶段跨越时播放一次)。 */
	public static final SoundEvent DOG_GROWTH_STAGE = register("dog_growth_stage");

	/** 蓄能开始音效(普通右键开始蓄力时播放一次)。 */
	public static final SoundEvent DOG_CHARGE = register("dog_charge");

	/** 蓄能完毕提示音效(蓄力达到 40 Tick 时播放一次)。 */
	public static final SoundEvent DOG_CHARGE_READY = register("dog_charge_ready");

	/** 蓄能发射音效(松开右键发射大狗时播放)。 */
	public static final SoundEvent DOG_LAUNCH = register("dog_launch");

	private BigDogBarkSoundEvents() {
	}

	public static void register() {
		// 静态字段初始化即完成注册;此方法仅用于确保类在 onInitialize 中被加载
	}

	private static SoundEvent register(String path) {
		Identifier id = BigDogBark.id(path);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}
}
