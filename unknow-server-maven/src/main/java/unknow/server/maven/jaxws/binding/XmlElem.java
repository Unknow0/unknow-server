/**
 * 
 */
package unknow.server.maven.jaxws.binding;

/**
 * @author unknow
 */
public class XmlElem<T extends XmlType<?>> {
	private final T xmlType;
	private final String ns;
	private final String name;

	private int minOccur = 1;
	private int maxOccur = 1;

	public XmlElem(T xmlType, String ns, String name) {
		this.xmlType = xmlType;
		this.ns = ns;
		this.name = name;
	}

	public T type() {
		return xmlType;
	}

	public String ns() {
		return ns;
	}

	public String name() {
		return name;
	}

	public String qname() {
		return ns.isEmpty() ? name : "{" + ns + "}" + name;
	}
}
