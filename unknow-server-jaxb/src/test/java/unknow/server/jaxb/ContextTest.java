/**
 * 
 */
package unknow.server.jaxb;

import java.util.Collection;
import java.util.Collections;

/**
 * @author unknow
 */
public class ContextTest extends ContextFactory {

	public ContextTest() {
		register(O.class, new OHandler());
	}

	@Override
	protected Collection<Class<?>> getClasses(String contextPackage) {
		return Collections.emptyList();
	}

}
