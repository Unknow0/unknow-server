/**
 * 
 */
package unknow.server.jaxb;

import javax.xml.namespace.QName;

/**
 * @author unknow
 * @param <T> content type
 */
public abstract class XmlRootHandler<T> implements XmlHandler<T> {

	private final QName qname;

	/**
	 * create new XmlRootHandler
	 * 
	 * @param qname
	 */
	protected XmlRootHandler(QName qname) {
		this.qname = qname;
	}

	/**
	 * @return element qname
	 */
	public final QName qname() {
		return qname;
	}

	public final String localName() {
		return qname.getLocalPart();
	}

	public final String ns() {
		return qname.getNamespaceURI();
	}
}
