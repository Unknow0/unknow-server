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

import unknow.server.http.test.xml.Mixed;
import unknow.server.http.test.xml.Root;

/**
 * @author unknow
 */
@WebService(targetNamespace = "http://test.unknow", name = "ws")
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
	public void onWayWrapped(@WebParam String root) {
	}

	@WebMethod
	@SOAPBinding(parameterStyle = ParameterStyle.BARE)
	public void onWayBare(@WebParam String root) {
	}
}
