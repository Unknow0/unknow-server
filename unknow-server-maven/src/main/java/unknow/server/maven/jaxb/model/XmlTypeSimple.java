/**
 * 
 */
package unknow.server.maven.jaxb.model;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlTypeSimple implements XmlType {
	private final QName qname;
	private final TypeModel type;

	/**
	 * create new XmlTypeSimple
	 * 
	 * @param qname
	 * @param type
	 */
	public XmlTypeSimple(QName qname, TypeModel type) {
		this.qname = qname;
		this.type = type;
	}

	@Override
	public QName qname() {
		return qname;
	}
	
	@Override
	public TypeModel type() {
		return type;
	}
}
