/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class IntHandler implements XmlSimpleHandler<Integer> {

	public static final IntHandler INSTANCE = new IntHandler();

	private IntHandler() {
	}

	@Override
	public Class<Integer> clazz() {
		return Integer.class;
	}

	@Override
	public String toString(Integer t) {
		return Integer.toString(t);
	}

	@Override
	public Integer toObject(String s) {
		return Integer.parseInt(s);
	}
}
