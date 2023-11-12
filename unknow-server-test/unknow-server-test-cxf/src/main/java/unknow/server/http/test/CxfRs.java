/**
 * 
 */
package unknow.server.http.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

/**
 * @author unknow
 */
@WebServlet(value = "/*")
public class CxfRs extends CXFNonSpringJaxrsServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected List<?> getProviders(ServletConfig servletConfig, String splitChar) throws ServletException {
		return Arrays.asList(new JacksonJsonProvider());
	}

	@Override
	protected List<? extends Feature> getFeatures(ServletConfig servletConfig, String splitChar) throws ServletException {
		return Arrays.asList(new OpenApiFeature());
	}

	@Override
	protected Map<Class<?>, Map<String, List<String>>> getServiceClasses(ServletConfig servletConfig, boolean modelAvailable, String splitChar) throws ServletException {
		return Collections.singletonMap(Rest.class, Collections.emptyMap());
	}
}
