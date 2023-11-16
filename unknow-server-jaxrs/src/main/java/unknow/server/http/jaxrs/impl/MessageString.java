package unknow.server.http.jaxrs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

public class MessageString implements MessageRW<String> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == String.class;
	}

	@Override
	public void writeTo(String t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException, WebApplicationException {
		entityStream.write(t.getBytes(mediaType.getParameters().getOrDefault("charset", "utf8")));
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == String.class;
	}

	@Override
	public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
			InputStream entityStream) throws IOException, WebApplicationException {
		char[] c = new char[1024];
		StringBuilder sb = new StringBuilder();
		try (Reader r = new InputStreamReader(entityStream, mediaType.getParameters().getOrDefault("charset", "utf8"))) {
			int l;
			while ((l = r.read(c)) > 0)
				sb.append(c, 0, l);
		}
		return sb.toString();
	}

}
