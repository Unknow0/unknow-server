/**
 * 
 */
package unknow.server.maven.sax;

import org.xml.sax.XMLReader;

import unknow.server.maven.descriptor.Descriptor;

/**
 * @author unknow
 */
public class HandlerContext {
	public final StringBuilder sb;
	public final Descriptor descriptor;
	public final XMLReader reader;

	public HandlerContext(StringBuilder sb, Descriptor descriptor, XMLReader reader) {
		this.sb = sb;
		this.descriptor = descriptor;
		this.reader = reader;
	}
}
