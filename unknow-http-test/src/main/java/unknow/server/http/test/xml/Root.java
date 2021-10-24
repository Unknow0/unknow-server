/**
 * 
 */
package unknow.server.http.test.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "webservice.unknow", name = "Root")
public class Root {
	@XmlAttribute
	private int value;

	@XmlElement
	private String elem;

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getElem() {
		return elem;
	}

	public void setElem(String elem) {
		this.elem = elem;
	}

	@Override
	public String toString() {
		return "Root [value=" + value + ", elem=" + elem + "]";
	}
}