/**
 * 
 */
package unknow.server.http.utils;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

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
