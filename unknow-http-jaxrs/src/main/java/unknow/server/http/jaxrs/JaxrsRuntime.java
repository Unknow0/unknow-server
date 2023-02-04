/**
 * 
 */
package unknow.server.http.jaxrs;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.SeBootstrap.Configuration;
import jakarta.ws.rs.SeBootstrap.Instance;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant.VariantListBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;
import unknow.server.http.jaxrs.impl.DateDelegate;
import unknow.server.http.jaxrs.impl.MediaTypeDelegate;
import unknow.server.http.jaxrs.impl.NewCookieDelegate;
import unknow.server.http.jaxrs.impl.ResponseBuilderImpl;

/**
 * @author unknow
 */
public class JaxrsRuntime extends RuntimeDelegate {
	/*
	 * following values for type: {@link jakarta.ws.rs.core.CacheControl}, {@link jakarta.ws.rs.core.Cookie}, {@link jakarta.ws.rs.core.EntityTag}, {@link jakarta.ws.rs.core.Link}
	 */
	private static final Map<Class<?>, HeaderDelegate<?>> headers = new HashMap<>();
	static {
		headers.put(MediaType.class, MediaTypeDelegate.INSTANCE);
		headers.put(NewCookie.class, NewCookieDelegate.INSTANCE);
		headers.put(Date.class, DateDelegate.INSTANCE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String header(Object o) {
		if (o == null)
			return null;
		HeaderDelegate d = headers.get(o.getClass());
		return d == null ? o.toString() : d.toString(o);
	}

	@Override
	public UriBuilder createUriBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseBuilder createResponseBuilder() {
		return new ResponseBuilderImpl();
	}

	@Override
	public VariantListBuilder createVariantListBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T createEndpoint(Application application, Class<T> endpointType) throws IllegalArgumentException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) throws IllegalArgumentException {
		return (HeaderDelegate<T>) headers.get(type);
	}

	@Override
	public Link.Builder createLinkBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configuration.Builder createConfigurationBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletionStage<Instance> bootstrap(Application application, Configuration configuration) {
		CompletableFuture<Instance> a = new CompletableFuture<>();
		a.completeExceptionally(new UnsupportedOperationException());
		return a;
	}

	@Override
	public CompletionStage<Instance> bootstrap(Class<? extends Application> clazz, Configuration configuration) {
		CompletableFuture<Instance> a = new CompletableFuture<>();
		a.completeExceptionally(new UnsupportedOperationException());
		return a;
	}

	@Override
	public EntityPart.Builder createEntityPartBuilder(String partName) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

}
