/**
 * 
 */
package unknow.server.http.jaxrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import unknow.server.http.jaxrs.impl.DefaultConvert;

/**
 * @author unknow
 */
@SuppressWarnings("rawtypes")
public class JaxrsContext {
	private static final Logger logger = LoggerFactory.getLogger(JaxrsContext.class);
	private static final String[] ALL = { "*/*" };
	private static final List<ParamConverterProvider> params = new ArrayList<>(Arrays.asList(new DefaultConvert()));
	private static final Map<Class, ExceptionMapper> exceptions = new HashMap<>();

	private static final Map<String, List<MessageBodyWriter<Object>>> writers = new HashMap<>();
	private static final Map<String, List<MessageBodyReader<Object>>> readers = new HashMap<>();

	static {
		exceptions.put(WebApplicationException.class, new WebAppExceptionMapping());

		// TODO default reader/writer: byte[], String, InputStream, Reader, File, jakarta.activation.DataSource
		// javax.xml.transform.Source (text/xml, application/xml and media types of the form application/*+xml),
		// MultivaluedMap<String,String> (application/x-www-form-urlencoded)
		// java.util.List<EntityPart> (multipart/form-data)
		// StreamingOutput writer only
		// Boolean, Character, Number (plain/text) => NoContentException for empty body owith primitive

		// TODO PathSegment for param
	}

	public static <T extends Throwable> void registerException(Class<T> clazz, ExceptionMapper<T> e) {
		if (exceptions.containsKey(clazz))
			logger.warn("Duplicate ExceptionMapper for exception '" + clazz + "'");
		exceptions.put(clazz, e);
	}

	public static void registerConverter(ParamConverterProvider param) {
		params.add(param);
	}

	public static void registerReader(MessageBodyReader<Object> reader, String... mimes) {
		if (mimes.length == 0)
			mimes = ALL;
		for (String mime : mimes) {
			synchronized (readers) {
				List<MessageBodyReader<Object>> list = readers.get(mime);
				if (list == null)
					readers.put(mime, list = new ArrayList<>(1));
				list.add(reader);
			}
		}
	}

	public static void registerWriter(MessageBodyWriter<Object> writer, String... mimes) {
		if (mimes.length == 0)
			mimes = ALL;
		for (String mime : mimes) {
			synchronized (writers) {
				List<MessageBodyWriter<Object>> list = writers.get(mime);
				if (list == null)
					writers.put(mime, list = new ArrayList<>(1));
				list.add(writer);
			}
		}
	}

	public static <T> ParamConverter<T> converter(Class<T> clazz, Type type, Annotation[] a) {
		for (ParamConverterProvider p : params) {
			ParamConverter<T> c = p.getConverter(clazz, type, a);
			if (c != null)
				return c;
		}
		throw new RuntimeException("No converter for " + type);
	}

	@SuppressWarnings("unchecked")
	public static void sendError(JaxrsReq r, Throwable t, HttpServletResponse res) throws IOException {
		Class<?> c = t.getClass();
		do {
			ExceptionMapper<Throwable> m = exceptions.get(c);
			if (m != null) {
				JaxrsEntityWriter.RESPONSE.write(r, m.toResponse(t), res);
				return;
			}
		} while ((c = c.getSuperclass()) != Object.class);
		res.sendError(500);
	}

	@SuppressWarnings("unchecked")
	public static <T> MessageBodyReader<T> reader(Class<T> clazz, Type genericType, Annotation[] annotations, MediaType t) {
		for (MessageBodyReader<?> m : readers.getOrDefault(t.getType() + "/" + t.getSubtype(), Collections.emptyList())) {
			if (!m.isReadable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return (MessageBodyReader<T>) m;
		}
		for (MessageBodyReader<?> m : readers.getOrDefault(t.getType() + "/*", Collections.emptyList())) {
			if (!m.isReadable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return (MessageBodyReader<T>) m;
		}
		for (MessageBodyReader<?> m : readers.getOrDefault("*/*", Collections.emptyList())) {
			if (!m.isReadable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return (MessageBodyReader<T>) m;
		}
		throw new NotSupportedException("No reader for " + clazz + " " + t);
	}

	public static MessageBodyWriter<Object> writer(Class clazz, Type genericType, Annotation[] annotations, MediaType t) {
		for (MessageBodyWriter<Object> m : writers.getOrDefault(t.getType() + "/" + t.getSubtype(), Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return m;
		}
		for (MessageBodyWriter<Object> m : writers.getOrDefault(t.getType() + "/*", Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return m;
		}
		for (MessageBodyWriter<Object> m : writers.getOrDefault("*/*", Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return m;
		}
		throw new InternalServerErrorException("No writer for " + clazz + " " + t);
	}

	private static class WebAppExceptionMapping implements ExceptionMapper<WebApplicationException> {

		@Override
		public Response toResponse(WebApplicationException e) {
			return e.getResponse();
		}
	}
}
