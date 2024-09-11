/**
 * 
 */
package unknow.server.http.test.xml;

import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * @author unknow
 */
@XmlType(propOrder = { "a", "b", "e", "loop", "mapField" }, namespace = "unknow.test.xml")
@XmlRootElement(namespace = "http://test.unknow", name = "complex")
@XmlAccessorType(XmlAccessType.FIELD)
public class Complex {
	@XmlEnum
	@XmlType(namespace = "unknow.test.xml")
	public enum E {
		@XmlEnumValue("a")
		A, @XmlEnumValue("b")
		B
	}

	private E[] e;

	private String a;
	private String b;

	@XmlAnyAttribute
	private Map<QName, String> otherAttr;

	private Map<String, String> mapField;

	private Collection<Complex> loop;

	public E[] getE() {
		return e;
	}

	public void setE(E[] e) {
		this.e = e;
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

	public String getB() {
		return b;
	}

	public void setB(String b) {
		this.b = b;
	}

	public Map<QName, String> getOtherAttr() {
		return otherAttr;
	}

	public void setOtherAttr(Map<QName, String> otherAttr) {
		this.otherAttr = otherAttr;
	}

	public Map<String, String> getMapField() {
		return mapField;
	}

	public void setMapField(Map<String, String> mapField) {
		this.mapField = mapField;
	}

	public Collection<Complex> getLoop() {
		return loop;
	}

	public void setLoop(Collection<Complex> loop) {
		this.loop = loop;
	}

}
