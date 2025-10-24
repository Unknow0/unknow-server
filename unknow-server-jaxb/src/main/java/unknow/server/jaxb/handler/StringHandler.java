/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public final class StringHandler implements XmlSimpleHandler<String> {

	public static final StringHandler INSTANCE = new StringHandler();

	private StringHandler() {
	}

	@Override
	public Class<String> clazz() {
		return String.class;
	}

	@Override
	public final String toString(String t) {
		return t;
	}

	@Override
	public final String toObject(String s) {
		return s;
	}
}
