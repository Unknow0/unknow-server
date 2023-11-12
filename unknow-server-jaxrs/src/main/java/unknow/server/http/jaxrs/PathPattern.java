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
