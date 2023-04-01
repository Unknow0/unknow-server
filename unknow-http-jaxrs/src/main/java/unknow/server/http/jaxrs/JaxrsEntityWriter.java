/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import unknow.server.http.jaxrs.impl.ResponseImpl;

/**
 * write entity
 * 
 * @author unknow
 * @param <T> the object type
 */
public interface JaxrsEntityWriter<T> {

	@SuppressWarnings("unchecked")
	public static <T> JaxrsEntityWriter<T> create(Class<T> clazz, Type genericType, Annotation[] annotations) {
		if (Response.class == clazz)
			return (JaxrsEntityWriter<T>) new ResponseWriter(annotations);
		if (GenericEntity.class.isAssignableFrom(clazz))
			return (JaxrsEntityWriter<T>) new GenericWriter(annotations);
		return new ObjectWriter<>(clazz, genericType, annotations);
	}

	/**
	 * write the object
	 * 
	 * @param r   the request
	 * @param e   the oject
	 * @param res the response
	 * @throws WebApplicationException on application error
	 * @throws IOException             on io error
	 */
	void write(JaxrsReq r, T e, HttpServletResponse res) throws WebApplicationException, IOException;

	static class ObjectWriter<T> implements JaxrsEntityWriter<T> {
		private final Class<T> clazz;
		private final Type genericType;
		private final Annotation[] annotations;

		public ObjectWriter(Class<T> clazz, Type genericType, Annotation[] annotations) {
			this.clazz = clazz;
			this.genericType = genericType;
			this.annotations = annotations;
		}

		@Override
		public void write(JaxrsReq r, Object e, HttpServletResponse res) throws WebApplicationException, IOException {
			MediaType mediaType = r.getAccept();
			MultivaluedMap<String, Object> httpHeaders = new ResponseHeader(res);
			JaxrsContext.writer(clazz, genericType, annotations, mediaType).writeTo(e, clazz, genericType, annotations, mediaType, httpHeaders, res.getOutputStream());
		}
	}

	static class ResponseWriter implements JaxrsEntityWriter<Response> {
		private final Annotation[] annotations;

		public ResponseWriter(Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public void write(JaxrsReq r, Response e, HttpServletResponse res) throws WebApplicationException, IOException {
			res.setStatus(e.getStatus());

			MultivaluedMap<String, Object> httpHeaders = new ResponseHeader(res);
			httpHeaders.putAll(e.getHeaders());

			Object o = e.getEntity();
			if (o != null) {
				MediaType mediaType = r.getAccept();
				Class<?> clazz = o.getClass();
				Annotation[] a = e instanceof ResponseImpl ? ((ResponseImpl) e).getAnnotations() : null;
				if (a == null)
					a = annotations;

				if (o instanceof GenericEntity)
					GenericWriter.write((GenericEntity<?>) o, mediaType, a, res);
				else
					JaxrsContext.writer(clazz, clazz, a, mediaType).writeTo(e, clazz, clazz, a, mediaType, httpHeaders, res.getOutputStream());
			}
		}
	}

	static class GenericWriter implements JaxrsEntityWriter<GenericEntity<?>> {
		private final Annotation[] annotations;

		public GenericWriter(Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public void write(JaxrsReq r, GenericEntity<?> e, HttpServletResponse res) throws WebApplicationException, IOException {
			write(e, r.getAccept(), annotations, res);
		}

		static void write(GenericEntity<?> e, MediaType mediaType, Annotation[] annotations, HttpServletResponse res) throws WebApplicationException, IOException {
			Class<?> clazz = e.getRawType();
			Type genericType = e.getType();
			Object o = e.getEntity();

			MultivaluedMap<String, Object> httpHeaders = new ResponseHeader(res);
			JaxrsContext.writer(clazz, genericType, annotations, mediaType).writeTo(o, clazz, genericType, annotations, mediaType, httpHeaders, res.getOutputStream());
		}
	}
}
