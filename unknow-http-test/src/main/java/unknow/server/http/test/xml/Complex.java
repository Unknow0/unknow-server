/**
 * 
 */
package unknow.server.http.test.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * @author unknow
 */
@XmlType(propOrder = { "a", "b", "e", "loop" }, namespace = "unknow.test.xml")
public class Complex {
	@XmlEnum
	public enum E {
		@XmlEnumValue("a")
		_A,
		@XmlEnumValue("b")
		_B
	}

	private E e;

	private String a;
	private String b;

	private Complex loop;

	public E getE() {
		return e;
	}

	public void setE(E e) {
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

	public Complex getLoop() {
		return loop;
	}

	public void setLoop(Complex loop) {
		this.loop = loop;
	}

}
