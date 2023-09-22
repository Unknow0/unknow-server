/**
 * 
 */
package unknow.server.jaxb.handler;

import java.math.BigDecimal;

/**
 * @author unknow
 */
public class BigDecimalHandler extends XmlDefaultHandler<BigDecimal> {

	public static final BigDecimalHandler INSTANCE = new BigDecimalHandler();

	private BigDecimalHandler() {
	}

	@Override
	public String toString(BigDecimal t) {
		return t.toString();
	}

	@Override
	public BigDecimal toObject(String s) {
		return new BigDecimal(s);
	}
}
