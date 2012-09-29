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

import java.util.HashMap;

import java.io.IOException;
import java.io.ObjectStreamField;

import java.security.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Class for Servlet Web user data permissions.
 * A WebUserDataPermission is a named permission and has actions.
 * <P>
 * The name of a WebUserDataPermission (also referred to as the target name)
 * identifies a Web resource by its context path relative URL pattern.
 *
 * @see java.security.Permission
 *
 * @author Ron Monzillo
 * @author Gary Ellison
 *
 */

public final class WebUserDataPermission extends Permission
implements java.io.Serializable
{

     private static String transportKeys[] = {
	 "NONE",
	 "INTEGRAL",
	 "CONFIDENTIAL",
     };

     private static HashMap transportHash = new HashMap();
     static {
	 for (int i=0; i<transportKeys.length; i++) 
	     transportHash.put(transportKeys[i], new Integer(i));
     };

     private static int TT_NONE = 
         ((Integer) transportHash.get("NONE")).intValue();
     private static int TT_CONFIDENTIAL = 
         ((Integer) transportHash.get("CONFIDENTIAL")).intValue();
  
     private transient URLPatternSpec urlPatternSpec = null;

     private transient HttpMethodSpec methodSpec;

     private transient int transportType;

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
     * Creates a new WebUserDataPermission with the specified name and actions.
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
     * The actions parameter contains a comma separated list of HTTP methods
     * that may be followed by a transportType separated from the HTTP
     * method by a colon.
     * <P><Pre>
     *
     *          ExtensionMethod ::= any token as defined by RFC 2616
     *                  (that is, 1*[any CHAR except CTLs or separators])
     *
     *          HTTPMethod ::= "Get" | "POST" | "PUT" | "DELETE" | "HEAD" |
     *                  "OPTIONS" | "TRACE" | ExtensionMethod
     *
     *          HTTPMethodList ::= HTTPMethod | HTTPMethodList comma HTTPMethod
     *
     *          HTTPMethodExceptionList ::= exclaimationPoint HTTPMethodList
     *
     *          HTTPMethodSpec ::= emptyString | HTTPMethodExceptionList |
     *                  HTTPMethodList
     *
     *          transportType ::= "INTEGRAL" | "CONFIDENTIAL" | "NONE"
     *
     *          actions ::= null | HTTPMethodSpec | 
     *                  HTTPMethodSpec colon transportType
     *
     * </Pre><P>
     * If duplicates occur in the HTTPMethodSpec
     * they must be eliminated by the permission constructor.
     * <P>
     * An empty string HTTPMethodSpec is a shorthand for a List
     * containing all the possible HTTP methods.
     * <P>
     * If the HTTPMethodSpec contains an HTTPMethodExceptionList (i.e., 
     * it begins with an exclaimationPoint), the permission pertains 
     * to all methods except those occuring in the exception list.
     * <P>
     * An actions string without a transportType is a shorthand for a
     * actions string with the value "NONE" as its TransportType.
     * <P>
     * A granted permission representing a transportType of "NONE", 
     * indicates that the associated resources may be accessed
     * using any conection type.
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
     * @param actions identifies the HTTP methods and transport type to which
     * the permission pertains. If the value passed through this
     * parameter is null or the empty string, then the permission
     * is constructed with actions corresponding to all the possible
     * HTTP methods and transportType "NONE".
     */

     public WebUserDataPermission(String name, String actions)
     {  
        super(name);
	this.urlPatternSpec = new URLPatternSpec(name);
	parseActions(actions);
     }

    /**
     * Creates a new WebUserDataPermission with name corresponding to the
     * URLPatternSpec, and actions composed from the array of HTTP methods 
     * and the transport type.
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
     * is constructed with actions corresponding to all the possible HTTP methods.
     * <P>
     * @param transportType a String whose value is a transportType.
     * If the value passed through this parameter is null, then the permission
     * is constructed with actions corresponding to transportType "NONE".
     */

     public WebUserDataPermission(String urlPatternSpec, String[] HTTPMethods, 
				  String transportType)
     {
	super(urlPatternSpec);
	this.urlPatternSpec = new URLPatternSpec(urlPatternSpec);

	this.transportType = TT_NONE;

	if (transportType != null) {
	    Integer bit = (Integer) transportHash.get(transportType);
	    if (bit == null) 
		throw new IllegalArgumentException("illegal transport value");
	    this.transportType = bit.intValue();
	}

	this.methodSpec = HttpMethodSpec.getSpec(HTTPMethods);
     }

    /**
     * Creates a new WebUserDataPermission from the HttpServletRequest
     * object.
     * <P>
     * @param request the HttpServletRequest object corresponding
     * to the Servlet operation to which the permission pertains.
     * The permission name is the substring of the requestURI 
     * (HttpServletRequest.getRequestURI()) that begins after the contextPath
     * (HttpServletRequest.getContextPath()). When the substring operation
     * yields the string "/", the permission is constructed with the empty
     * string as its name. The constructor must transform all colon
     * characters occuring in the name to escaped encoding as defined in
     * RFC 2396. The HTTP method component of the permission's
     * actions is as obtained from HttpServletRequest.getMethod().
     * The TransportType component of the permission's
     * actions is determined by calling HttpServletRequest.isSecure().
     */

     public WebUserDataPermission(HttpServletRequest request)
     {
	super(getUriMinusContextPath(request));
	this.urlPatternSpec = new URLPatternSpec(super.getName());
	this.transportType = request.isSecure() ? TT_CONFIDENTIAL : TT_NONE;
	this.methodSpec = HttpMethodSpec.getSpec(request.getMethod());
     }

    /**
     * Checks two WebUserDataPermission objects for equality.
     * WebUserDataPermission objects are equivalent if their 
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
     * @param o the WebUserDataPermission object being tested for equality
     * with this WebUserDataPermission.
     * <P>
     * @return true if the argument WebUserDataPermission object is equivalent
     * to this WebUserDataPermission.
     */

    public boolean equals(Object o) {
	if (o == null || ! (o instanceof WebUserDataPermission)) return false;

	WebUserDataPermission that = (WebUserDataPermission) o;

	if (this.transportType != that.transportType) return false;

	if (!this.methodSpec.equals(that.methodSpec)) return false;

	return this.urlPatternSpec.equals(that.urlPatternSpec);
    }

   /**
    * Returns a canonical String representation of the actions of this
    * WebUserDataPermission. The canonical form of the actions of a 
    * WebUserDataPermission is described by the following syntax description.
    * <P><Pre>
    *
    *          ExtensionMethod ::= any token as defined by RFC 2616
    *                   (that is, 1*[any CHAR except CTLs or separators])
    *
    *          HTTPMethod ::= "GET" | "POST" | "PUT" | "DELETE" | "HEAD" |
    *                   "OPTIONS" | "TRACE" | ExtensionMethod
    *          
    *          HTTPMethodList ::= HTTPMethod | HTTPMethodList comma HTTPMethod
    *
    *          HTTPMethodExceptionList ::= exclaimationPoint HTTPMethodList
    *
    *          HTTPMethodSpec ::= emptyString | HTTPMethodExceptionList |
    *                  HTTPMethodList
    *
    *          transportType ::= "INTEGRAL" | "CONFIDENTIAL" | "NONE"
    *
    *          actions ::= null | HTTPMethodList | 
    *                  HTTPMethodSpec colon transportType
    *
    * </Pre><P>
    * If the permission's HTTP methods correspond to the entire HTTP method
    * set and the permission's transport type is "INTEGRAL" or "CONFIDENTIAL", 
    * the HTTP methods shall be represented in the canonical form by an 
    * emptyString HTTPMethodSpec. If the permission's HTTP methods correspond
    * to the entire HTTP method set, and the permission's transport type is not 
    * "INTEGRAL"or "CONFIDENTIAL", the canonical actions value shall be the
    * null value. 
    *<P>
    * If the permission's methods do not correspond to the entire HTTP
    * method set, duplicates must be eliminated and the remaining elements 
    * must be ordered such that the predefined methods preceed the extension 
    * methods, and such that within each method classification the corresponding 
    * methods occur in ascending lexical order. 
    * The resulting (non-emptyString) HTTPMethodSpec
    * must be included in the canonical form, and if the permission's 
    * transport type is not "INTEGRAL" or "CONFIDENTIAL", the canonical 
    * actions value must be exactly the resulting HTTPMethodSpec.
    * <P>
    * @return a String containing the canonicalized actions of this
    * WebUserDataPermission (or the null value).
    */

    public String getActions() 
    {
	String result;
	String hActions = this.methodSpec.getActions();
	if (this.transportType == TT_NONE && hActions == null) result = null;
	else if (this.transportType == TT_NONE) result = hActions;
	else if (hActions == null) 
	    result = ":" + transportKeys[this.transportType];
	else result = hActions + ":" + transportKeys[this.transportType];
	return result;
    }

   /**
    * Returns the hash code value for this WebUserDataPermission. The
    * properties of the returned hash code must be as follows: <p>
    * <ul>
    * <li> During the lifetime of a Java application, the hashCode method
    *      shall return the same integer value every time it is called on a
    *      WebUserDataPermission object. The value returned by hashCode for a
    *      particular EJBMethod permission need not remain consistent from
    *      one execution of an application to another.
    * <li> If two WebUserDataPermission objects are equal according to the
    *      equals method, then calling the hashCode method on each of the two
    *      Permission objects must produce the same integer result (within an
    *      application).
    * </ul>
    * <P>
    * @return the integer hash code value for this object.
    */

    public int hashCode() {
	if (this.hashCodeValue == 0) {
	    String hashInput = this.urlPatternSpec.toString() + 
                            " " + this.methodSpec.hashCode() +
                            ":" + this.transportType;

	    this.hashCodeValue = hashInput.hashCode();
	}
	return this.hashCodeValue;
    }

    /**
     * Determines if the argument Permission is "implied by" this
     * WebUserDataPermission. For this to be the case all of the following
     * must be true:<p>
     * <ul>
     * <li> The argument is an instanceof WebUserDataPermission.
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
     * <li> The transportType in the actions of this permission 
     *      either corresponds to the value "NONE", or equals the 
     *      transportType in the actions of the argument permission.
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
     * @param permission "this" WebUserDataPermission is checked to see if
     * it implies the argument permission.
     * <P>
     * @return true if the specified permission is implied by this object,
     * false if not.
     */

    public boolean implies(Permission permission) {
	if (permission == null ||
	    ! (permission instanceof WebUserDataPermission)) return false;

	WebUserDataPermission that = (WebUserDataPermission) permission;

	if (this.transportType != TT_NONE && 
	    this.transportType != that.transportType) return false;

	if (!this.methodSpec.implies(that.methodSpec)) return false;

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
 		//  encode all colons
 		uri = uri.replaceAll(":",ESCAPED_COLON);
  	    }
	} else {
	    uri = EMPTY_STRING;
	}
	return uri;
    }

    private void parseActions(String actions)
    {
	this.transportType = TT_NONE;
 
	if (actions == null || actions.equals("")) {
	     this.methodSpec = HttpMethodSpec.getSpec((String) null);
    	} else {
	    int colon = actions.indexOf(':');
	    if (colon < 0) {
		this.methodSpec = HttpMethodSpec.getSpec(actions);
	    } else { 
		if (colon == 0) {
		    this.methodSpec = HttpMethodSpec.getSpec((String) null);
		} else {
		    this.methodSpec = HttpMethodSpec.getSpec
                       (actions.substring(0,colon));
		}
		Integer bit = (Integer)
		    transportHash.get(actions.substring(colon+1));
		if (bit == null)
		    throw new IllegalArgumentException
			("illegal transport value");
      
		this.transportType = bit.intValue();
	    }
	}
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
	parseActions((String) s.readFields().get("actions",null));
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







