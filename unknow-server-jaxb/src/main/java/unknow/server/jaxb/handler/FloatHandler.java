/**
 * 
 */
package unknow.server.jaxb.handler;

/**
 * @author unknow
 */
public class FloatHandler extends XmlDefaultHandler<Float> {

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
