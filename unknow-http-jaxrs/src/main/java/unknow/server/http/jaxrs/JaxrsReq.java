/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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
	private final HttpServletRequest r;
	private final List<String> pathValue;

	private MediaType accept;

	private Map<String, Integer> paths;

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
	public JaxrsReq(HttpServletRequest r, List<String> paths) {
		this.r = r;
		this.pathValue = paths;
	}

	public void initPaths(Map<String, Integer> paths) {
		this.paths = paths;
	}

	public HttpServletRequest getRequest() {
		return r;
	}

	public String getMethod() {
		return r.getMethod();
	}

	public MediaType getAccept() {
		return accept;
	}

	public MediaType getContentType() {
		String h = r.getHeader("content-type");
		return h == null ? MediaType.APPLICATION_OCTET_STREAM_TYPE : MediaTypeDelegate.INSTANCE.fromString(h);
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
		Integer i = paths.get(path);
		return toValue(i == null ? null : pathValue.get(i), def, c);
	}

	@SuppressWarnings("unchecked")
	public <T> T[] getPathArray(String path, String def, Class<T> cl, ParamConverter<T> c) {
		T s = getPath(path, def, c);
		if (s == null)
			return (T[]) Array.newInstance(cl, 0);
		T[] t = (T[]) Array.newInstance(cl, 1);
		t[0] = s;
		return t;
	}

	public <T> List<T> getPathList(String path, String def, ParamConverter<T> c) {
		T s = getPath(path, def, c);
		if (s == null)
			return Collections.emptyList();
		return Arrays.asList(s);
	}

	public MultivaluedMap<String, String> getHeaders() {
		initHeaders();
		return headers;
	}

	public <T> T getHeader(String name, String def, ParamConverter<T> c) {
		initHeaders();
		return toValue(headers.getFirst(name), def, c);
	}

	public <T> T[] getHeaderArray(String name, String def, Class<T> cl, ParamConverter<T> c) {
		initHeaders();
		return toArray(headers.get(name), def, cl, c);
	}

	public <T> List<T> getHeaderList(String name, String def, ParamConverter<T> c) {
		initHeaders();
		return toList(headers.get(name), def, c);
	}

	public <T> T getQuery(String name, String def, ParamConverter<T> c) {
		initQueries();
		return toValue(queries.getFirst(name), def, c);
	}

	public <T> T[] getQueryArray(String name, String def, Class<T> cl, ParamConverter<T> c) {
		initQueries();
		return toArray(queries.get(name), def, cl, c);
	}

	public <T> List<T> getQueryList(String name, String def, ParamConverter<T> c) {
		initQueries();
		return toList(queries.get(name), def, c);
	}

	public <T> T getForm(String name, String def, ParamConverter<T> c) throws IOException {
		initForms();
		return toValue(forms.getFirst(name), def, c);
	}

	public <T> T[] getFormArray(String name, String def, Class<T> cl, ParamConverter<T> c) throws IOException {
		initForms();
		return toArray(forms.get(name), def, cl, c);
	}

	public <T> List<T> getFormList(String name, String def, ParamConverter<T> c) throws IOException {
		initForms();
		return toList(forms.get(name), def, c);
	}

	public <T> T getCookie(String name, String def, ParamConverter<T> c) {
		initCookies();
		return toValue(cookies.getFirst(name), def, c);
	}

	public <T> T[] getCookieArray(String name, String def, Class<T> cl, ParamConverter<T> c) {
		initCookies();
		return toArray(cookies.get(name), def, cl, c);
	}

	public <T> List<T> getCookieList(String name, String def, ParamConverter<T> c) {
		initCookies();
		return toList(cookies.get(name), def, c);
	}

	public <T> T getMatrix(String name, String def, ParamConverter<T> c) {
		initMatrix();
		return toValue(matrix.getFirst(name), def, c);
	}

	public <T> T[] getMatrixArray(String name, String def, Class<T> cl, ParamConverter<T> c) {
		initMatrix();
		return toArray(matrix.get(name), def, cl, c);
	}

	public <T> List<T> getMatrixList(String name, String def, ParamConverter<T> c) {
		initMatrix();
		return toList(matrix.get(name), def, c);
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
	private static <T> T[] toArray(List<String> list, String def, Class<T> cl, ParamConverter<T> c) {
		if (list == null && def != null)
			list = Arrays.asList(def);
		if (list == null || list.isEmpty())
			return (T[]) Array.newInstance(cl, 0);
		T[] t = (T[]) Array.newInstance(cl, list.size());
		for (int i = 0; i < list.size(); i++)
			t[i] = c.fromString(list.get(i));
		return t;
	}

	private static <T> List<T> toList(List<String> list, String def, ParamConverter<T> c) {
		if (list == null && def != null)
			list = Arrays.asList(def);
		if (list == null || list.isEmpty())
			return Collections.emptyList();
		List<T> t = new ArrayList<>(list.size());
		for (String s : list)
			t.add(c.fromString(s));
		return t;
	}
}
