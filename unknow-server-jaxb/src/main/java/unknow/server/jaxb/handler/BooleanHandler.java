/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class BooleanHandler implements XmlSimpleHandler<Boolean> {

	public static final BooleanHandler INSTANCE = new BooleanHandler();

	private BooleanHandler() {
	}

	@Override
	public String toString(Boolean t) {
		return Boolean.toString(t);
	}

	@Override
	public Boolean toObject(String s) { // TODO validation
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

}
