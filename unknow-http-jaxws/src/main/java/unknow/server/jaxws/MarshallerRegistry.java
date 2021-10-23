/**
 * 
 */
package unknow.server.jaxws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author unknow
 */
public class MarshallerRegistry {

	private final Map<Class<?>, Marshaller<?>> marshalers = new HashMap<>();

	public MarshallerRegistry() {
		register(Envelope.class, Marshaller.ENVELOPE);
		register(OperationWrapper.class, Marshaller.OPERATION);
		register(Element.class, Marshaller.ELEMENT);
	}

	public <T> void register(Class<T> clazz, Marshaller<T> m) {
		marshalers.put(clazz, m);
	}

	public <T> Marshaller<T> get(Class<T> clazz) throws IOException {
		@SuppressWarnings("unchecked") Marshaller<T> m = (Marshaller<T>) marshalers.get(clazz);
		if (m == null)
			throw new IOException("not marshaler found for '" + clazz + "'");
		return m;
	}

}
