/**
 * 
 */
package unknow.server.http.jaxrs;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.SeBootstrap.Configuration;
import jakarta.ws.rs.SeBootstrap.Instance;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant.VariantListBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;
import unknow.server.http.jaxrs.builder.ConfigurationBuilderImpl;
import unknow.server.http.jaxrs.builder.LinkBuilderImpl;
import unknow.server.http.jaxrs.builder.ResponseBuilderImpl;
import unknow.server.http.jaxrs.builder.UriBuilderImpl;
import unknow.server.http.jaxrs.builder.VariantListBuilderImpl;
import unknow.server.http.jaxrs.header.CacheControlDelegate;
import unknow.server.http.jaxrs.header.CookieDelegate;
import unknow.server.http.jaxrs.header.DateDelegate;
import unknow.server.http.jaxrs.header.EntityTagDelegate;
import unknow.server.http.jaxrs.header.LinkDelegate;
import unknow.server.http.jaxrs.header.MediaTypeDelegate;
import unknow.server.http.jaxrs.header.NewCookieDelegate;

/**
 * @author unknow
 */
public class JaxrsRuntime extends RuntimeDelegate {
	/*
	 * {@link jakarta.ws.rs.core.EntityTag} , {@link jakarta.ws.rs.core.Link}
	 */
	private static final Map<Class<?>, HeaderDelegate<?>> headers = new HashMap<>();
	static {
		headers.put(CacheControl.class, CacheControlDelegate.INSTANCE);
		headers.put(Cookie.class, CookieDelegate.INSTANCE);
		headers.put(Date.class, DateDelegate.INSTANCE);
		headers.put(EntityTag.class, EntityTagDelegate.INSTANCE);
		headers.put(Link.class, LinkDelegate.INSTANCE);
		headers.put(MediaType.class, MediaTypeDelegate.INSTANCE);
		headers.put(NewCookie.class, NewCookieDelegate.INSTANCE);
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
		return new UriBuilderImpl();
	}

	@Override
	public ResponseBuilder createResponseBuilder() {
		return new ResponseBuilderImpl();
	}

	@Override
	public VariantListBuilder createVariantListBuilder() {
		return new VariantListBuilderImpl();
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
		return new LinkBuilderImpl();
	}

	@Override
	public Configuration.Builder createConfigurationBuilder() {
		return new ConfigurationBuilderImpl();
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
