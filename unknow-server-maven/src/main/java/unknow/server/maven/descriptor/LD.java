/**
 * 
 */
package unknow.server.maven.descriptor;

import java.util.Set;

public class LD {
	public final String clazz;
	public final Set<Class<?>> listener;

	public LD(String clazz, Set<Class<?>> listener) {
		this.clazz = clazz;
		this.listener = listener;
	}
}