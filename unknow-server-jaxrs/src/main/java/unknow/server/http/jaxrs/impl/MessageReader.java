package unknow.server.http.jaxrs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

public class MessageReader implements MessageRW<Reader> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return Reader.class.isAssignableFrom(type);
	}

	@Override
	public void writeTo(Reader t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException, WebApplicationException {
		try (Writer w = new OutputStreamWriter(entityStream, mediaType.getParameters().getOrDefault("charset", "utf8"))) {
			char[] b = new char[1024];
			int l;
			while ((l = t.read(b)) > 0)
				w.write(b, 0, l);
		}
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return Reader.class.isAssignableFrom(type);
	}

	@Override
	public Reader readFrom(Class<Reader> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
			InputStream entityStream) throws IOException, WebApplicationException {
		return new InputStreamReader(entityStream, mediaType.getParameters().getOrDefault("charset", "utf8"));
	}
}
