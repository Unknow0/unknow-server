/**
 * 
 */
package unknow.server.maven.servlet.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.TypeModel;

/**
 * servlet of filter descriptor
 * 
 * @author unknow
 */
public class SD {
	public final int index;
	public String clazz;
	public String jsp;

	public String name;
	public final List<String> pattern = new ArrayList<>();
	public final Map<String, String> param = new HashMap<>();
	public int loadOnStartup = -1;
	public final List<String> servletNames = new ArrayList<>(0);
	public final List<DispatcherType> dispatcher = new ArrayList<>(0);
	public boolean enabled = true;

	public SD(int index) {
		this.index = index;
	}

	public SD(int index, AnnotationModel a, TypeModel e) {
		this.index = index;
		this.clazz = e.name();

		this.name = a.member("name").filter(v -> v.isSet()).or(() -> a.member("filterName")).map(v -> v.asLiteral()).orElse(e.name());
		a.member("value").filter(v -> v.isSet()).or(() -> a.member("urlPatterns")).ifPresent(v -> pattern.addAll(Arrays.asList(v.asArrayLiteral())));
		a.member("loadOnStartup").filter(v -> v.isSet()).ifPresent(v -> loadOnStartup = v.asInt());
		a.member("initParams").filter(v -> v.isSet()).map(v -> v.asArrayAnnotation()).ifPresent(v -> {
			for (int i = 0; i < v.length; i++) {
				String key = v[i].member("name").orElseThrow().asLiteral();
				String value = v[i].member("value").orElseThrow().asLiteral();
				param.put(key, value);
			}
		});
		a.member("servletNames").filter(v -> v.isSet()).ifPresent(v -> servletNames.addAll(Arrays.asList(v.asArrayLiteral())));
		a.member("dispatcherTypes").filter(v -> v.isSet()).map(v -> v.asArrayLiteral()).ifPresent(v -> {
			for (int i = 0; i < v.length; i++)
				dispatcher.add(DispatcherType.valueOf(v[i]));
		});
	}

	@Override
	public String toString() {
		return name == null ? clazz : name + (dispatcher.isEmpty() ? "" : dispatcher);
	}
}