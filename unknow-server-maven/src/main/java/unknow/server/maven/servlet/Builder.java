/**
 * 
 */
package unknow.server.maven.servlet;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import unknow.server.maven.TypeCache;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.servlet.descriptor.Descriptor;

/**
 * @author unknow
 */
public abstract class Builder {
	public abstract void add(BuilderContext ctx);

	public static interface BuilderContext {
		ClassOrInterfaceDeclaration self();

		Descriptor descriptor();

		TypeCache type();

		ModelLoader loader();

		String sessionFactory();
	}
}
