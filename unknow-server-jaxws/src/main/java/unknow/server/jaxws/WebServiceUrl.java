/**
 * 
 */
package unknow.server.jaxws;

/**
 * UrlPattern to use of a service default to the class name
 * 
 * @author unknow
 */
public @interface WebServiceUrl {
	/** @return url pattern */
	String[] value();
}
