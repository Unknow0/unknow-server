/**
 * 
 */
package unknow.server.http.test;

import static javax.jws.soap.SOAPBinding.ParameterStyle.BARE;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * @author unknow
 */
@WebService(targetNamespace = "unknow.test")
public class Webservice {

	@WebMethod
	public String echo(@WebParam(header = true) String content, int value) {
		return content + value;
	}

	@WebMethod
	@SOAPBinding(parameterStyle = BARE)
	public Root echo(@WebParam(targetNamespace = "webservice.unknow", name = "Root") Root root) {
		return root;
	}

	@WebMethod
	@SOAPBinding(parameterStyle = ParameterStyle.WRAPPED)
	public Root copy(@WebParam(targetNamespace = "webservice.unknow", name = "Root") Root root) {
		return root;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(namespace = "webservice.unknow", name = "Root")
	public static class Root {
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
	}
}
