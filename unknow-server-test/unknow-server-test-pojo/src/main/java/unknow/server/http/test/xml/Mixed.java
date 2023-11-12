/**
 * 
 */
package unknow.server.http.test.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
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