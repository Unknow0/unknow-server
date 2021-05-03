/**
 * 
 */
package unknow.server.nio.cli;

import java.lang.reflect.InvocationTargetException;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;
import unknow.server.nio.NIOServerListener;

/**
 * @author unknow
 */
public class NIOListenerConverter implements ITypeConverter<NIOServerListener> {
	@Override
	public NIOServerListener convert(String value) throws Exception {
		String[] split = value.split(";");
		if (split.length == 1)
			return get(split[0]);
		NIOServerListener[] listeners = new NIOServerListener[split.length];
		for (int i = 0; i < split.length; i++)
			listeners[i] = get(split[i]);
		return new NIOServerListener.Composite(listeners);
	}

	private static NIOServerListener get(String s) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if ("NOP".equals(s))
			return NIOServerListener.NOP;
		if ("LOG".equals(s))
			return NIOServerListener.LOG;
		Class<?> forName = Class.forName(s);
		if (!NIOServerListener.class.isAssignableFrom(forName))
			throw new TypeConversionException("class '" + forName + "' doesn't implements NIOServerListener");
		return (NIOServerListener) forName.getDeclaredConstructor().newInstance();
	}
}
