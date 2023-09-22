/**
 * 
 */
package unknow.server.jaxb.handler;

/**
 * @author unknow
 */
public class DoubleHandler extends XmlDefaultHandler<Double> {

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
