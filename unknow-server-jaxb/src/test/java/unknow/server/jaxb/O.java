/**
 * 
 */
package unknow.server.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * @author unknow
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class O {
	@XmlAttribute
	public int a;
	@XmlValue
	public String v;
	@XmlElement(name = "a")
	public O o;
}
