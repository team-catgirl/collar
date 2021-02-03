package team.catgirl.collar.api.entities;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/**
 * Up to game version 1.12, numerical IDs should be preferred in all cases
 * besides players, and lightning bolts due to them not having a numerical ID
 * although existing in the version.
 * <p>
 * From game version 1.13 and onwards, lexiographical IDs should be used due to
 * removal of numerical IDs in said versions
 * <p>
 * Bobbers do not have an entity ID nor lexiographical although existing in 1.12
 * 
 */
public enum EntityType {
	/**
	 * spawned from lingering potions
	 */
	AREA_EFFECT_CLOUD("area_effect_cloud", 3),
	ARMOR_STAND("armor_stand", 30),
	ARROW("arrow", 10),
	AXOLOTL("axolotl", EntityType.UNKNOWN_ID),
	BAT("bat", 65),
	BEE("bee", EntityType.UNKNOWN_ID),
	BLAZE("blaze", 61),
	BOAT("boat", 41),
	CAT("cat", EntityType.UNKNOWN_ID),
	CAVE_SPIDER("cave_spider", 59),
	CHEST_MINECART("chest_minecart", 43),
	/**
	 * diamond chickens in JE 2.0 hoax edition are just chickens internally
	 */
	CHICKEN("chicken", 93),
	COD("cod", EntityType.UNKNOWN_ID),
	COMMAND_BLOCK_MINECART("command_block_minecart", 40),
	COW("cow", 92),
	CREEPER("creeper", 50),
	DOLPHIN("dolphin", EntityType.UNKNOWN_ID),
	DONKEY("donkey", 31),
	DRAGON_FIREBALL("dragon_fireball", 26),
	DROWNED("drowned", EntityType.UNKNOWN_ID),
	EGG("egg", 7),
	ELDER_GUARDIAN("elder_guardian", 4),
	ENDERMAN("enderman", 58),
	ENDERMITE("endermite", 67),
	ENDER_DRAGON("ender_dragon", 63),
	ENDER_PEARL("ender_pearl", 14),
	END_CRYSTAL("end_crystal", 200),
	EVOKER("evoker", 34),
	EVOKER_FANGS("evoker_fangs", 33),
	EXPERIENCE_BOTTLE("experience_bottle", 17),
	EXPERIENCE_ORB("experience_orb", 2),
	EYE_OF_ENDER("eye_of_ender", 15),
	FALLING_BLOCK("falling_block", 21),
	FIREBALL("fireball", 12),
	FIREWORK_ROCKET("firework_rocket", 22),
	FISHING_BOBBER("fishing_bobber", EntityType.UNKNOWN_ID),
	FOX("fox", EntityType.UNKNOWN_ID),
	FURNACE_MINECART("furnace_minecart", 44),
	GHAST("ghast", 56),
	GIANT("giant", 53),
	GLOW_SQUID("glow_squid", EntityType.UNKNOWN_ID),
	GOAT("goat", EntityType.UNKNOWN_ID),
	GUARDIAN("guardian", 68),
	HOGLIN("hoglin", EntityType.UNKNOWN_ID),
	HOPPER_MINECART("hopper_minecart", 46),
	HORSE("horse", 100),
	/**
	 * Human 48 doesn't have AI and was removed in game version beta 1.2 while 49
	 * has hostile AI and was removed in game version beta 1.8
	 */
	HUMAN_MOB("Mob", 48),
	HUMAN_MONSTER("Monster", 49),
	HUSK("husk", 23),
	ILLUSIONER("illusioner", 37),
	IRON_GOLEM("iron_golem", 99),
	ITEM("item", 1),
	ITEM_FRAME("item_frame", 18),
	LEASH_KNOT("leash_knot", 8),
	LIGHTNING_BOLT("lightning_bolt", EntityType.UNKNOWN_ID),
	LLAMA("llama", 103), LLAMA_SPIT("llama_spit", 104),
	MAGMA_CUBE("magma_cube", 62),
	MINECART("minecart", 42),
	MOOSHROOM("mooshroom", 96),
	MULE("mule", 32),
	OCELOT("ocelot", 98),
	PAINTING("painting", 9),
	PANDA("panda", EntityType.UNKNOWN_ID),
	PARROT("parrot", 105),
	PHANTOM("phantom", EntityType.UNKNOWN_ID),
	PIG("pig", 90),
	PIGLIN("piglin", EntityType.UNKNOWN_ID),
	PIGLIN_BRUTE("piglin_brute", EntityType.UNKNOWN_ID),
	PILLAGER("pillager", EntityType.UNKNOWN_ID),
	PLAYER("player", EntityType.UNKNOWN_ID),
	POLAR_BEAR("polar_bear", 102),
	POTION("potion", 16),
	PUFFERFISH("pufferfish", EntityType.UNKNOWN_ID),
	RABBIT("rabbit", 101),
	RAVAGER("ravager", EntityType.UNKNOWN_ID),
	SALMON("salmon", EntityType.UNKNOWN_ID),
	SHEEP("sheep", 91),
	SHULKER("shulker", 69),
	SHULKER_BULLET("shulker_bullet", 25),
	/**
	 * redstone bugs in JE 2.0 hoax edition are just silverfish internally
	 */
	SILVERFISH("silverfish", 60),
	SKELETON("skeleton", 51),
	SKELETON_HORSE("skeleton_horse", 28),
	SLIME("slime", 55),
	SMALL_FIREBALL("small_fireball", 13),
	SNOWBALL("snowball", 11),
	SNOW_GOLEM("snow_golem", 97),
	SPAWNER_MINECART("spawner_minecart", 47),
	SPECTRAL_ARROW("spectral_arrow", 24),
	SPIDER("spider", 52),
	SQUID("squid", 94),
	STRAY("stray", 6),
	STRIDER("strider", EntityType.UNKNOWN_ID),
	TNT("tnt", 20),
	TNT_MINECART("tnt_minecart", 45),
	TRADER_LLAMA("trader_llama", EntityType.UNKNOWN_ID),
	TRIDENT("trident", EntityType.UNKNOWN_ID),
	TROPICAL_FISH("tropical_fish", EntityType.UNKNOWN_ID),
	TURTLE("turtle", EntityType.UNKNOWN_ID),
	VEX("vex", 35),
	VILLAGER("villager", 120),
	VINDICATOR("vindicator", 36),
	WANDERING_TRADER("wandering_trader", EntityType.UNKNOWN_ID),
	WITCH("witch", 66),
	WITHER("wither", 64),
	WITHER_SKELETON("wither_skeleton", 5),
	WITHER_SKULL("wither_skull", 19),
	WOLF("wolf", 95),
	ZOGLIN("zoglin", EntityType.UNKNOWN_ID),
	ZOMBIE("zombie", 54),
	ZOMBIE_HORSE("zombie_horse", 29),
	ZOMBIE_VILLAGER("zombie_villager", 27),
	ZOMBIFIED_PIGLIN("zombified_piglin", 57),

  @JsonEnumDefaultValue
  UNKNOWN("unknown_id", EntityType.UNKNOWN_ID);

	private static final int UNKNOWN_ID = Integer.MAX_VALUE - 1;
	private final String lexiographicalID;
	private final int numericalID;

	EntityType(final String lexiographicalID, final int numericalID) {
		this.lexiographicalID = lexiographicalID;
		this.numericalID = numericalID;
	}

	public final String getLexiographicalID() {
		return lexiographicalID;
	}

	public final int getNumericalID() {
		return numericalID;
	}
	
}
