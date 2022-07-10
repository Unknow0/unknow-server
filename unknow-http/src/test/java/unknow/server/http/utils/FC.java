/**
 * 
 */
package unknow.server.http.utils;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import unknow.server.http.utils.PathTree.Node;
import unknow.server.http.utils.PathTree.PartNode;

public class FC implements FilterChain {
	private final String name;

	public FC(String name) {
		this.name = name;
	}

	public PartNode part() {
		return new PartNode(name, null, null, null, this, this);
	}

	public Node node() {
		return new Node(name, this);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
	}
}