package dev.emi.remastered.mobs.data;

import java.util.ArrayList;
import java.util.List;

/**
 * The numbers shown on a mob page, read from the default attributes of the entity type. Attributes a mob does not
 * define are NaN and are not shown. Pure logic: the Minecraft side fills the record, the page formats it here.
 */
public record MobStats(double health, double armor, double toughness, double attackDamage, double movementSpeed,
		double knockbackResistance, double followRange, double width, double height, boolean fireImmune, String group) {

	public enum Stat {
		HEALTH("health"),
		ARMOR("armor"),
		TOUGHNESS("toughness"),
		ATTACK("attack"),
		SPEED("speed"),
		KNOCKBACK_RESISTANCE("knockback_resistance"),
		FOLLOW_RANGE("follow_range");

		private final String key;

		Stat(String key) {
			this.key = key;
		}

		/** Last part of the lang keys {@code emi_mobs.stat.<key>} and {@code emi_mobs.stat.<key>.tooltip}. */
		public String key() {
			return key;
		}
	}

	private double raw(Stat stat) {
		return switch (stat) {
			case HEALTH -> health;
			case ARMOR -> armor;
			case TOUGHNESS -> toughness;
			case ATTACK -> attackDamage;
			case SPEED -> movementSpeed;
			case KNOCKBACK_RESISTANCE -> knockbackResistance;
			case FOLLOW_RANGE -> followRange;
		};
	}

	/**
	 * The stats that are shown, in display order. Health is always there; armour, toughness and knockback
	 * resistance only when above zero (almost every mob has them at zero); the rest only when the mob defines them.
	 */
	public List<Stat> visible() {
		List<Stat> list = new ArrayList<>();
		for (Stat stat : Stat.values()) {
			double value = raw(stat);
			boolean show = switch (stat) {
				case HEALTH -> true;
				case ARMOR, TOUGHNESS, KNOCKBACK_RESISTANCE -> value > 0;
				default -> !Double.isNaN(value);
			};
			if (show) {
				list.add(stat);
			}
		}
		return list;
	}

	/** Short value for the page cell. With icons, health, attack and armour are in heart / armour icon units. */
	public String value(Stat stat, boolean icons) {
		double value = raw(stat);
		return switch (stat) {
			case HEALTH, ATTACK -> icons ? MobFormat.hearts(value) : MobFormat.number(value, 1);
			case ARMOR -> icons ? MobFormat.number(value / 2.0, 1) : MobFormat.number(value, 1);
			case TOUGHNESS -> MobFormat.number(value, 1);
			case SPEED -> MobFormat.blocksPerSecond(value);
			case KNOCKBACK_RESISTANCE -> MobFormat.number(value * 100.0, 0) + "%";
			case FOLLOW_RANGE -> MobFormat.number(value, 0);
		};
	}

	/** Arguments of the tooltip lang key of the stat. */
	public Object[] tooltipArgs(Stat stat) {
		double value = raw(stat);
		return switch (stat) {
			case HEALTH, ATTACK -> new Object[] {MobFormat.number(value, 2), MobFormat.hearts(value)};
			case ARMOR -> new Object[] {MobFormat.number(value, 2), MobFormat.number(value / 2.0, 2)};
			case TOUGHNESS, FOLLOW_RANGE -> new Object[] {MobFormat.number(value, 2)};
			case SPEED -> new Object[] {MobFormat.number(value, 3), MobFormat.blocksPerSecond(value)};
			case KNOCKBACK_RESISTANCE -> new Object[] {MobFormat.number(value * 100.0, 0)};
		};
	}

	public String sizeText() {
		return MobFormat.size(width, height);
	}
}
