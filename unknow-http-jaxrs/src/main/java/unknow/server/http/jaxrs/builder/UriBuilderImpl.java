/**
 * 
 */
package unknow.server.http.jaxrs.builder;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import unknow.server.http.jaxrs.JaxrsReq;

public class UriBuilderImpl extends UriBuilder {
	private String scheme;
	private String ssp;
	private String userInfo;
	private String host;
	private int port = -1;
	private final List<PathPart> paths = new ArrayList<>();
	private final MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
	private String fragment;

	@Override
	public UriBuilder uri(URI uri) {
		if (uri.getScheme() != null)
			scheme = uri.getScheme();
		if (uri.getSchemeSpecificPart() != null)
			ssp = uri.getSchemeSpecificPart();
		if (uri.getUserInfo() != null)
			userInfo = uri.getUserInfo();
		if (uri.getHost() != null)
			host = uri.getHost();
		if (uri.getPort() > -1)
			port = uri.getPort();
		if (uri.getPath() != null) {
			replacePath(uri.getPath());
		}
		if (uri.getQuery() != null)
			JaxrsReq.parseQueryString(uri.getQuery(), query);
		if (uri.getFragment() != null)
			fragment = uri.getFragment();
		return this;
	}

	@Override
	public UriBuilder uri(String uriTemplate) {
		return uri(URI.create(uriTemplate));
	}

	@Override
	public UriBuilder scheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	@Override
	public UriBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		return this;
	}

	@Override
	public UriBuilder userInfo(String ui) {
		this.userInfo = ui;
		return this;
	}

	@Override
	public UriBuilder host(String host) {
		this.host = host;
		return this;
	}

	@Override
	public UriBuilder port(int port) {
		this.port = port;
		return this;
	}

	@Override
	public UriBuilder replacePath(String path) {
		paths.clear();
		paths.add(new PathPart("", new MultivaluedHashMap<>()));
		return path(path);
	}

	@Override
	public UriBuilder path(String path) {
		int i;
		int l = 1;
		while ((i = path.indexOf('/', l)) > 0) {
			paths.add(PathPart.parse(path.substring(l, i)));
			l = i + 1;
		}
		if (l < path.length())
			paths.add(PathPart.parse(path.substring(l)));
		return this;
	}

	@Override
	public UriBuilder path(@SuppressWarnings("rawtypes") Class resource) {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public UriBuilder path(@SuppressWarnings("rawtypes") Class resource, String method) {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public UriBuilder path(Method method) {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public UriBuilder segment(String... segments) {
		if (segments.length == 0)
			return this;
		for (int i = 0; i < segments.length; i++)
			paths.add(PathPart.parse(segments[i]));
		return this;
	}

	@Override
	public UriBuilder replaceMatrix(String matrix) {
		MultivaluedMap<String, String> m = paths.get(paths.size() - 1).matrix;
		m.clear();
		JaxrsReq.parseMatrix(matrix, matrix.charAt(0) == ';' ? 1 : 0, m);
		return this;
	}

	@Override
	public UriBuilder matrixParam(String name, Object... values) {
		MultivaluedMap<String, String> matrix = paths.get(paths.size() - 1).matrix;
		for (int i = 0; i < values.length; i++)
			matrix.add(name, URLEncoder.encode(values[i].toString(), StandardCharsets.UTF_8));
		return this;
	}

	@Override
	public UriBuilder replaceMatrixParam(String name, Object... values) {
		MultivaluedMap<String, String> matrix = paths.get(paths.size() - 1).matrix;
		matrix.remove(name);
		return matrixParam(name, values);
	}

	@Override
	public UriBuilder replaceQuery(String query) {
		this.query.clear();
		JaxrsReq.parseQueryString(query, this.query);
		return this;
	}

	@Override
	public UriBuilder queryParam(String name, Object... values) {
		for (int i = 0; i < values.length; i++)
			query.add(name, URLEncoder.encode(values[i].toString(), StandardCharsets.UTF_8));
		return this;
	}

	@Override
	public UriBuilder replaceQueryParam(String name, Object... values) {
		query.remove(name);
		return queryParam(name, values);
	}

	@Override
	public UriBuilder fragment(String fragment) {
		this.fragment = fragment;
		return this;
	}

	@Override
	public UriBuilder resolveTemplate(String name, Object value) {
		if (name == null || value == null)
			throw new IllegalArgumentException("param can't be null");
		return resolveTemplates(Collections.singletonMap(name, value));
	}

	@Override
	public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
		if (name == null || value == null)
			throw new IllegalArgumentException("param can't be null");
		return resolveTemplates(Collections.singletonMap(name, value), encodeSlashInPath);
	}

	@Override
	public UriBuilder resolveTemplateFromEncoded(String name, Object value) {
		if (name == null || value == null)
			throw new IllegalArgumentException("param can't be null");
		return resolveTemplatesFromEncoded(Collections.singletonMap(name, value));
	}

	@Override
	public UriBuilder resolveTemplates(Map<String, Object> templateValues) {
		if (templateValues.isEmpty())
			return this;

		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) throws IllegalArgumentException {
		if (templateValues.isEmpty())
			return this;

		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
		if (templateValues.isEmpty())
			return this;

		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public URI buildFromMap(Map<String, ?> values) {
		return buildFromMap(values, true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
		return URI.create(clone().resolveTemplates((Map<String, Object>) values, encodeSlashInPath).toTemplate());
	}

	@SuppressWarnings("unchecked")
	@Override
	public URI buildFromEncodedMap(Map<String, ?> values) throws IllegalArgumentException, UriBuilderException {
		return URI.create(clone().resolveTemplatesFromEncoded((Map<String, Object>) values).toTemplate());
	}

	@Override
	public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
		return build(values, true);
	}

	@Override
	public URI build(Object[] values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toTemplate() {
		StringBuilder sb = new StringBuilder();
		if (scheme != null)
			sb.append(scheme).append(':');
		if (ssp != null)
			sb.append(ssp);
		if (host != null) {
			sb.append("//");
			if (userInfo != null)
				sb.append(userInfo).append('@');
			sb.append(host);
			if (port > -1)
				sb.append(':').append(port);
		}

		// path
		for (PathPart p : paths)
			p.append(sb.append('/'));

		if (!query.isEmpty()) {
			boolean first = true;
			for (String key : query.keySet()) {
				for (String v : query.get(key)) {
					sb.append(first ? '?' : '&').append(key);
					if (!v.isEmpty())
						sb.append('=').append(v);
					first = false;
				}
			}
		}

		if (fragment != null)
			sb.append('#').append(fragment);
		return null;
	}

	@Override
	public UriBuilder clone() {
		UriBuilderImpl uri = new UriBuilderImpl();
		uri.scheme = scheme;
		uri.ssp = ssp;
		uri.userInfo = userInfo;
		uri.host = host;
		uri.port = port;
		uri.paths.addAll(paths);
		uri.paths.set(paths.size() - 1, paths.get(paths.size() - 1).clone());
		uri.query.putAll(query);
		uri.fragment = fragment;
		return uri;
	}

	private static final class PathPart {
		private final String path;
		private final MultivaluedMap<String, String> matrix;

		public PathPart(String path, MultivaluedMap<String, String> matrix) {
			this.path = path;
			this.matrix = matrix;
		}

		public static PathPart parse(String path) {
			int i = path.indexOf(';');
			MultivaluedMap<String, String> matrix = new MultivaluedHashMap<>();
			if (i > 0)
				JaxrsReq.parseMatrix(path, i, matrix);
			return new PathPart(i < 0 ? path : path.substring(i), matrix);
		}

		@Override
		public PathPart clone() {
			return new PathPart(path, new MultivaluedHashMap<>(matrix));
		}

		public StringBuilder append(StringBuilder sb) {
			sb.append(path);
			for (String key : matrix.keySet()) {
				for (String v : matrix.get(key)) {
					sb.append(';').append(key);
					if (!v.isEmpty())
						sb.append('=').append(v);
				}
			}
			return sb;
		}

		@Override
		public String toString() {
			if (matrix.isEmpty())
				return path;
			return append(new StringBuilder()).toString();
		}
	}
}
