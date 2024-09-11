/**
 * 
 */
package unknow.server.http.test;

import static jakarta.jws.soap.SOAPBinding.ParameterStyle.BARE;

import java.util.Collections;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.jws.soap.SOAPBinding.ParameterStyle;
import unknow.server.http.test.xml.Complex;
import unknow.server.http.test.xml.Mixed;
import unknow.server.http.test.xml.Root;

/**
 * @author unknow
 */
@SuppressWarnings("unused")
@WebService(targetNamespace = "http://test.unknow", serviceName = "ws")
public class Webservice {

	@WebMethod
	@SOAPBinding(parameterStyle = BARE)
	public Root bare(@WebParam(targetNamespace = "http://webservice.unknow", name = "Root") Root root) {
		return root;
	}

	@WebMethod
	@SOAPBinding(parameterStyle = ParameterStyle.WRAPPED)
	public Root wrapped(@WebParam(targetNamespace = "http://webservice.unknow", name = "Root") Root root) {
		return root;
	}

	@WebMethod(exclude = true)
	@SOAPBinding(parameterStyle = ParameterStyle.BARE)
	public Mixed mixed(@WebParam(targetNamespace = "http://webservice.unknow", name = "Mixed") Mixed root) {
		return root;
	}

	@WebMethod
	@SOAPBinding(parameterStyle = ParameterStyle.WRAPPED)
	public void onWayWrapped(@WebParam String root) { // OK
	}

	@WebMethod
	@SOAPBinding(parameterStyle = ParameterStyle.BARE)
	public void onWayBare(@WebParam String root) { // OK
	}

	@WebMethod
	public Complex complex(Complex param) {
		param.setMapField(Collections.singletonMap("Key", "value"));
		return param;
	}
}
