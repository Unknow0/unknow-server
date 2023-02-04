/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * @author unknow
 */
public class JaxrsBody<T> {
	private final Class<T> clazz;
	private final Type genericType;
	private final Annotation[] annotations;

	public JaxrsBody(Class<T> clazz, Type genericType, Annotation[] annotations) {
		this.clazz = clazz;
		this.genericType = genericType;
		this.annotations = annotations;
	}

	public T read(JaxrsReq r) throws WebApplicationException, IOException {
		HttpServletRequest req = r.getRequest();
		String contentType = req.getContentType();
		if (contentType == null)
			contentType = "*/*";
		MediaType mediaType = MediaType.valueOf(contentType);

		MultivaluedMap<String, String> httpHeaders = r.getHeaders();
		return JaxrsContext.reader(clazz, genericType, annotations, mediaType).readFrom(clazz, genericType, annotations, mediaType, httpHeaders, req.getInputStream());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void write(JaxrsReq r, Object e, HttpServletResponse res) throws WebApplicationException, IOException {
		HttpServletRequest req = r.getRequest();
		String contentType = req.getHeader("Accept");
		if (contentType == null)
			contentType = "*/*";
		MediaType mediaType = MediaType.valueOf(contentType);

		Class clazz = this.clazz;
		Type genericType = this.genericType;
		if (e instanceof GenericEntity) {
			GenericEntity g = (GenericEntity) e;
			clazz = g.getRawType();
			genericType = g.getType();
			e = g.getEntity();
		}

		MultivaluedMap<String, Object> httpHeaders = new ResponseHeader(res);

		JaxrsContext.writer(clazz, genericType, annotations, mediaType).writeTo(e, clazz, genericType, annotations, mediaType, httpHeaders, res.getOutputStream());
	}
}
