/**
 * 
 */
package unknow.server.maven.jaxws.model;

/**
 * @author unknow
 */
public class SchemaData {
	public final String name;
	public final String ns;

	public final String rootElement;
	public final String rootNs;

	public SchemaData(String name, String ns, String rootElement, String rootNs) {
		this.name = name;
		this.ns = ns;
		this.rootElement = rootElement;
		this.rootNs = rootNs;
	}
}
