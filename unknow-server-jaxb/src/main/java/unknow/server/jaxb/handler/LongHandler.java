/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class LongHandler implements XmlSimpleHandler<Long> {

	public static final LongHandler INSTANCE = new LongHandler();

	private LongHandler() {
	}

	@Override
	public String toString(Long t) {
		return Long.toString(t);
	}

	@Override
	public Long toObject(String s) {
		return Long.parseLong(s);
	}
}
