/**
 * 
 */
package unknow.server.jaxb.handler;

import java.math.BigInteger;

import unknow.server.jaxb.XmlSimpleHandler;

/**
 * @author unknow
 */
public class BigIntegerHandler implements XmlSimpleHandler<BigInteger> {

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
