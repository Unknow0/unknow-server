package unknow.server.http.jaxrs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

public class MessageByte implements MessageRW<byte[]> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == byte[].class;
	}

	@Override
	public void writeTo(byte[] t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException, WebApplicationException {
		entityStream.write(t);
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == byte[].class;
	}

	@Override
	public byte[] readFrom(Class<byte[]> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
			InputStream entityStream) throws IOException, WebApplicationException {
		byte[] b = new byte[4096];
		int l;
		int o = 0;
		while ((l = entityStream.read(b, o, b.length - o)) > 0) {
			o += l;
			if (o == b.length)
				b = Arrays.copyOf(b, b.length + 4096);
		}

		return o == b.length ? b : Arrays.copyOf(b, o);
	}
}
