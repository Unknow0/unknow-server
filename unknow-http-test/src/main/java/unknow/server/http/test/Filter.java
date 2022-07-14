/**
 * 
 */
package unknow.server.http.test;

import javax.servlet.annotation.WebFilter;

import unknow.server.http.AccessLogFilter;

/**
 * @author unknow
 */
@WebFilter("/*")
public class Filter extends AccessLogFilter {

}
