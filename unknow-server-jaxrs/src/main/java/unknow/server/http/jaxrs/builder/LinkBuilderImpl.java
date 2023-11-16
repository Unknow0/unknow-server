/**
 * 
 */
package unknow.server.http.jaxrs.builder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.UriBuilder;
import unknow.server.http.jaxrs.impl.LinkImpl;

/**
 * @author unknow
 */
public class LinkBuilderImpl implements Link.Builder {
	private UriBuilder ub;
	private URI uri;
	private final Map<String, String> params = new HashMap<>(6);

	@Override
	public Builder link(Link link) {
		ub = UriBuilder.fromLink(link);
		params.putAll(link.getParams());
		return this;
	}

	@Override
	public Builder link(String link) {
		link = link.trim();
		if (link.length() > 1 && link.startsWith("<")) {
			int index = link.lastIndexOf('>', link.length());
			if (index != -1) {
				ub = UriBuilder.fromUri(link.substring(1, index));
				if (index + 1 == link.length()) {
					link = "";
				} else {
					link = link.substring(index + 1);
				}
			}
		}

		String[] tokens = link.split(";");
		for (String token : tokens) {
			String theToken = token.trim();
			if (!theToken.isEmpty()) {
				int i = theToken.indexOf('=');
				if (i != -1) {
					String name = theToken.substring(0, i);
					String value = theToken.substring(i + 1).replace("\"", "");
					params.put(name, value);
				}
			}
		}
		return this;
	}

	@Override
	public Builder uri(URI uri) {
		ub = UriBuilder.fromUri(uri);
		return this;
	}

	@Override
	public Builder uri(String uri) {
		ub = UriBuilder.fromUri(uri);
		return this;
	}

	@Override
	public Builder baseUri(URI uri) {
		this.uri = uri;
		return this;
	}

	@Override
	public Builder baseUri(String uri) {
		this.uri = URI.create(uri);
		return this;
	}

	@Override
	public Builder uriBuilder(UriBuilder uriBuilder) {
		this.ub = uriBuilder;
		return this;
	}

	@Override
	public Builder rel(String rel) {
		return param(Link.REL, rel);
	}

	@Override
	public Builder title(String title) {
		return param(Link.TITLE, title);
	}

	@Override
	public Builder type(String type) {
		return param(Link.TYPE, type);
	}

	@Override
	public Builder param(String name, String value) {
		params.put(name, value);
		return this;
	}

	@Override
	public Link build(Object... values) {
		URI resolvedLinkUri = getResolvedUri(values);
		return new LinkImpl(resolvedLinkUri, new HashMap<>(params));
	}

	@Override
	public Link buildRelativized(URI requestUri, Object... values) {
		URI resolvedLinkUri = getResolvedUri(values);
		URI relativized = relativize(requestUri, resolvedLinkUri);
		return new LinkImpl(relativized, new HashMap<>(params));
	}

	private URI getResolvedUri(Object... values) {
		if (ub == null) {
			ub = new UriBuilderImpl();
			if (uri != null)
				ub.uri(uri);
		}
		URI u = ub.build(values);

		if (!u.isAbsolute() && uri != null && uri.isAbsolute()) {
			UriBuilder linkUriBuilder = UriBuilder.fromUri(uri);
			return resolve(linkUriBuilder, u);
		}
		return u;
	}

	public static URI resolve(UriBuilder baseBuilder, URI uri) {
		if (!uri.isAbsolute())
			return baseBuilder.build().resolve(uri);
		return uri;
	}

	public static URI relativize(URI base, URI uri) {
		// quick bail-out
		if (!(base.isAbsolute()) || !(uri.isAbsolute())) {
			return uri;
		}
		if (base.isOpaque() || uri.isOpaque()) {
			// Unlikely case of an URN which can't deal with
			// relative path, such as urn:isbn:0451450523
			return uri;
		}
		// Check for common root
		URI root = base.resolve("/");
		if (!(root.equals(uri.resolve("/")))) {
			// Different protocol/auth/host/port, return as is
			return uri;
		}

		// Ignore hostname bits for the following , but add "/" in the beginning
		// so that in worst case we'll still return "/fred" rather than
		// "http://example.com/fred".
		URI baseRel = URI.create("/").resolve(root.relativize(base));
		URI uriRel = URI.create("/").resolve(root.relativize(uri));

		// Is it same path?
		if (baseRel.getPath().equals(uriRel.getPath())) {
			return baseRel.relativize(uriRel);
		}

		// Direct siblings? (ie. in same folder)
		URI commonBase = baseRel.resolve("./");
		if (commonBase.equals(uriRel.resolve("./"))) {
			return commonBase.relativize(uriRel);
		}

		// No, then just keep climbing up until we find a common base.
		URI relative = URI.create("");
		while (!(uriRel.getPath().startsWith(commonBase.getPath())) && !"/".equals(commonBase.getPath())) {
			commonBase = commonBase.resolve("../");
			relative = relative.resolve("../");
		}

		// Now we can use URI.relativize
		URI relToCommon = commonBase.relativize(uriRel);
		// and prepend the needed ../
		return relative.resolve(relToCommon);

	}
}
