/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import unknow.server.http.jaxrs.header.MediaTypeDelegate;

/**
 * read body content into class
 * 
 * @author unknow
 * @param <T> class type
 */
public class JaxrsEntityReader<T> {
	private final Class<T> clazz;
	private final Type genericType;
	private final Annotation[] annotations;
	private final Map<MediaType, MessageBodyReader<T>> readers;

	/**
	 * create new JaxrsBodyReader
	 * 
	 * @param clazz the class
	 * @param genericType the class as a generix
	 * @param annotations the annotation on the param
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JaxrsEntityReader(Class clazz, Type genericType, Annotation[] annotations) {
		this.clazz = clazz;
		this.genericType = genericType;
		this.annotations = annotations;
		this.readers = new ConcurrentHashMap<>();
	}

	/**
	 * do the read
	 * 
	 * @param r request content
	 * @return the java class
	 * @throws WebApplicationException
	 * @throws IOException
	 */
	public T read(JaxrsReq r) throws WebApplicationException, IOException {
		HttpServletRequest req = r.getRequest();
		String contentType = req.getContentType();
		if (contentType == null)
			contentType = "*/*";
		MediaType mediaType = MediaTypeDelegate.INSTANCE.fromString(contentType);
		MessageBodyReader<T> reader = readers.computeIfAbsent(mediaType, k -> JaxrsContext.reader(clazz, genericType, annotations, mediaType));

		MultivaluedMap<String, String> httpHeaders = r.getHeaders();
		try (InputStream is = req.getInputStream()) {
			return reader.readFrom(clazz, genericType, annotations, mediaType, httpHeaders, is);
		}
	}
}
