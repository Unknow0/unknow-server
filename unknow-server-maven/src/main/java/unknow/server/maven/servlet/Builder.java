/**
 * 
 */
package unknow.server.maven.servlet;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import unknow.maven.codegen.TypeFactory;
import unknow.model.api.ModelLoader;
import unknow.server.maven.servlet.descriptor.Descriptor;

/**
 * @author unknow
 */
public abstract class Builder {
	public abstract void add(BuilderContext ctx);

	public static interface BuilderContext {
		ClassOrInterfaceDeclaration self();

		ClassLoader classLoader();

		Descriptor descriptor();

		TypeFactory type();

		ModelLoader loader();

		String sessionFactory();
	}
}
