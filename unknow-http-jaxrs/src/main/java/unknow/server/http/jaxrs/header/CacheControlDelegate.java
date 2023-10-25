/**
 * 
 */
package unknow.server.http.jaxrs.header;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 * @author unknow
 */
public class CacheControlDelegate implements HeaderDelegate<CacheControl> {

	public static final CacheControlDelegate INSTANCE = new CacheControlDelegate();

	private static final Pattern COMPLEX_HEADER_PATTERN = Pattern.compile("(([\\w-]+=\"[^\"]*\")|([\\w-]+=[\\w]+)|([\\w-]+))");
	private static final String PUBLIC = "public";
	private static final String PRIVATE = "private";
	private static final String NO_CACHE = "no-cache";
	private static final String NO_STORE = "no-store";
	private static final String NO_TRANSFORM = "no-transform";
	private static final String MUST_REVALIDATE = "must-revalidate";
	private static final String PROXY_REVALIDATE = "proxy-revalidate";
	private static final String MAX_AGE = "max-age";
	private static final String SMAX_AGE = "s-maxage";

	private CacheControlDelegate() {
	}

	@Override
	public CacheControl fromString(String value) {
		Iterator<String> it = value.indexOf('"') != -1 ? new MatcherIt(COMPLEX_HEADER_PATTERN.matcher(value)) : new ArrayIt<>(value.split(","));
		CacheControl cc = new CacheControl();
		cc.setNoTransform(false);

		List<String> privateFields = cc.getPrivateFields();
		List<String> noCacheFields = cc.getNoCacheFields();
		Map<String, String> extensions = cc.getCacheExtension();

		while (it.hasNext()) {
			String token = it.next().trim();
			if (token.startsWith(MAX_AGE)) {
				cc.setMaxAge(Integer.parseInt(token.substring(MAX_AGE.length() + 1)));
			} else if (token.startsWith(SMAX_AGE)) {
				cc.setSMaxAge(Integer.parseInt(token.substring(SMAX_AGE.length() + 1)));
			} else if (token.startsWith(PUBLIC)) {
				// ignore
			} else if (token.startsWith(NO_STORE)) {
				cc.setNoStore(true);
			} else if (token.startsWith(NO_TRANSFORM)) {
				cc.setNoTransform(true);
			} else if (token.startsWith(MUST_REVALIDATE)) {
				cc.setMustRevalidate(true);
			} else if (token.startsWith(PROXY_REVALIDATE)) {
				cc.setProxyRevalidate(true);
			} else if (token.startsWith(PRIVATE)) {
				cc.setPrivate(true);
				addFields(privateFields, token);
			} else if (token.startsWith(NO_CACHE)) {
				cc.setNoCache(true);
				addFields(noCacheFields, token);
			} else {
				String[] extPair = token.split("=");
				String v = extPair.length == 2 ? extPair[1] : "";
				extensions.put(extPair[0], v);
			}
		}

		return cc;
	}

	@Override
	public String toString(CacheControl c) {
		StringBuilder sb = new StringBuilder();
		if (c.isPrivate()) {
			sb.append(PRIVATE);
			appendFields(sb, c.getPrivateFields());
			sb.append(',');
		}
		if (c.isNoCache()) {
			sb.append(NO_CACHE);
			appendFields(sb, c.getNoCacheFields());
			sb.append(',');
		}
		if (c.isNoStore())
			sb.append(NO_STORE).append(',');
		if (c.isNoTransform())
			sb.append(NO_TRANSFORM).append(',');
		if (c.isMustRevalidate())
			sb.append(MUST_REVALIDATE).append(',');
		if (c.isProxyRevalidate())
			sb.append(PROXY_REVALIDATE).append(',');
		if (c.getMaxAge() != -1)
			sb.append(MAX_AGE).append('=').append(c.getMaxAge()).append(',');
		if (c.getSMaxAge() != -1)
			sb.append(SMAX_AGE).append('=').append(c.getSMaxAge()).append(',');

		for (Map.Entry<String, String> entry : c.getCacheExtension().entrySet()) {
			sb.append(entry.getKey());
			String v = entry.getValue();
			if (v != null) {
				sb.append('=');
				if (v.indexOf(' ') != -1)
					sb.append('\"').append(v).append('\"');
				else
					sb.append(v);
			}
			sb.append(',');
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	private static void addFields(List<String> fields, String token) {
		int i = token.indexOf('=');
		if (i == -1)
			return;

		String f = i == token.length() + 1 ? "" : token.substring(i + 1);
		if (f.length() < 2 || !f.startsWith("\"") || !f.endsWith("\""))
			return;

		f = f.length() == 2 ? "" : f.substring(1, f.length() - 1);
		for (String v : f.split(","))
			fields.add(v.trim());
	}

	private static void appendFields(StringBuilder sb, List<String> fields) {
		if (fields.isEmpty())
			return;

		sb.append("=\"");
		Iterator<String> it = fields.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			sb.append(',');
		}
		sb.setCharAt(sb.length() - 1, '"');
	}

	private static final class ArrayIt<T> implements Iterator<T> {
		private final T[] a;
		private int i;

		ArrayIt(T[] a) {
			this.a = a;
			this.i = 0;
		}

		@Override
		public boolean hasNext() {
			return i < a.length;
		}

		@Override
		public T next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return a[i++];
		}
	}

	private static final class MatcherIt implements Iterator<String> {
		private final Matcher m;
		private boolean next;

		MatcherIt(Matcher m) {
			this.m = m;
			this.next = m.find();
		}

		@Override
		public boolean hasNext() {
			return next;
		}

		@Override
		public String next() {
			if (!next)
				throw new NoSuchElementException();
			String s = m.group();
			next = m.find();
			return s;
		}
	}
}
