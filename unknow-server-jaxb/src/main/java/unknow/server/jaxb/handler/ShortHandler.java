/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class ShortHandler implements XmlSimpleHandler<Short> {

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
