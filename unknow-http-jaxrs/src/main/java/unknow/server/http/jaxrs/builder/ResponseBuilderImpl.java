/**
 * 
 */
package unknow.server.http.jaxrs.builder;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.RuntimeDelegate;
import unknow.server.http.jaxrs.header.MediaTypeDelegate;
import unknow.server.http.jaxrs.impl.ResponseImpl;

/**
 * @author unknow
 */
public class ResponseBuilderImpl extends ResponseBuilder {
	private int status;
	private Object entity;
	private Annotation[] annotations;
	private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
	private MultivaluedMap<String, Object> headersCustom = new MultivaluedHashMap<>();

	@Override
	public Response build() {
		MultivaluedMap<String, Object> h = new MultivaluedHashMap<>(headers);
		h.putAll(headersCustom);
		return new ResponseImpl(status, entity, annotations, h);
	}

	@Override
	public ResponseBuilder clone() {
		ResponseBuilderImpl r = new ResponseBuilderImpl();
		r.status = status;
		r.entity = entity;
		r.annotations = annotations;
		r.headers = new MultivaluedHashMap<>(headers);
		r.headersCustom = new MultivaluedHashMap<>(headersCustom);
		return r;
	}

	@Override
	public ResponseBuilder status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public ResponseBuilder status(int status, String reasonPhrase) {
		this.status = status;
		return this;
	}

	@Override
	public ResponseBuilder entity(Object entity) {
		this.entity = entity;
		return this;
	}

	@Override
	public ResponseBuilder entity(Object entity, Annotation[] annotations) {
		this.entity = entity;
		this.annotations = annotations;
		return this;
	}

	@Override
	public ResponseBuilder allow(String... methods) {
		headers.addAll("Allow", (Object[]) methods);
		return this;
	}

	@Override
	public ResponseBuilder allow(Set<String> methods) {
		headers.addAll("Allow", methods);
		return this;
	}

	@Override
	public ResponseBuilder cacheControl(CacheControl cacheControl) {
		headers.putSingle("Cache-Control", cacheControl);
		return this;
	}

	@Override
	public ResponseBuilder encoding(String encoding) {
		headers.putSingle("Content-Encoding", encoding);
		return this;
	}

	@Override
	public ResponseBuilder header(String name, Object value) {
		this.headersCustom.add(name, value);
		return this;
	}

	@Override
	public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
		this.headersCustom.clear();
		this.headersCustom.putAll(headers);
		return this;
	}

	@Override
	public ResponseBuilder language(String language) {
		headers.putSingle("Content-Language", Locale.forLanguageTag(language));
		return this;
	}

	@Override
	public ResponseBuilder language(Locale language) {
		headers.putSingle("Content-Language", language);
		return this;
	}

	@Override
	public ResponseBuilder type(MediaType type) {
		headers.putSingle("Content-Type", type);
		return this;
	}

	@Override
	public ResponseBuilder type(String type) {
		return type(MediaTypeDelegate.INSTANCE.fromString(type));
	}

	@Override
	public ResponseBuilder variant(Variant variant) {
		headers.putSingle("Content-Type", variant.getMediaType());
		headers.putSingle("Content-Language", variant.getLanguage());
		headers.putSingle("Content-Encoding", variant.getEncoding());
		return this;
	}

	@Override
	public ResponseBuilder contentLocation(URI location) {
		this.headers.putSingle("Content-Location", location);
		return this;
	}

	@Override
	public ResponseBuilder cookie(NewCookie... cookies) {
		this.headers.addAll("Set-Cookie", (Object[]) cookies);
		return this;
	}

	@Override
	public ResponseBuilder expires(Date expires) {
		this.headers.putSingle("Expires", expires);
		return this;
	}

	@Override
	public ResponseBuilder lastModified(Date lastModified) {
		this.headers.putSingle("Last-Modified", lastModified);
		return this;
	}

	@Override
	public ResponseBuilder location(URI location) {
		this.headers.putSingle("Location", location);
		return this;
	}

	@Override
	public ResponseBuilder tag(EntityTag tag) {
		headers.putSingle("ETag", tag);
		return this;
	}

	@Override
	public ResponseBuilder tag(String tag) {
		return tag(new EntityTag(tag));
	}

	@Override
	public ResponseBuilder variants(Variant... variants) {
		this.headers.put("Vary", Arrays.asList((Object[]) variants));
		return this;
	}

	@Override
	@SuppressWarnings({ "cast", "rawtypes" })
	public ResponseBuilder variants(List<Variant> variants) {
		this.headers.add("Vary", (List) variants);
		return this;
	}

	@Override
	public ResponseBuilder links(Link... links) {
		this.headers.addAll("Link", (Object[]) links);
		return this;
	}

	@Override
	public ResponseBuilder link(URI uri, String rel) {
		return links(RuntimeDelegate.getInstance().createLinkBuilder().baseUri(uri).rel(rel).build());
	}

	@Override
	public ResponseBuilder link(String uri, String rel) {
		return links(RuntimeDelegate.getInstance().createLinkBuilder().baseUri(uri).rel(rel).build());
	}
}
