/**
 * 
 */
package unknow.server.http.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author unknow
 */
public interface PathPattern {

	List<String> process(String path);

	public static class PathSimple implements PathPattern {
		private final boolean last;
		private final String[] parts;

		public PathSimple(boolean last, String... parts) {
			this.parts = parts;
			this.last = last;
		}

		@Override
		public List<String> process(String path) {
			int o = 0;
			List<String> list = new ArrayList<>(parts.length + (last ? 1 : 0));

			for (int i = 0; i < parts.length; i++) {
				int e = path.indexOf('/', o);
				if (e < 0)
					return null;
				list.add(path.substring(o, e));
				o = e + 1;

				String s = parts[i];
				int l = s.length();
				if (!s.regionMatches(0, path, o, l))
					return null;
				o += l;
			}
			if (last) {
				if (o == path.length())
					return null;
				int e = path.indexOf('/', o);
				if (e > 0)
					return null;
				list.add(path.substring(o));
			} else if (o != path.length())
				return null;
			return list;
		}
	}

	public static class PathRegexp implements PathPattern {
		private final Pattern p;

		public PathRegexp(String p) {
			this.p = Pattern.compile(p);
		}

		@Override
		public List<String> process(String path) {
			Matcher m = p.matcher(path);
			if (!m.matches())
				return null;
			List<String> list = new ArrayList<>(m.groupCount());
			for (int i = 1; i <= m.groupCount(); i++)
				list.add(m.group(i));
			return list;
		}
	}
}
