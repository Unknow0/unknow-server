package unknow.server.maven.jaxws.binding;

import javax.xml.namespace.QName;

import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model_xml.XmlType;

public class Parameter {
	public final QName name;
	public final TypeModel type;
	public final XmlType xml;
	public boolean header;

	public Parameter(QName name, TypeModel type, XmlType xml, boolean header) {
		this.name = name;
		this.type = type;
		this.xml = xml;
		this.header = header;
	}
}
