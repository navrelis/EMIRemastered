package dev.emi.remastered.tools.tags;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One tag of one registry with the ids of its (already flattened) members. Pure data, no Minecraft classes, so the
 * search logic can be unit tested.
 */
public final class TagEntry {
	private final String registry;
	private final String id;
	private final List<String> members;
	private final String idLower;
	private final String pathLower;
	private volatile String[] membersLower;
	private volatile Set<String> memberSet;

	/**
	 * @param registry registry id, e.g. {@code minecraft:item}
	 * @param id       tag id without {@code #}, e.g. {@code minecraft:logs}
	 * @param members  ids of the members, e.g. {@code minecraft:oak_log}
	 */
	public TagEntry(String registry, String id, List<String> members) {
		this.registry = registry;
		this.id = id;
		this.members = List.copyOf(members);
		this.idLower = id.toLowerCase(Locale.ROOT);
		int colon = idLower.indexOf(':');
		this.pathLower = colon < 0 ? idLower : idLower.substring(colon + 1);
	}

	public String registry() {
		return registry;
	}

	public String id() {
		return id;
	}

	public List<String> members() {
		return members;
	}

	public int size() {
		return members.size();
	}

	String idLower() {
		return idLower;
	}

	String pathLower() {
		return pathLower;
	}

	boolean anyMemberContains(String lowerNeedle) {
		String[] lower = membersLower;
		if (lower == null) {
			lower = new String[members.size()];
			for (int i = 0; i < lower.length; i++) {
				lower[i] = members.get(i).toLowerCase(Locale.ROOT);
			}
			membersLower = lower;
		}
		for (String member : lower) {
			if (member.contains(lowerNeedle)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasMember(String memberId) {
		Set<String> set = memberSet;
		if (set == null) {
			set = new HashSet<>(members);
			memberSet = set;
		}
		return set.contains(memberId);
	}

	@Override
	public String toString() {
		return "#" + id + " [" + registry + ", " + members.size() + "]";
	}
}
