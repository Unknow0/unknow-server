/**
 * 
 */
package unknow.server.http.test.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
@XmlType(namespace = "http://webservice.unknow", name = "Root")
public class Root {
	@XmlAttribute
	private int value;

	@XmlElement
	private List<String> elem;

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public List<String> getElem() {
		return elem;
	}

	public void setElem(List<String> elem) {
		this.elem = elem;
	}

	@Override
	public String toString() {
		return "Root [value=" + value + ", elem=" + elem + "]";
	}
}