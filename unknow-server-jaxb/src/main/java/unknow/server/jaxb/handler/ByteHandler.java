/**
 * 
 */
package unknow.server.jaxb.handler;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class ByteHandler implements XmlSimpleHandler<Byte> {

	public static final ByteHandler INSTANCE = new ByteHandler();

	private ByteHandler() {
	}

	@Override
	public String toString(Byte t) {
		return Byte.toString(t);
	}

	@Override
	public Byte toObject(String s) {
		return Byte.parseByte(s);
	}

}
