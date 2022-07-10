/**
 * 
 */
package unknow.server.http.utils;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author unknow
 */
public class F implements Filter {
	private final String name;

	public F(String name) {
		this.name = name;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
	}

	@Override
	public String toString() {
		return name;
	}
}
