/**
 * 
 */
package unknow.server.http.test.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
@XmlType(namespace = Root.NS, name = "Root")
public class Root {
	public static final String NS = "http://webservice.unknow";
	@XmlAttribute
	private String value;

	@XmlElements({ @XmlElement(name = "s", type = String.class), @XmlElement(name = "elem", type = String.class), @XmlElement(name = "mixed", type = Mixed.class) })
	private List<Object> elems;

	@XmlElements({ @XmlElement(name = "a", type = String.class), @XmlElement(name = "c", type = String.class), @XmlElement(name = "c", type = Mixed.class) })
	private Object choice;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<Object> getElems() {
		return elems;
	}

	public void setElems(List<Object> elem) {
		this.elems = elem;
	}

	public Object getChoice() {
		return choice;
	}

	public void setChoice(Object choice) {
		this.choice = choice;
	}

	@Override
	public String toString() {
		return "Root [value=" + value + ", elem=" + elems + "]";
	}
}