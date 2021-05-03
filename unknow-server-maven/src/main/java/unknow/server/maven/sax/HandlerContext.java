/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.XMLReader;

import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import unknow.server.maven.descriptor.Descriptor;

/**
 * @author unknow
 */
public class HandlerContext {
	public final StringBuilder sb;
	public final Descriptor descriptor;
	public final XMLReader reader;
	public final TypeSolver resolver;

	public HandlerContext(StringBuilder sb, Descriptor descriptor, XMLReader reader, TypeSolver resolver) {
		this.sb = sb;
		this.descriptor = descriptor;
		this.reader = reader;
		this.resolver = resolver;
	}
}
