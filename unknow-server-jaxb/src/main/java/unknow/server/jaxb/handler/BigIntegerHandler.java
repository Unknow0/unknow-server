/**
 * 
 */
package unknow.server.jaxb.handler;

import java.math.BigInteger;

/**
 * @author unknow
 */
public class BigIntegerHandler extends XmlDefaultHandler<BigInteger> {

	public static final BigIntegerHandler INSTANCE = new BigIntegerHandler();

	private BigIntegerHandler() {
	}

	@Override
	public String toString(BigInteger t) {
		return t.toString();
	}

	@Override
	public BigInteger toObject(String s) {
		return new BigInteger(s);
	}

}
