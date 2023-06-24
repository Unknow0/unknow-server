/**
 * 
 */
package unknow.server.maven.servlet.sax;

import unknow.sax.SaxContext;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.servlet.descriptor.Descriptor;

/**
 * @author unknow
 */
public class Context extends SaxContext {
	public final Descriptor descriptor;
	public final ModelLoader loader;

	public Context(Descriptor descriptor, ModelLoader loader) {
		super(WebAppHandler.INSTANCE);
		this.descriptor = descriptor;
		this.loader = loader;
	}
}
