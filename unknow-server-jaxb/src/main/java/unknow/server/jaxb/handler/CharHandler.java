/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class CharHandler implements XmlSimpleHandler<Character> {

	public static final CharHandler INSTANCE = new CharHandler();

	private CharHandler() {
	}

	@Override
	public String toString(Character t) {
		return Character.toString(t);
	}

	@Override
	public Character toObject(String s) {
		return s.charAt(0);
	}
}
