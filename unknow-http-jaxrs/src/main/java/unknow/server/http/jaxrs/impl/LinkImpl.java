/**
 * 
 */
package unknow.server.http.jaxrs.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;

/**
 * @author unknow
 */
public class LinkImpl extends Link {
	private final URI uri;
	private final Map<String, String> params;

	public LinkImpl(URI uri, Map<String, String> params) {
		this.uri = uri;
		this.params = Collections.unmodifiableMap(params);
	}

	@Override
	public Map<String, String> getParams() {
		return params;
	}

	@Override
	public String getRel() {
		return params.get(Link.REL);
	}

	@Override
	public List<String> getRels() {
		String rel = getRel();
		if (rel == null)
			return Collections.emptyList();

		String[] values = rel.split(" ");
		List<String> rels = new ArrayList<>(values.length);
		for (String val : values)
			rels.add(val.trim());
		return rels;
	}

	@Override
	public String getTitle() {
		return params.get(Link.TITLE);
	}

	@Override
	public String getType() {
		return params.get(Link.TYPE);
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public UriBuilder getUriBuilder() {
		return UriBuilder.fromUri(uri);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('<').append(uri.toString()).append('>');
		for (Map.Entry<String, String> entry : params.entrySet())
			sb.append(';').append(entry.getKey()).append("=\"").append(entry.getValue()).append('"');
		return sb.toString();
	}
}
