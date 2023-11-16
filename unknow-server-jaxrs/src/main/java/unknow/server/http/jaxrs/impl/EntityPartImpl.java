/**
 * 
 */
package unknow.server.http.jaxrs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Optional;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import unknow.server.http.jaxrs.JaxrsContext;

/**
 * @author unknow
 */
public class EntityPartImpl implements EntityPart {
	private final InputStream content;
	private final MultivaluedMap<String, String> headers;
	private final MediaType mt;

	/**
	 * create new EntityPartImpl
	 * 
	 * @param content
	 * @param headers
	 * @param mt
	 */
	public EntityPartImpl(InputStream content, MultivaluedMap<String, String> headers, MediaType mt) {
		this.content = content;
		this.headers = headers;
		this.mt = mt;
	}

	@Override
	public String getName() {
		String first = headers.getFirst("Content-Disposition");
		if (first == null)
			return null;

		int i = first.indexOf("filename=");
		if (i < 0)
			return null;
		i += 9;
		int e = first.indexOf(";", i);
		return (e < 0 ? first.substring(i) : first.substring(i, e)).trim();
	}

	@Override
	public Optional<String> getFileName() {
		String first = headers.getFirst("Content-Disposition");
		if (first == null)
			return Optional.empty();

		int i = first.indexOf("filename=");
		if (i < 0)
			return Optional.empty();
		i += 9;
		int e = first.indexOf(";", i);
		return Optional.of((e < 0 ? first.substring(i) : first.substring(i, e)).trim());
	}

	@Override
	public InputStream getContent() {
		return content;
	}

	@Override
	public <T> T getContent(Class<T> type) throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
		try {
			MessageBodyReader<T> reader = JaxrsContext.reader(type, type, new Annotation[0], mt);
			return reader.readFrom(type, type, new Annotation[0], mt, headers, content);
		} catch (Exception e) {
			throw new ProcessingException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> T getContent(GenericType<T> type) throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
		try {
			MessageBodyReader reader = JaxrsContext.reader(type.getRawType(), type.getType(), new Annotation[0], mt);
			return (T) reader.readFrom(type.getRawType(), type.getType(), new Annotation[0], mt, headers, content);
		} catch (Exception e) {
			throw new ProcessingException(e);
		}
	}

	@Override
	public MultivaluedMap<String, String> getHeaders() {
		return headers;
	}

	@Override
	public MediaType getMediaType() {
		return mt;
	}
}
