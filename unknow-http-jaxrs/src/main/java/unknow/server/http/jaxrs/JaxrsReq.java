/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ParamConverter;
import unknow.server.http.jaxrs.header.MediaTypeDelegate;

/**
 * @author unknow
 */
public class JaxrsReq {
	private static final Object[] EMPTY = {};

	private final HttpServletRequest r;

	private MediaType accept;

	private Map<String, String> paths;
	private MultivaluedMap<String, String> queries;
	private MultivaluedMap<String, String> headers;
	private MultivaluedMap<String, String> forms;
	private MultivaluedMap<String, String> cookies;
	private MultivaluedMap<String, String> matrix;

	/**
	 * create new JaxrsReq
	 * 
	 * @param r
	 */
	public JaxrsReq(HttpServletRequest r) {
		this.r = r;
	}

	public HttpServletRequest getRequest() {
		return r;
	}

	public MediaType getAccept() {
		return accept;
	}

	public MediaType getAccepted(Predicate<MediaType> allowed) {
		String a = r.getHeader("accept");
		if (a == null) {
			if (allowed.test(MediaType.WILDCARD_TYPE))
				return accept = MediaType.WILDCARD_TYPE;
			return null;
		}
		int i, l = 0;
		double lq = -1;
		do {
			i = a.indexOf(',', l);
			MediaType m = MediaTypeDelegate.fromString(a, l, i < 0 ? a.length() : i);
			if (allowed.test(m)) {
				double q = Double.parseDouble(m.getParameters().getOrDefault("q", "1"));
				if (q > lq) {
					lq = q;
					this.accept = m;
				}
			}
			l = i + 1;
		} while (i > 0);
		return accept;
	}

	public <T> T getPath(String path, String def, ParamConverter<T> c) {
		return toValue(paths.get(path), def, c);
	}

	@SuppressWarnings("unchecked")
	public <T> T[] getPathArray(String path, String def, ParamConverter<T> c) {
		String s = paths.getOrDefault(path, def);
		if (s == null)
			return (T[]) EMPTY;
		return (T[]) new Object[] { c.fromString(s) };
	}

	public MultivaluedMap<String, String> getHeaders() {
		initHeaders();
		return headers;
	}

	public <T> T getHeader(String name, String def, ParamConverter<T> c) {
		initHeaders();
		return toValue(headers.getFirst(name), def, c);
	}

	public <T> T[] getHeaderArray(String name, String def, ParamConverter<T> c) {
		initHeaders();
		return toArray(headers.get(name), def, c);
	}

	public <T> T getQuery(String name, String def, ParamConverter<T> c) {
		initQueries();
		return toValue(queries.getFirst(name), def, c);
	}

	public <T> T[] getQueryArray(String name, String def, ParamConverter<T> c) {
		initQueries();
		return toArray(queries.get(name), def, c);
	}

	public <T> T getForm(String name, String def, ParamConverter<T> c) throws IOException {
		initForms();
		return toValue(forms.getFirst(name), def, c);
	}

	public <T> T[] getFormArray(String name, String def, ParamConverter<T> c) throws IOException {
		initForms();
		return toArray(forms.get(name), def, c);
	}

	public <T> T getCookie(String name, String def, ParamConverter<T> c) {
		initCookies();
		return toValue(cookies.getFirst(name), def, c);
	}

	public <T> T[] getCookieArray(String name, String def, ParamConverter<T> c) {
		initCookies();
		return toArray(cookies.get(name), def, c);
	}

	public <T> T getMatrix(String name, String def, ParamConverter<T> c) {
		initMatrix();
		return toValue(matrix.getFirst(name), def, c);
	}

	public <T> T[] getMatrixArray(String name, String def, ParamConverter<T> c) {
		initMatrix();
		return toArray(matrix.get(name), def, c);
	}

	private void initHeaders() {
		if (headers != null)
			return;
		headers = new MultivaluedHashMap<>();
		Enumeration<String> it = r.getHeaderNames();
		while (it.hasMoreElements()) {
			String n = it.nextElement();
			Enumeration<String> v = r.getHeaders(n);
			while (v.hasMoreElements())
				headers.addAll(n, v.nextElement());
		}
	}

	public void initPaths(JaxrsPath[] path) {
		paths = new HashMap<>();
		if (path.length == 0)
			return;

		String s = r.getRequestURI();
		int n = 0;
		for (int i = 0; i < path.length; i++) {
			JaxrsPath p = path[i];
			n += p.i;
			int l = s.indexOf('/', n);
			if (l == -1)
				l = s.length();
			paths.put(URLDecoder.decode(p.n, StandardCharsets.UTF_8), s.substring(n, l));
			n = l + 1;
		}
	}

	private void initQueries() {
		if (queries != null)
			return;
		parseQueryString(r.getQueryString(), queries = new MultivaluedHashMap<>());
	}

	private void initForms() throws IOException {
		if (forms != null)
			return;

		StringBuilder sb = new StringBuilder();
		char[] cbuf = new char[2048];
		int l = 0;
		try (BufferedReader r = this.r.getReader()) {
			while ((l = r.read(cbuf)) != -1)
				sb.append(cbuf, 0, l);
		}
		parseQueryString(sb.toString(), forms = new MultivaluedHashMap<>());
	}

	private void initCookies() {
		if (cookies != null)
			return;
		cookies = new MultivaluedHashMap<>();

		Cookie[] cookies = r.getCookies();
		if (cookies == null)
			return;
		for (int c$ = 0; c$ < cookies.length; c$++) {
			Cookie c = cookies[c$];
			this.cookies.add(URLDecoder.decode(c.getName(), StandardCharsets.UTF_8), c.getValue());
		}
	}

	private void initMatrix() {
		if (matrix != null)
			return;
		matrix = new MultivaluedHashMap<>();
		String p = r.getRequestURI();
		int e = p.lastIndexOf('/');
		e = p.indexOf(';', e < 0 ? 0 : e);
		if (e < 0)
			return;
		parseMatrix(p, e, matrix);
	}

	/**
	 * @param query
	 * @param map
	 */
	public static void parseQueryString(String query, MultivaluedMap<String, String> map) {
		if (query == null)
			return;
		int i = 0;
		int l = query.length();
		while (i < l) {
			int e = query.indexOf('&', i);
			if (e == -1)
				e = l;
			int q = query.indexOf('=', i);
			String value;
			if (q == -1 || q > e) {
				value = "";
				q = e;
			} else
				value = query.substring(q + 1, e);
			map.add(URLDecoder.decode(query.substring(i, q), StandardCharsets.UTF_8), value);
			i = e + 1;
		}
	}

	public static void parseMatrix(String path, int start, MultivaluedMap<String, String> matrix) {
		int l = path.length();
		do {
			int end = path.indexOf(';', start);
			if (end == -1)
				end = l;
			int equals = path.indexOf('=', start);

			if (equals == -1 || equals > end)
				matrix.add(path.substring(start, end), "");
			else
				matrix.add(path.substring(start, equals), path.substring(equals + 1, end));
			start = end + 1;
		} while (start < l);
	}

	private static <T> T toValue(String value, String def, ParamConverter<T> c) {
		if (value == null)
			value = def;
		if (value == null)
			return null;
		return c.fromString(value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] toArray(List<String> list, String def, ParamConverter<T> c) {
		if (list == null && def != null)
			list = Arrays.asList(def);
		if (list == null || list.isEmpty())
			return (T[]) EMPTY;
		T[] t = (T[]) new Object[list.size()];
		for (int i = 0; i < list.size(); i++)
			t[i] = c.fromString(list.get(i));
		return t;
	}
}
