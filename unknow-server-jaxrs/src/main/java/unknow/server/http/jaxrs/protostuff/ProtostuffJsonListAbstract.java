package unknow.server.http.jaxrs.protostuff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import io.protostuff.JsonXIOUtil2;
import io.protostuff.JsonXIOUtil2.ListFormat;
import io.protostuff.LinkedBuffer;
import io.protostuff.Message;
import io.protostuff.Schema;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

public abstract class ProtostuffJsonListAbstract<T extends Message<?>> extends ProtostuffListAbstract<T> {
	private final ListFormat fmt;

	protected ProtostuffJsonListAbstract(ListFormat fmt) {
		this.fmt = fmt;
	}

	@Override
	public final void writeTo(Collection<T> t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
			OutputStream out) throws IOException, WebApplicationException {
		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];
		Schema<T> schema = ProtostuffSchema.get(p);
		JsonXIOUtil2.writeListTo(out, t, schema, false, LinkedBuffer.allocate(4096), fmt);
	}

	@Override
	public final Collection<T> readFrom(Class<Collection<T>> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
			InputStream in) throws IOException, WebApplicationException {
		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];
		Schema<T> schema = ProtostuffSchema.get(p);
		return JsonXIOUtil2.parseListFrom(in, schema, false, fmt);
	}

	@Provider
	@Priority(4500)
	@Consumes({ "application/x-ndjson", "application/jsonl" })
	@Produces({ "application/x-ndjson", "application/jsonl" })
	public static class ProtostuffJsonLineProvider<T extends Message<?>> extends ProtostuffJsonListAbstract<T> {
		public ProtostuffJsonLineProvider() {
			super(ListFormat.NDJSON);
		}
	}

	@Provider
	@Priority(4500)
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	public static class ProtostuffJsonListProvider<T extends Message<?>> extends ProtostuffJsonListAbstract<T> {
		public ProtostuffJsonListProvider() {
			super(ListFormat.JSON);
		}
	}
}
