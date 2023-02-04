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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import unknow.server.http.jaxrs.impl.DateDelegate;
import unknow.server.http.jaxrs.impl.NewCookieDelegate;

/**
 * @author unknow
 */
@SuppressWarnings("rawtypes")
public class JaxrsContext {
	private static final String[] ALL = { "*/*" };
	private static final List<ParamConverterProvider> params = new ArrayList<>(Arrays.asList(new DefaultConvert()));
	private static final Map<Class, ExceptionMapper> exceptions = new HashMap<>();

	private static final Map<String, List<MessageBodyWriter>> writers = new HashMap<>();
	private static final Map<String, List<MessageBodyReader>> readers = new HashMap<>();

	static {
		exceptions.put(WebApplicationException.class, new WebAppExceptionMapping());
	}

	public static <T extends Throwable> void register(Class<T> clazz, ExceptionMapper<T> e) {
		exceptions.put(clazz, e);
	}

	public static void registerConverter(ParamConverterProvider param) {
		params.add(param);
	}

	public static void registerReader(MessageBodyReader<?> reader, String... mimes) {
		if (mimes.length == 0)
			mimes = ALL;
		for (String mime : mimes) {
			synchronized (readers) {
				List<MessageBodyReader> list = readers.get(mime);
				if (list == null)
					readers.put(mime, list = new ArrayList<>(1));
				list.add(reader);
			}
		}
	}

	public static void registerWriter(MessageBodyWriter<?> writer, String... mimes) {
		if (mimes.length == 0)
			mimes = ALL;
		for (String mime : mimes) {
			synchronized (writers) {
				List<MessageBodyWriter> list = writers.get(mime);
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
	public static void sendError(Throwable t, HttpServletResponse res) throws IOException {
		Class<?> c = t.getClass();
		do {
			ExceptionMapper<Throwable> m = exceptions.get(c);
			if (m != null) {
				sendResponse(m.toResponse(t), res);
				return;
			}
		} while ((c = c.getSuperclass()) != Throwable.class);
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

	@SuppressWarnings("unchecked")
	public static <T> MessageBodyWriter<T> writer(Class<T> clazz, Type genericType, Annotation[] annotations, MediaType t) {
		for (MessageBodyWriter<?> m : writers.getOrDefault(t.getType() + "/" + t.getSubtype(), Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return (MessageBodyWriter<T>) m;
		}
		for (MessageBodyWriter<?> m : writers.getOrDefault(t.getType() + "/*", Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return (MessageBodyWriter<T>) m;
		}
		for (MessageBodyWriter<?> m : writers.getOrDefault("*/*", Collections.emptyList())) {
			if (!m.isWriteable(clazz, genericType, annotations, t))
				continue;
			// TODO priorities
			return (MessageBodyWriter<T>) m;
		}
		throw new InternalServerErrorException("No writer for " + clazz + " " + t);
	}

	public static void sendResponse(Response r, HttpServletResponse res) {
		res.setStatus(r.getStatus());

		MultivaluedMap<String, Object> headers = r.getHeaders();
		// limit header response to 8192

	}

	public static final ParamConverter<String> STRING = new ParamConverter<>() {
		@Override
		public String fromString(String value) {
			return value;
		}

		@Override
		public String toString(String value) {
			return value;
		}
	};
	public static final ParamConverter<Boolean> BOOLEAN = new ParamConverter<>() {
		@Override
		public Boolean fromString(String value) {
			return Boolean.parseBoolean(value);
		}

		@Override
		public String toString(Boolean value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Byte> BYTE = new ParamConverter<>() {
		@Override
		public Byte fromString(String value) {
			return Byte.parseByte(value);
		}

		@Override
		public String toString(Byte value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Short> SHORT = new ParamConverter<>() {
		@Override
		public Short fromString(String value) {
			return Short.parseShort(value);
		}

		@Override
		public String toString(Short value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Integer> INTEGER = new ParamConverter<>() {
		@Override
		public Integer fromString(String value) {
			return Integer.parseInt(value);
		}

		@Override
		public String toString(Integer value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Long> LONG = new ParamConverter<>() {
		@Override
		public Long fromString(String value) {
			return Long.parseLong(value);
		}

		@Override
		public String toString(Long value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Float> FLOAT = new ParamConverter<>() {
		@Override
		public Float fromString(String value) {
			return Float.parseFloat(value);
		}

		@Override
		public String toString(Float value) {
			return value.toString();
		}
	};
	public static final ParamConverter<Double> DOUBLE = new ParamConverter<>() {
		@Override
		public Double fromString(String value) {
			return Double.parseDouble(value);
		}

		@Override
		public String toString(Double value) {
			return value.toString();
		}
	};

	private static final class DefaultConvert implements ParamConverterProvider {
		@SuppressWarnings("unchecked")
		@Override
		public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
			if (rawType == String.class)
				return (ParamConverter<T>) STRING;
			if (rawType == Boolean.class || rawType == boolean.class)
				return (ParamConverter<T>) BOOLEAN;
			if (rawType == Byte.class || rawType == byte.class)
				return (ParamConverter<T>) BYTE;
			if (rawType == Short.class || rawType == short.class)
				return (ParamConverter<T>) SHORT;
			if (rawType == Integer.class || rawType == int.class)
				return (ParamConverter<T>) INTEGER;
			if (rawType == Long.class || rawType == long.class)
				return (ParamConverter<T>) LONG;
			if (rawType == Float.class || rawType == float.class)
				return (ParamConverter<T>) FLOAT;
			if (rawType == Double.class || rawType == double.class)
				return (ParamConverter<T>) DOUBLE;
			return null;
		}
	}

	private static class WebAppExceptionMapping implements ExceptionMapper<WebApplicationException> {

		@Override
		public Response toResponse(WebApplicationException e) {
			return e.getResponse();
		}
	}
}
