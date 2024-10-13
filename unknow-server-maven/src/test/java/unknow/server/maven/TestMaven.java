package unknow.server.maven;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.protostuff.JsonXInput;

public class TestMaven {
	private static final Pattern pa = Pattern.compile("\\{\\s*(\\w[\\w\\.\\-]*)\\s*(?::\\s*((?:[^\\{\\}]|\\{[^\\{\\}]*\\})*)\\s*)?\\}");

	public static void main(String[] arg) {
	

		String path = "/{q}/list/{t}";

		StringBuilder sb = new StringBuilder();
		List<String> parts = new ArrayList<>();

		Matcher m = pa.matcher(path);
		int i = 1;
		int l = 0;
		boolean last = true;
		while (m.find()) {
			l += m.start() - i;
			String s = path.substring(i, m.start());
			sb.append('/').append(s.replaceAll("([\\\\.+*\\[\\{])", "\\$1"));
			if (parts != null && !s.isEmpty())
				parts.add(s);
//			map.put(m.group(1), map.size());
			if (m.group(2) != null) {
				sb.append('(').append(m.group(2)).append(')');
				parts = null;
			} else
				sb.append("([^/]+)");
			i = m.end() + 1;
		}
		if (i < path.length()) {
			l += path.length() - i;
			sb.append('/').append(path.substring(i));
			if (parts != null) {
				last = false;
				parts.add(path.substring(i));
			}
		}

		System.out.println("Parts: " + parts + ", last: " + last + ", pattern: " + sb);
	}
}
