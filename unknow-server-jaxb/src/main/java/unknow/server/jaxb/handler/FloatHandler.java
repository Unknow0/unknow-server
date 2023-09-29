/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class FloatHandler implements XmlSimpleHandler<Float> {

	public static final FloatHandler INSTANCE = new FloatHandler();

	private FloatHandler() {
	}

	@Override
	public String toString(Float t) {
		return Float.toString(t);
	}

	@Override
	public Float toObject(String s) {
		return Float.parseFloat(s);
	}

}
