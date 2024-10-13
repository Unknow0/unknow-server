//package io.protostuff;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.lang.annotation.Annotation;
//import java.lang.reflect.ParameterizedType;
//import java.lang.reflect.Type;
//import java.util.Collection;
//
//import com.fasterxml.jackson.core.JsonParser;
//
//import jakarta.annotation.Priority;
//import jakarta.ws.rs.Consumes;
//import jakarta.ws.rs.Produces;
//import jakarta.ws.rs.WebApplicationException;
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.core.MultivaluedMap;
//import jakarta.ws.rs.ext.Provider;
//import unknow.server.http.jaxrs.protostuff.ProtostuffListAbstract;
//import unknow.server.http.jaxrs.protostuff.ProtostuffSchema;
//
//@Provider
//@Priority(4500)
//@Consumes({ "application/x-ndjson", "application/jsonl" })
//@Produces({ "application/x-ndjson", "application/jsonl" })
//public class ProtostuffJsonLineProvider<T extends Message<?>> extends ProtostuffListAbstract<T> {
//	private static final byte[] EMPTY = {};
//	private static final byte[] LF = { '\n' };
//
//	@Override
//	public final void writeTo(Collection<T> t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
//			OutputStream out) throws IOException, WebApplicationException {
//		if (t.isEmpty())
//			return;
//		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];
//		Schema<T> schema = ProtostuffSchema.get(p);
//		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
//		JsonXIOUtil2.writeListTo(out, t, schema, false, buffer, EMPTY, LF, EMPTY);
//	}
//}
