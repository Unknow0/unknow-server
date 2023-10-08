/**
 * 
 */
package unknow.server.maven.model_xml;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public interface XmlType {

	default String ns() {
		return qname().getNamespaceURI();
	}

	default String name() {
		return qname().getLocalPart();
	}

	QName qname();

	TypeModel type();
}
