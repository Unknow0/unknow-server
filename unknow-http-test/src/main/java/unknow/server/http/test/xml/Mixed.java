/**
 * 
 */
package unknow.server.http.test.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Mixed {
	@XmlValue
	private int value;
	@XmlAttribute
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
		return "Mixed [value=" + value + ", elem=" + elem + "]";
	}
}