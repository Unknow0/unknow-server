/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.util.Set;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public interface XmlType {

	QName name();

	TypeModel type();

	void toString(StringBuilder sb, Set<XmlType> saw);
}
