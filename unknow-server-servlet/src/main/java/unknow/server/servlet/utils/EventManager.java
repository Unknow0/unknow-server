/**
 * 
 */
package unknow.server.servlet.utils;

import java.util.EventListener;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeEvent;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestAttributeEvent;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

/**
 * @author unknow
 */
public class EventManager {
	private static final Logger logger = LoggerFactory.getLogger(EventManager.class);

	private final List<ServletContextListener> contextListeners;
	private final List<ServletContextAttributeListener> contextAttributeListeners;
	private final List<ServletRequestListener> requestListeners;
	private final List<ServletRequestAttributeListener> requestAttributeListeners;
	private final List<HttpSessionListener> sessionListeners;
	private final List<HttpSessionAttributeListener> sessionAttributeListeners;
	private final List<HttpSessionIdListener> sessionIdListeners;

	private ServletContext ctx;

	/**
	 * create new EventManager
	 * 
	 * @param contextListeners the context listeners
	 * @param contextAttributeListeners the attribute listeners
	 * @param requestListeners the request listeners
	 * @param requestAttributeListeners the request attribute listeners
	 * @param sessionListeners the session listener
	 * @param sessionAttributeListeners the session attribute listeners
	 * @param sessionIdListeners the sessionId listeners
	 */
	public EventManager(List<ServletContextListener> contextListeners, List<ServletContextAttributeListener> contextAttributeListeners,
			List<ServletRequestListener> requestListeners, List<ServletRequestAttributeListener> requestAttributeListeners, List<HttpSessionListener> sessionListeners,
			List<HttpSessionAttributeListener> sessionAttributeListeners, List<HttpSessionIdListener> sessionIdListeners) {
		this.contextListeners = contextListeners;
		this.contextAttributeListeners = contextAttributeListeners;
		this.requestListeners = requestListeners;
		this.requestAttributeListeners = requestAttributeListeners;
		this.sessionListeners = sessionListeners;
		this.sessionAttributeListeners = sessionAttributeListeners;
		this.sessionIdListeners = sessionIdListeners;
	}

	private static final void error(EventListener l, Exception e) {
		logger.error("failed to notify {}", l, e);
	}

	/**
	 * notify of the context initialization
	 * 
	 * @param context the context
	 */
	public void fireContextInitialized(ServletContext context) {
		if (ctx != null)
			throw new IllegalStateException("context already initialized");
		this.ctx = context;
		ServletContextEvent e = new ServletContextEvent(context);
		for (ServletContextListener l : contextListeners) {
			try {
				l.contextInitialized(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	/**
	 * notify the context destruction
	 */
	public void fireContextDestroyed() {
		if (ctx == null)
			throw new IllegalStateException("context wasn't initialized");

		ServletContextEvent e = new ServletContextEvent(ctx);
		for (ServletContextListener l : contextListeners) {
			try {
				l.contextDestroyed(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	/**
	 * fire a change in the context attribute
	 * 
	 * @param key     the key that changed
	 * @param value   the new value
	 * @param old     the old value
	 */
	public void fireContextAttribute(String key, Object value, Object old) {
		if (contextAttributeListeners.isEmpty())
			return;
		if (value == null)
			fireContextAttributeRemoved(new ServletContextAttributeEvent(ctx, key, old));
		else if (old == null)
			fireContextAttributeAdded(new ServletContextAttributeEvent(ctx, key, value));
		else
			fireContextAttributeReplaced(new ServletContextAttributeEvent(ctx, key, old));
	}

	private void fireContextAttributeRemoved(ServletContextAttributeEvent e) {
		for (ServletContextAttributeListener l : contextAttributeListeners) {
			try {
				l.attributeRemoved(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	private void fireContextAttributeAdded(ServletContextAttributeEvent e) {
		for (ServletContextAttributeListener l : contextAttributeListeners) {
			try {
				l.attributeAdded(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	private void fireContextAttributeReplaced(ServletContextAttributeEvent e) {
		for (ServletContextAttributeListener l : contextAttributeListeners) {
			try {
				l.attributeReplaced(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	/**
	 * fire a request initialization
	 * 
	 * @param req the request
	 */
	public void fireRequestInitialized(ServletRequest req) {
		ServletRequestEvent e = new ServletRequestEvent(ctx, req);
		for (ServletRequestListener l : requestListeners) {
			try {
				l.requestInitialized(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	/**
	 * fire a request destruction
	 * 
	 * @param req the request
	 */
	public void fireRequestDestroyed(ServletRequest req) {
		ServletRequestEvent e = new ServletRequestEvent(ctx, req);
		for (ServletRequestListener l : requestListeners) {
			try {
				l.requestDestroyed(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	/**
	 * fire a change in a request attribute
	 * 
	 * @param req   the request
	 * @param key   the key that changed
	 * @param value the new value
	 * @param old   the old value
	 */
	public void fireRequestAttribute(ServletRequest req, String key, Object value, Object old) {
		if (contextAttributeListeners.isEmpty())
			return;
		if (value == null)
			fireRequestAttributeRemoved(new ServletRequestAttributeEvent(ctx, req, key, old));
		else if (old == null)
			fireRequestAttributeAdded(new ServletRequestAttributeEvent(ctx, req, key, value));
		else
			fireRequestAttributeReplaced(new ServletRequestAttributeEvent(ctx, req, key, old));
	}

	private void fireRequestAttributeRemoved(ServletRequestAttributeEvent e) {
		for (ServletRequestAttributeListener l : requestAttributeListeners) {
			try {
				l.attributeRemoved(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	private void fireRequestAttributeAdded(ServletRequestAttributeEvent e) {
		for (ServletRequestAttributeListener l : requestAttributeListeners) {
			try {
				l.attributeAdded(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	private void fireRequestAttributeReplaced(ServletRequestAttributeEvent e) {
		for (ServletRequestAttributeListener l : requestAttributeListeners) {
			try {
				l.attributeReplaced(e);
			} catch (Exception ex) {
				error(l, ex);
			}
		}
	}

	/**
	 * add a listener
	 * @param <T> listeners type
	 * @param t the listener
	 */
	public <T extends EventListener> void addListener(T t) {
		if (t instanceof ServletContextListener)
			contextListeners.add((ServletContextListener) t);
		if (t instanceof ServletContextAttributeListener)
			contextAttributeListeners.add((ServletContextAttributeListener) t);
		if (t instanceof ServletRequestListener)
			requestListeners.add((ServletRequestListener) t);
		if (t instanceof ServletRequestAttributeListener)
			requestAttributeListeners.add((ServletRequestAttributeListener) t);
		if (t instanceof HttpSessionListener)
			sessionListeners.add((HttpSessionListener) t);
		if (t instanceof HttpSessionAttributeListener)
			sessionAttributeListeners.add((HttpSessionAttributeListener) t);
		if (t instanceof HttpSessionIdListener)
			sessionIdListeners.add((HttpSessionIdListener) t);
	}
}
