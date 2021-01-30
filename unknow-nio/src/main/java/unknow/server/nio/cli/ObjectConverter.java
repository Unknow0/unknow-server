/**
 * 
 */
package unknow.server.nio.cli;

import picocli.CommandLine.ITypeConverter;

/**
 * @author unknow
 */
public class ObjectConverter implements ITypeConverter<Object> {
	@Override
	public Object convert(String value) throws Exception {
		return Class.forName(value).newInstance();
	}
}
