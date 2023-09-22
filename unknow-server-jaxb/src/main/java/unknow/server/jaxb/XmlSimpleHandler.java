/**
 * 
 */
package unknow.server.jaxb;

import javax.xml.stream.XMLStreamException;

/**
 * @author unknow
 */
public interface XmlSimpleHandler<T> extends XmlHandler<T> {

	String toString(T t) throws XMLStreamException;

	T toObject(String s) throws XMLStreamException;
}
