/**
 * 
 */
package unknow.server.jaxb.handler;

/**
 * @author unknow
 */
public class ShortHandler extends XmlDefaultHandler<Short> {

	public static final ShortHandler INSTANCE = new ShortHandler();

	private ShortHandler() {
	}

	@Override
	public String toString(Short t) {
		return Short.toString(t);
	}

	@Override
	public Short toObject(String s) {
		return Short.parseShort(s);
	}

}
