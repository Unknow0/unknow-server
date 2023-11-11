/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class DoubleHandler implements XmlSimpleHandler<Double> {

	public static final DoubleHandler INSTANCE = new DoubleHandler();

	private DoubleHandler() {
	}

	@Override
	public String toString(Double t) {
		return Double.toString(t);
	}

	@Override
	public Double toObject(String s) {
		return Double.parseDouble(s);
	}

}
