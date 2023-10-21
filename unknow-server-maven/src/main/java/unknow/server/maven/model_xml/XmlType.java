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

	QName name();

	TypeModel type();
}
