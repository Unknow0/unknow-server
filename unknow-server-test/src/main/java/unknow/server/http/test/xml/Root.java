/**
 * 
 */
package unknow.server.http.test.xml;

import java.time.LocalDate;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
@XmlType(namespace = Root.NS, name = "Root")
public class Root {
	public static final String NS = "http://webservice.unknow";
	@XmlAttribute
	private LocalDate value;

	@XmlElement
	private List<String> elem;

	public LocalDate getValue() {
		return value;
	}

	public void setValue(LocalDate value) {
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