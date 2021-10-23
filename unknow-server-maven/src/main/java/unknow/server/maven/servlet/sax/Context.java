/**
 * 
 */
package unknow.server.maven.servlet.sax;

import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import unknow.sax.SaxContext;
import unknow.server.maven.servlet.descriptor.Descriptor;

/**
 * @author unknow
 */
public class Context extends SaxContext {
	public final Descriptor descriptor;
	public final TypeSolver resolver;

	public Context(Descriptor descriptor, TypeSolver resolver) {
		super(WebAppHandler.INSTANCE);
		this.descriptor = descriptor;
		this.resolver = resolver;
	}
}
