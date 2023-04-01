/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ParamConverter;

/**
 * @author unknow
 */
public class JaxrsReq {
	private final HttpServletRequest r;
	private final JaxrsPath[] path;

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
	 * @param path
	 */
	public JaxrsReq(HttpServletRequest r, JaxrsPath[] path) {
		this.r = r;
		this.path = path;
	}

	public HttpServletRequest getRequest() {
		return r;
	}

	public MediaType getAccept() {
		String contentType = r.getHeader("Accept");
		if (contentType == null)
			contentType = "*/*";
		return MediaType.valueOf(contentType);
	}

	public <T> T getPath(String path, String def, ParamConverter<T> c) {
		initPaths();
		String s = paths.getOrDefault(path, def);
		if (s == null)
			return null;
		return c.fromString(s);
	}

	public MultivaluedMap<String, String> getHeaders() {
		initHeaders();
		return headers;
	}

	public <T> T getHeader(String name, String def, ParamConverter<T> c) {
		initHeaders();
		String value = headers.getFirst(name);
		if (value == null)
			value = def;
		if (value == null)
			return null;
		return c.fromString(value);
	}

	public <T> T getQuery(String name, String def, ParamConverter<T> c) {
		initQueries();
		String value = queries.getFirst(name);
		if (value == null)
			value = def;
		if (value == null)
			return null;
		return c.fromString(value);
	}

	public <T> T getForm(String name, String def, ParamConverter<T> c) throws IOException {
		initForms();
		String value = forms.getFirst(name);
		if (value == null)
			value = def;
		if (value == null)
			return null;
		return c.fromString(value);
	}

	public <T> T getCookie(String name, String def, ParamConverter<T> c) {
		initCookies();
		String value = cookies.getFirst(name);
		if (value == null)
			value = def;
		if (value == null)
			return null;
		return c.fromString(value);
	}

	public <T> T getMatrix(String name, String def, ParamConverter<T> c) {
		initMatrix();
		String value = matrix.getFirst(name);
		if (value == null)
			value = def;
		if (value == null)
			return null;
		return c.fromString(value);
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

	private void initPaths() {
		if (paths != null)
			return;
		paths = new HashMap<>();
		if (path.length == 0)
			return;

		String s = r.getServletPath();
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
		String p = r.getServletPath();
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
}
