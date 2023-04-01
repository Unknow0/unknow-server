/**
 * 
 */
package unknow.server.http.jaxrs.impl;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import unknow.server.http.jaxrs.JaxrsRuntime;

public class ResponseImpl extends Response {
	private final int status;
	private final Object entity;
	private final Annotation[] annotations;
	private final MultivaluedMap<String, Object> headers;

	/**
	 * create new ResponseBuilderImpl.ResponseImpl
	 */
	public ResponseImpl(int status, Object entity, Annotation[] annotations, MultivaluedMap<String, Object> headers) {
		this.status = status;
		this.entity = entity;
		this.annotations = annotations;
		this.headers = headers;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public StatusType getStatusInfo() {
		return Status.fromStatusCode(status);
	}

	@Override
	public Object getEntity() {
		return entity;
	}

	@Override
	public <T> T readEntity(Class<T> entityType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T readEntity(GenericType<T> entityType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasEntity() {
		return entity != null;
	}

	@Override
	public boolean bufferEntity() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public MediaType getMediaType() {
		return (MediaType) headers.getFirst("Content-Type");
	}

	@Override
	public Locale getLanguage() {
		return (Locale) headers.getFirst("Content-Language");
	}

	@Override
	public int getLength() {
		Object first = headers.getFirst("Content-Length");
		return first == null ? -1 : (int) first;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<String> getAllowedMethods() {
		return new HashSet(headers.get("Allow"));
	}

	@Override
	public Map<String, NewCookie> getCookies() {
		Map<String, NewCookie> map = new HashMap<>();
		for (Object o : headers.get("Set-Cookie")) {
			NewCookie c = (NewCookie) o;
			map.put(c.getName(), c);
		}
		return map;
	}

	@Override
	public EntityTag getEntityTag() {
		return (EntityTag) headers.getFirst("ETag");
	}

	@Override
	public Date getDate() {
		return (Date) headers.getFirst("Date");
	}

	@Override
	public Date getLastModified() {
		return (Date) headers.getFirst("Last-Modified");
	}

	@Override
	public URI getLocation() {
		return (URI) headers.getFirst("Location");
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<Link> getLinks() {
		return new HashSet(headers.get("Link"));
	}

	@Override
	public boolean hasLink(String relation) {
		for (Object o : headers.get("Link")) {
			if (((Link) o).getRels().contains(relation))
				return true;
		}
		return false;
	}

	@Override
	public Link getLink(String relation) {
		for (Object o : headers.get("Link")) {
			Link l = (Link) o;
			if (l.getRels().contains(relation))
				return l;
		}
		return null;
	}

	@Override
	public Builder getLinkBuilder(String relation) {
		Link link = getLink(relation);
		return link == null ? null : Link.fromLink(link);
	}

	@Override
	public MultivaluedMap<String, Object> getMetadata() {
		return getHeaders();
	}

	@Override
	public MultivaluedMap<String, String> getStringHeaders() { // TODO replace with an internal class
		MultivaluedMap<String, String> h = new MultivaluedHashMap<>();
		for (Entry<String, List<Object>> e : headers.entrySet()) {
			String key = e.getKey();
			for (Object v : e.getValue()) {
				JaxrsRuntime.header(v);
			}
			h.put(e.getKey(), e.getValue().stream().map(JaxrsRuntime::header).collect(Collectors.toList()));
		}
		return h;
	}

	@Override
	public String getHeaderString(String name) {
		List<Object> list = headers.get(name);
		if (list == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (Object o : list) {
			if (sb.length() > 0)
				sb.append(',');
			sb.append(JaxrsRuntime.header(o));
		}
		return sb.toString();
	}
}