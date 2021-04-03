package team.catgirl.collar.api.entities;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/**
 * Up to game version 1.12, numerical IDs should be preferred in all cases
 * besides players, and lightning bolts due to them not having a numerical ID
 * although existing in the version.
 * <p>
 * From game version 1.13 and onwards, collar lexiographical ids corrospond with the
 * mojang internal namespace identifiers
 * <p>
 * Bobbers do not have an entity ID nor lexiographical although existing in 1.12
 * 
 */
public enum EntityType {
	/**
	 * spawned from lingering potions
	 */
	AREA_EFFECT_CLOUD("area_effect_cloud"),
	ARMOR_STAND("armor_stand"),
	ARROW("arrow"),
	AXOLOTL("axolotl"),
	BAT("bat"),
	BEE("bee"),
	BLAZE("blaze"),
	BOAT("boat"),
	CAT("cat"),
	CAVE_SPIDER("cave_spider"),
	CHEST_MINECART("chest_minecart"),
	/**
	 * diamond chickens in JE 2.0 hoax edition are just chickens internally
	 */
	CHICKEN("chicken"),
	COD("cod"),
	COMMAND_BLOCK_MINECART("command_block_minecart"),
	COW("cow"),
	CREEPER("creeper"),
	DOLPHIN("dolphin"),
	DONKEY("donkey"),
	DRAGON_FIREBALL("dragon_fireball"),
	DROWNED("drowned"),
	EGG("egg"),
	ELDER_GUARDIAN("elder_guardian"),
	ENDERMAN("enderman"),
	ENDERMITE("endermite"),
	ENDER_DRAGON("ender_dragon"),
	ENDER_PEARL("ender_pearl"),
	END_CRYSTAL("end_crystal"),
	EVOKER("evoker"),
	EVOKER_FANGS("evoker_fangs"),
	EXPERIENCE_BOTTLE("experience_bottle"),
	EXPERIENCE_ORB("experience_orb"),
	EYE_OF_ENDER("eye_of_ender"),
	FALLING_BLOCK("falling_block"),
	FIREBALL("fireball"),
	FIREWORK_ROCKET("firework_rocket"),
	FISHING_BOBBER("fishing_bobber"),
	FOX("fox"),
	FURNACE_MINECART("furnace_minecart"),
	GHAST("ghast"),
	GIANT("giant"),
	GLOW_SQUID("glow_squid"),
	GOAT("goat"),
	GUARDIAN("guardian"),
	HOGLIN("hoglin"),
	HOPPER_MINECART("hopper_minecart"),
	HORSE("horse"),
	/**
	 * Human 48 doesn't have AI and was removed in game version beta 1.2 while 49
	 * has hostile AI and was removed in game version beta 1.8
	 */
	HUMAN_MOB("Mob"),
	HUMAN_MONSTER("Monster"),
	HUSK("husk"),
	ILLUSIONER("illusioner"),
	IRON_GOLEM("iron_golem"),
	ITEM("item"),
	ITEM_FRAME("item_frame"),
	LEASH_KNOT("leash_knot"),
	LIGHTNING_BOLT("lightning_bolt"),
	LLAMA("llama"),
	LLAMA_SPIT("llama_spit"),
	MAGMA_CUBE("magma_cube"),
	MINECART("minecart"),
	MOOSHROOM("mooshroom"),
	MULE("mule"),
	OCELOT("ocelot"),
	PAINTING("painting"),
	PANDA("panda"),
	PARROT("parrot"),
	PHANTOM("phantom"),
	PIG("pig"),
	PIGLIN("piglin"),
	PIGLIN_BRUTE("piglin_brute"),
	PILLAGER("pillager"),
	PLAYER("player"),
	POLAR_BEAR("polar_bear"),
	POTION("potion"),
	PUFFERFISH("pufferfish"),
	RABBIT("rabbit"),
	RAVAGER("ravager"),
	SALMON("salmon"),
	SHEEP("sheep"),
	SHULKER("shulker"),
	SHULKER_BULLET("shulker_bullet"),
	/**
	 * redstone bugs in JE 2.0 hoax edition are just silverfish internally
	 */
	SILVERFISH("silverfish"),
	SKELETON("skeleton"),
	SKELETON_HORSE("skeleton_horse"),
	SLIME("slime"),
	SMALL_FIREBALL("small_fireball"),
	SNOWBALL("snowball"),
	SNOW_GOLEM("snow_golem"),
	SPAWNER_MINECART("spawner_minecart"),
	SPECTRAL_ARROW("spectral_arrow"),
	SPIDER("spider"),
	SQUID("squid"),
	STRAY("stray"),
	STRIDER("strider"),
	TNT("tnt"),
	TNT_MINECART("tnt_minecart"),
	TRADER_LLAMA("trader_llama"),
	TRIDENT("trident"),
	TROPICAL_FISH("tropical_fish"),
	TURTLE("turtle"),
	VEX("vex"),
	VILLAGER("villager"),
	VINDICATOR("vindicator"),
	WANDERING_TRADER("wandering_trader"),
	WITCH("witch"),
	/**
	 * pink wither in JE 2.0 hoax edition are just silverfish internally
	 */
	WITHER("wither"),
	WITHER_SKELETON("wither_skeleton"),
	WITHER_SKULL("wither_skull"),
	WOLF("wolf"),
	ZOGLIN("zoglin"),
	ZOMBIE("zombie"),
	ZOMBIE_HORSE("zombie_horse"),
	ZOMBIE_VILLAGER("zombie_villager"),
	ZOMBIFIED_PIGLIN("zombified_piglin"),
	
	@JsonEnumDefaultValue
	UNKNOWN("unknown_id");
	
	private final String identifier;

	EntityType(final String lexiographicalID) {
		this.identifier = lexiographicalID;
	}

	@Override
	public String toString() {
		return this.identifier;
	}
	
}
