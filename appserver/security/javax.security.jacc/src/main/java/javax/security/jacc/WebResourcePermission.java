/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.security.jacc;

import java.io.IOException;
import java.io.ObjectStreamField;

import java.security.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Class for Servlet web resource permissions.
 * A WebResourcePermission is a named permission and has actions.
 * <P>
 * The name of a WebResourcePermission (also referred to as the target name)
 * identifies the Web resources to which the permission pertains.
 * <P>
 * Implementations of this class MAY implement newPermissionCollection or
 * inherit its implementation from the super class.
 * 
 * @see java.security.Permission
 *
 * @version %I% %E%
 *
 * @author Ron Monzillo
 * @author Gary Ellison
 *
 */

public final class WebResourcePermission extends Permission
implements java.io.Serializable
{

     private transient HttpMethodSpec methodSpec;

     private transient URLPatternSpec urlPatternSpec = null;

     private transient int hashCodeValue = 0;

     private transient static final String EMPTY_STRING = "";
     private transient static final String ESCAPED_COLON = "%3A";

     private static final long serialVersionUID = 1L;

    /**
     * The serialized fields of this permission are defined below. Whether
     * or not the serialized fields correspond to actual (private) fields 
     * is an implementation decision.
     * @serialField actions String
     * the canonicalized actions string (as returned by getActions).
     */
     private static final ObjectStreamField[] serialPersistentFields = { 
	 new ObjectStreamField("actions", java.lang.String.class)
     };

    /**
     * Creates a new WebResourcePermission with the specified name and actions.
     * <P>
     * The name contains a URLPatternSpec that identifies the web
     * resources to which the permissions applies. The syntax of a URLPatternSpec
     * is as follows:
     * <P><Pre>
     *
     *          URLPatternList ::= URLPattern | URLPatternList colon URLPattern
     *
     *          URLPatternSpec ::= null | URLPattern | URLPattern colon URLPatternList
     *
     * </Pre><P>
     * A null URLPatternSpec is translated to the default URLPattern, "/", by
     * the permission constructor. The empty string is an exact URLPattern, and
     * may occur anywhere in a URLPatternSpec that an exact URLPattern may occur.
     * The first URLPattern in a URLPatternSpec may be any of the pattern 
     * types, exact, path-prefix, extension, or default as defined in the
     * <i>Java Servlet Specification)</i>. When a URLPatternSpec includes
     * a URLPatternList, the patterns of the URLPatternList identify the
     * resources to which the permission does NOT apply and depend on the
     * pattern type and value of the first pattern as follows:  <p>
     * <ul>
     * <li> No pattern may exist in the URLPatternList that matches the
     *      first pattern.
     * <li> If the first pattern is a path-prefix
     *      pattern, only exact patterns matched by the first pattern
     *      and path-prefix patterns matched by, but different from,
     *      the first pattern may occur in the URLPatternList.
     * <li> If the first pattern is an extension
     *      pattern, only exact patterns that are matched by the first 
     *      pattern and path-prefix patterns may occur in the URLPatternList.
     * <li> If the first pattern is the default pattern, "/", any pattern
     *      except the default pattern may occur in the URLPatternList.
     * <li> If the first pattern is an exact pattern a URLPatternList must not
     *      be present in the URLPatternSpec.
     * </ul>
     * <P>
     * The actions parameter contains a comma seperated list of HTTP methods.
     * The syntax of the actions parameter is defined as follows:
     * <P><Pre>
     *
     *          ExtensionMethod ::= any token as defined by RFC 2616
     *                    (that is, 1*[any CHAR except CTLs or separators])
     *
     *          HTTPMethod ::= "GET" | "POST" | "PUT" | "DELETE" | "HEAD" |
     *                   "OPTIONS" | "TRACE" | ExtensionMethod
     *          
     *          HTTPMethodList ::= HTTPMethod | HTTPMethodList comma HTTPMethod
     *  
     *          HTTPMethodExceptionList ::= exclaimationPoint HTTPMethodList
     * 
     *          HTTPMethodSpec ::= null | HTTPMethodExceptionList | 
     *                   HTTPMethodList
     *
     * </Pre><P>
     * If duplicates occur in the HTTPMethodSpec 
     * they must be eliminated by the permission constructor.
     * <P>
     * A null or empty string HTTPMethodSpec indicates that the permission
     * applies to all HTTP methods at the resources identified by the URL
     * pattern.
     * <P>
     * If the HTTPMethodSpec contains an HTTPMethodExceptionList (i.e., 
     * it begins with an exclaimationPoint), the permission pertains to all 
     * methods except those occuring in the exception list.
     * <P>
     * @param name the URLPatternSpec that identifies the application 
     * specific web resources to which the permission pertains. 
     * All URLPatterns in the URLPatternSpec are relative to the context path
     * of the deployed web application module, and the same URLPattern must not
     * occur more than once in a URLPatternSpec. A null URLPatternSpec is 
     * translated to the default URLPattern, "/", by the permission constructor.
     * All colons occuring within the URLPattern elements of the URLPatternSpec
     * must be represented in escaped encoding as defined in RFC 2396.
     * <P>
     * @param actions identifies the HTTP methods to which the permission
     * pertains. If the value passed through this parameter is null or
     * the empty string, then the permission pertains to all the possible 
     * HTTP methods.
     */

     public WebResourcePermission(String name, String actions)
     {	
	super(name);
	this.urlPatternSpec = new URLPatternSpec(name);
	this.methodSpec = HttpMethodSpec.getSpec(actions);
     }

    /**
     * Creates a new WebResourcePermission with name corresponding
     * to the URLPatternSpec, and actions composed from the array of HTTP
     * methods.
     * <P>
     * @param urlPatternSpec the URLPatternSpec that identifies the 
     * application specific web resources to which the permission pertains.
     * All URLPatterns in the URLPatternSpec are relative to the context path
     * of the deployed web application module, and the same URLPattern must not
     * occur more than once in a URLPatternSpec. A null URLPatternSpec is 
     * translated to the default URLPattern, "/", by the permission constructor.
     * All colons occuring within the URLPattern elements of the URLPatternSpec
     * must be represented in escaped encoding as defined in RFC 2396.
     * <P>
     * @param HTTPMethods an array of strings each element of which contains
     * the value of an HTTP method. If the value passed through this
     * parameter is null or is an array with no elements, then the permission
     * pertains to all the possible HTTP methods.
     */

     public WebResourcePermission(String urlPatternSpec, String[] HTTPMethods)
     {
	super(urlPatternSpec);
	this.urlPatternSpec = new URLPatternSpec(urlPatternSpec);
	this.methodSpec = HttpMethodSpec.getSpec(HTTPMethods);
     }

    /**
     * Creates a new WebResourcePermission from the HttpServletRequest
     * object.
     * <P>
     * @param request the HttpServletRequest object corresponding
     * to the Servlet operation to which the permission pertains.
     * The permission name is the substring of the requestURI 
     * (HttpServletRequest.getRequestURI()) that begins after the contextPath
     * (HttpServletRequest.getContextPath()). When the substring operation
     * yields the string "/", the permission is constructed with the empty
     * string as its name. The permission's actions field is obtained from 
     * HttpServletRequest.getMethod(). The constructor must transform all colon
     * characters occuring in the name to escaped encoding as defined in
     * RFC 2396.
     */

     public WebResourcePermission(HttpServletRequest request)
     {
	super(getUriMinusContextPath(request));
	this.urlPatternSpec = new URLPatternSpec(super.getName());
	this.methodSpec= HttpMethodSpec.getSpec(request.getMethod());
     }

    /**
     * Checks two WebResourcePermission objects for equality.
     * WebResourcePermission objects are equivalent if their 
     * URLPatternSpec and (canonicalized) actions values are equivalent.
     * The URLPatternSpec of a reference permission is equivalent to that
     * of an argument permission if their first patterns are
     * equivalent, and the patterns of the URLPatternList of the reference
     * permission collectively match exactly the same set of patterns
     * as are matched by the patterns of the URLPatternList of the 
     * argument permission.
     * <P>
     * Two Permission objects, P1 and P2, are equivalent if and only if
     * P1.implies(P2) && P2.implies(P1).
     * <P>
     * @param o the WebResourcePermission object being tested for equality
     * with this WebResourcePermission.
     * <P>
     * @return true if the argument WebResourcePermission object is equivalent
     * to this WebResourcePermission.
     */

    public boolean equals(Object o) {
	if (o == null || ! (o instanceof WebResourcePermission)) return false;

	WebResourcePermission that = (WebResourcePermission) o;

	if (!this.methodSpec.equals(that.methodSpec)) return false;
	
	return this.urlPatternSpec.equals(that.urlPatternSpec);
    }

   /**
    * Returns a canonical String representation of the actions of this
    * WebResourcePermission. In the canonical form, predefined methods
    * preceed extension methods, and within each method classification the
    * corresponding methods occur in ascending lexical order. There may be 
    * no duplicate HTTP methods in the canonical form, and the canonical 
    * form of the set of all HTTP methods is the value null.
    * <P>
    * @return a String containing the canonicalized actions of this
    * WebResourcePermission (or the null value).
    */

    public String getActions() {
	return this.methodSpec.getActions();
    }

   /**
    * Returns the hash code value for this WebResourcePermission. The
    * properties of the returned hash code must be as follows: <p>
    * <ul>
    * <li> During the lifetime of a Java application, the hashCode method
    *      must return the same integer value, every time it is called on a
    *      WebResourcePermission object. The value returned by hashCode for a
    *      particular WebResourcePermission need not remain consistent from
    *      one execution of an application to another.
    * <li> If two WebResourcePermission objects are equal according to the
    *      equals method, then calling the hashCode method on each of the two
    *      Permission objects must produce the same integer result (within an
    *      application).
    * </ul>
    * <P>
    * @return the integer hash code value for this object.
    */

    public int hashCode() {
	if (this.hashCodeValue == 0) {
	    String hashInput = this.urlPatternSpec.toString()+ " " +
                    this.methodSpec.hashCode();

	    this.hashCodeValue = hashInput.hashCode();
	}
	return this.hashCodeValue;
    }

    /**
     * Determines if the argument Permission is "implied by" this
     * WebResourcePermission. For this to be the case, all of the following
     * must be true:
     * <p><ul>
     * <li> The argument is an instanceof WebResourcePermission
     * <li> The first URLPattern in the name of the argument permission
     *      is matched by the first URLPattern in the name of this permission.
     * <li> The first URLPattern in the name of the argument permission
     *      is NOT matched by any URLPattern in the URLPatternList of the
     *      URLPatternSpec of this permission.
     * <li> If the first URLPattern in the name of the argument permission
     *      matches the first URLPattern in the URLPatternSpec of this 
     *      permission, then every URLPattern in the URLPatternList of the
     *      URLPatternSpec of this permission is matched by a URLPattern
     *      in the URLPatternList of the argument permission.
     * <li> The HTTP methods represented by the actions of the argument 
     *      permission are a subset of the HTTP methods represented by the
     *      actions of this permission.
     * </ul>
     * <P>
     * URLPattern matching is performed using the <i>Servlet matching 
     * rules</i> where two URL patterns match if they are related as follows:
     * <p><ul>
     * <li> their pattern values are String equivalent, or
     * <li> this pattern is the path-prefix pattern "/*", or
     * <li> this pattern is a path-prefix pattern (that is, it starts with 
     *      "/" and ends with "/*") and the argument pattern starts with the 
     *      substring of this pattern, minus its last 2 characters, and the
     *      next character of the argument pattern, if there is one, is "/", or
     * <li> this pattern is an extension pattern (that is, it starts with 
     *      "*.") and the argument pattern ends with this pattern, or
     * <li> the reference pattern is the special default pattern, "/",
     *      which matches all argument patterns.
     * </ul>
     * <P>
     * All of the comparisons described above are case sensitive.
     * <P>
     * @param permission "this" WebResourcePermission is checked to see if
     * it implies the argument permission.
     * <P>
     * @return true if the specified permission is implied by this object,
     * false if not.
     */
    public boolean implies(Permission permission) {
	if (permission == null ||
	    ! (permission instanceof WebResourcePermission)) return false;

	WebResourcePermission that = (WebResourcePermission) permission;
	    
	if (!this.methodSpec.implies(that.methodSpec)) 
	    return false;

	return this.urlPatternSpec.implies(that.urlPatternSpec);
    }

    // ----------------- Private Methods ---------------------

    /**
     * Chops the ContextPath off the front of the requestURI to
     * yield the servletPath + PathInfo. For the special case where
     * the servletPath + PathInfo is the pattern, "/", this
     * routine returns the empty string.
     */
     private static String getUriMinusContextPath(HttpServletRequest request) {
	 String uri = request.getRequestURI();
	 if (uri != null) {
	     String contextPath = request.getContextPath();
	     int contextLength = contextPath == null ? 0 : contextPath.length();
	     if (contextLength > 0) {
		 uri = uri.substring(contextLength);
	     }
	     if (uri.equals("/")) {
		 uri = EMPTY_STRING;
	     } else {
 		 // encode all colons
 		 uri = uri.replaceAll(":",ESCAPED_COLON);
  	     }
	 } else {
	     uri = EMPTY_STRING;
	 }
	 return uri;
     }

    /**
     * readObject reads the serialized fields from the
     * input stream and uses them to restore the permission. 
     * This method need not be implemented if establishing the 
     * values of the serialized fields (as is done by defaultReadObject) 
     * is sufficient to initialize the permission.
     */
    private synchronized void readObject(java.io.ObjectInputStream s)
         throws IOException,ClassNotFoundException
    {
	this.methodSpec = HttpMethodSpec.getSpec
	    ((String) s.readFields().get("actions",null));
	this.urlPatternSpec = new URLPatternSpec(super.getName());
    }

    /**
     * writeObject is used to establish the values of the serialized fields
     * before they are written to the output stream and need not be
     * implemented if the values of the serialized fields are always 
     * available and up to date. The serialized fields are written to
     * the output stream in the same form as they would be written
     * by defaultWriteObject.
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s)
         throws IOException
    {
	s.putFields().put("actions",this.getActions());
	s.writeFields();
    }

}



