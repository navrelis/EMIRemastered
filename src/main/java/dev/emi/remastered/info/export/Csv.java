package dev.emi.remastered.info.export;

import java.util.List;

/** CSV writing after RFC 4180: fields with commas, quotes, line breaks or edge spaces are quoted, quotes doubled. */
public final class Csv {
	/** RFC 4180 line break. */
	public static final String EOL = "\r\n";

	private Csv() {
	}

	public static String escape(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
			|| value.indexOf('\r') >= 0 || value.charAt(0) == ' ' || value.charAt(value.length() - 1) == ' ';
		return quote ? '"' + value.replace("\"", "\"\"") + '"' : value;
	}

	public static String row(List<String> cells) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cells.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(escape(cells.get(i)));
		}
		return sb.toString();
	}

	/** Header and rows, each line ended with {@link #EOL}. */
	public static String table(List<String> header, List<? extends List<String>> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append(row(header)).append(EOL);
		for (List<String> cells : rows) {
			sb.append(row(cells)).append(EOL);
		}
		return sb.toString();
	}
}
