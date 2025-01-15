/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import unknow.server.http.jaxrs.impl.ResponseImpl;

/**
 * write entity
 * 
 * @author unknow
 * @param <T> the object type
 */
public interface JaxrsEntityWriter<T> {
	public static JaxrsEntityWriter<Response> RESPONSE = new ResponseWriter(new Annotation[0]);

	@SuppressWarnings("unchecked")
	public static <T> JaxrsEntityWriter<T> create(Class<?> clazz, Type genericType, Annotation[] annotations) {
		if (Response.class == clazz)
			return (JaxrsEntityWriter<T>) (annotations.length == 0 ? RESPONSE : new ResponseWriter(annotations));
		if (GenericEntity.class.isAssignableFrom(clazz))
			return (JaxrsEntityWriter<T>) new GenericWriter(annotations);
		return new ObjectWriter<T>(clazz, genericType, annotations);
	}

	/**
	 * write the object
	 * 
	 * @param r the request
	 * @param e the oject
	 * @param res the response
	 * @throws WebApplicationException on application error
	 * @throws IOException on io error
	 */
	void write(JaxrsReq r, T e, HttpServletResponse res) throws WebApplicationException, IOException;

	/**
	 * writer for Object
	 * 
	 * @param <T> object class
	 * @author unknow
	 */
	static class ObjectWriter<T> implements JaxrsEntityWriter<T> {
		private final Class<?> clazz;
		private final Type genericType;
		private final Annotation[] annotations;

		public ObjectWriter(Class<?> clazz, Type genericType, Annotation[] annotations) {
			this.clazz = clazz;
			this.genericType = genericType;
			this.annotations = annotations;
		}

		@Override
		public void write(JaxrsReq r, Object e, HttpServletResponse res) throws WebApplicationException, IOException {
			MediaType mediaType = r.getAccept();
			res.setContentType(mediaType.toString());
			MultivaluedMap<String, Object> httpHeaders = new ResponseHeader(res);
			MessageBodyWriter<Object> writer = JaxrsContext.writer(clazz, genericType, annotations, mediaType);
			try (ServletOutputStream out = res.getOutputStream()) {
				writer.writeTo(e, clazz, genericType, annotations, mediaType, httpHeaders, out);
			}
		}
	}

	/**
	 * writer for Response
	 * 
	 * @author unknow
	 */
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
				MediaType mediaType = e.getMediaType();
				if (mediaType == null)
					mediaType = r.getAccept();
				res.setContentType(mediaType.toString());

				Class<?> clazz = o.getClass();
				Annotation[] a = e instanceof ResponseImpl ? ((ResponseImpl) e).getAnnotations() : null;
				if (a == null)
					a = annotations;

				if (o instanceof GenericEntity)
					GenericWriter.write((GenericEntity<?>) o, mediaType, a, res);
				else {
					MessageBodyWriter<Object> writer = JaxrsContext.writer(clazz, clazz, a, mediaType);
					try (ServletOutputStream out = res.getOutputStream()) {
						writer.writeTo(e.getEntity(), clazz, clazz, a, mediaType, httpHeaders, out);
					}
				}
			}
		}
	}

	/**
	 * writer for GenericEntity
	 * 
	 * @author unknow
	 */
	static class GenericWriter implements JaxrsEntityWriter<GenericEntity<?>> {
		private final Annotation[] annotations;

		public GenericWriter(Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public void write(JaxrsReq r, GenericEntity<?> e, HttpServletResponse res) throws WebApplicationException, IOException {
			res.setContentType(r.getAccept().toString());
			write(e, r.getAccept(), annotations, res);
		}

		static void write(GenericEntity<?> e, MediaType mediaType, Annotation[] annotations, HttpServletResponse res) throws WebApplicationException, IOException {
			Class<?> clazz = e.getRawType();
			Type genericType = e.getType();
			Object o = e.getEntity();

			MultivaluedMap<String, Object> httpHeaders = new ResponseHeader(res);
			MessageBodyWriter<Object> writer = JaxrsContext.writer(clazz, genericType, annotations, mediaType);
			try (ServletOutputStream out = res.getOutputStream()) {
				writer.writeTo(o, clazz, genericType, annotations, mediaType, httpHeaders, out);
			}
		}
	}
}
