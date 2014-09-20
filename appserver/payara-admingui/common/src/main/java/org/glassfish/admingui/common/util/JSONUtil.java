/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  <p>	This class provides basic JSON encoding / decoding.  It has 2 primary
 *	methods that are of interest.  The first allows you to encode a Java
 *	Object into JSON.  The other allows you to create a Java data
 *	structure from a JSON String.  See:</p>
 *
 *  <ul><li>{@link #jsonToJava(String json)}</li>
 *	<li>{@link #javaToJSON(Object obj, int depth)}</li></ul>
 */
public class JSONUtil {
    private static final String	ABORT_PROCESSING    =	"____EnD___";
    private static final String	COLON		    =	"____CoLoN___";
    private static final String	COMMA		    =	"____CoMmA___";
    private static final String	NULL		    =	"____NuLl___";

    /**
     *	<p> This method returns a Java representation of the given JSON
     *	    String.  The Java data structure created will be created using
     *	    Map's, String's, Long's, Float's, Boolean's, and List's as
     *	    specified by the JSON String.</p>
     */
    public static Object jsonToJava(String json) {
	return replaceSpecial(jsonToJava(new JsonChars(json)));
    }

    /**
     *	<p> This method attempts to convert the given Object into a JSON String
     *	    to given depth.  If -1 (or lower) is supplied for depth, it will
     *	    walk upto a default depth of 10 levels of the given Object.  If 0
     *	    is supplied, it will simply return "".  1 will encode the current
     *	    Object, but no children.  2 will encode the given Object and its
     *	    direct children (if any), and so on.</p>
     *
     *	<p> Strings, Longs, Float, and primitives are considered to not have
     *	    child Objects.  Objects which have a public no-argument getXYZ()
     *	    method are considered to be child Objects.  Maps and Collections
     *	    will be walked.</p>
     */
    public static String javaToJSON(Object obj, int depth) {
	if (depth == 0) {
	    // Make sure we do nothing if told to do nothing...
	    return "";
	} else if (depth == -1) {
	    // To prevent recursion...
	    depth = 10;
	}
	String value = "";
	if (obj == null) {
	    value = "null";
	} else if (obj instanceof String) {
	    String chStr;
	    int len;
	    StringCharacterIterator it =
		new StringCharacterIterator((String) obj);
	    char ch = it.first();
	    StringBuilder builder =
		    new StringBuilder(((String) obj).length() << 2);
	    builder.append("\"");
	    while (ch != StringCharacterIterator.DONE) {
		switch (ch) {
		    case '\t':
			builder.append("\\t");
			break;
		    case '\n':
			builder.append("\\n");
			break;
		    case '\r':
			builder.append("\\r");
			break;
		    case '\b':
			builder.append("\\b");
			break;
		    case '\f':
			builder.append("\\f");
			break;
		    case '&':
		    case '<':
		    case '>':
		    case '(':
		    case ')':
		    case '{':
		    case '}':
		    case ':':
		    case '/':
		    case '\\':
		    case '\'':
		    case '"':
			builder.append("\\");
			builder.append(ch);
			break;
		    default:
			// Check if we should unicode escape this...
			if ((ch > 0x7e) || (ch < 0x20)) {
			    builder.append("\\u");
			    chStr = Integer.toHexString(ch);
			    len = chStr.length();
			    for (int idx=4; idx > len; idx--) {
				// Add leading 0's
				builder.append('0');
			    }
			    builder.append(chStr);
			} else {
			    builder.append(ch);
			}
			break;
		}
		ch = it.next();
	    }
	    builder.append("\"");
	    value = builder.toString();
	} else if ((obj instanceof Boolean) || (obj instanceof Number)) {
	    value = obj.toString();
	} else if (obj instanceof Object[]) {
	    StringBuilder builder = new StringBuilder("[");
	    boolean first = true;
	    for (Object element : ((Object []) obj)) {
		if (first) {
		    first = false;
		} else {
		    builder.append(',');
		}
		if (depth == 1) {
		    // Treat as String, but don't try to go deeper...
		    builder.append(javaToJSON(element.toString(), 1));
		} else {
		    // Recurse...
		    builder.append(javaToJSON(element, depth-1));
		}
	    }
	    builder.append("]");
	    value = builder.toString();
	} else if (obj instanceof Map) {
	    StringBuilder builder = new StringBuilder("{");
	    String key;
	    boolean first = true;
	    Map map = ((Map) obj);
	    Iterator it = map.keySet().iterator();
	    while (it.hasNext()) {
		if (first) {
		    first = false;
		} else {
		    builder.append(',');
		}
		key = it.next().toString();
		builder.append(javaToJSON(key, 1) + ":");
		if (depth == 1) {
		    // Treat as String, but don't try to go deeper...
		    builder.append(javaToJSON(map.get(key).toString(), 1));
		} else {
		    // Recurse...
		    builder.append(javaToJSON(map.get(key), depth-1));
		}
	    }
	    builder.append("}");
	    value = builder.toString();
	} else if (obj instanceof Collection) {
	    StringBuilder builder = new StringBuilder("[");
	    boolean first = true;
	    Iterator it = ((Collection) obj).iterator();
	    while (it.hasNext()) {
		if (first) {
		    first = false;
		} else {
		    builder.append(',');
		}
		if (depth == 1) {
		    // Treat as String, but don't try to go deeper...
		    builder.append(javaToJSON(it.next().toString(), 1));
		} else {
		    // Recurse...
		    builder.append(javaToJSON(it.next(), depth-1));
		}
	    }
	    builder.append("]");
	    value = builder.toString();
	} else {
	    // Object
	    StringBuilder builder = new StringBuilder("{");
	    String methodName;
	    Object result;
	    boolean first = true;
	    Iterator<String> it = getGetters(obj).iterator();
	    while (it.hasNext()) {
		if (first) {
		    first = false;
		} else {
		    builder.append(',');
		}
		methodName = it.next();
		// Drop "get"...
		builder.append(javaToJSON(methodName.substring(3), 1) + ":");
		result = invokeGetter(obj, methodName);
		if ((result != null) && (depth == 1)) {
		    // Treat as String, but don't try to go deeper...
		    builder.append(javaToJSON(result.toString(), 1));
		} else {
		    // Recurse...
		    builder.append(javaToJSON(result, depth-1));
		}
	    }
	    builder.append("}");
	    value = builder.toString();
	}
	return value;
    }

    /**
     *	<p> This method invokes a getter on the given object.</p>
     *
     *	<p> NOTE: I found a VERY similar method defined in IntegrationPoint...
     *	    at least I'm consistent. ;)  These should probably be combined.</p>
     */
    private static Object invokeGetter(Object obj, String methodName) {
	try {
	    return obj.getClass().getMethod(methodName).invoke(obj);
	} catch (Exception ex) {
	    // Unable to execute it, return null...
	    return null;
	}
    }

    /**
     *	<p> This method returns the names of the public no-arg getters on the
     *	    given Object.</p>
     */
    private static List<String> getGetters(Object obj) {
	List<String> result = new ArrayList<String>();
	for (Method method : obj.getClass().getMethods()) {
	    if (method.getName().startsWith("get")
		    && ((method.getModifiers() & Modifier.PUBLIC) != 0)
		    && (method.getParameterTypes().length == 0)
		    && (!method.getName().equals("getClass"))
		    && (!method.getReturnType().getName().equals("void"))) {
		result.add(method.getName());
	    }
	}
	return result;
    }
    /**
     *	<p> This is the primary switching method which determines the context
     *	    in which the processing should occur.</p>
     */
    private static Object jsonToJava(JsonChars json) {
	Object value = null;
	while (json.hasNext() && (value == null)) {
            char ch = json.next();
	    switch (ch) {
		case '{' :
		    value = readObject(json);
		    break;
		case '[' :
		    value = readArray(json);
		    break;
		case '}' :
		case ']' :
		    if (json.isAtContextEnd()) {
			// Stop processing
			value = ABORT_PROCESSING;
		    } else {
			throw new IllegalArgumentException("Expected '"
			    + json.peekContextEnd() + "' but found '"
			    + json.current() + "' instead!");
		    }
		    break;
		case '-' :
		case '0' :
		case '1' :
		case '2' :
		case '3' :
		case '4' :
		case '5' :
		case '6' :
		case '7' :
		case '8' :
		case '9' :
		    value = readNumber(json);
		    break;
		case '\'' :
		case '"' :
		    value = readString(json);
		    break;
		case 'T' :
		case 't' :
		    value = readConstant(json, "true");
		    break;
		case 'F' :
		case 'f' :
		    value = readConstant(json, "false");
		    break;
		case 'N' :
		case 'n' :
		    value = readConstant(json, "null");
		    break;
		case ' ' :
		case '\t' :
		case '\r' :
		case '\n' :
		case '\b' :
		case '\f' :
		    // Ignore whitespace
		    break;
		case ':' :
		    value = COLON;
		    break;
		case ',' :
		    value = COMMA;
		    break;
		default:
		    throw new IllegalArgumentException(
			"Unexpected char '" + json.current() + "' near: " + json.getContext(30) + "!");
	    }
	}
	return value;
    }

    /**
     *	<p> This method creates a HashMap to represent the JSON Object.</p>
     */
    private static Map<String, Object> readObject(JsonChars json) {
	// Save the ending char...
	json.pushContextEnd('}');

	// Create the Map
	Map<String, Object> map = new HashMap<String, Object>(10);

	Object tmp = null;
	Object key = null;
	Object value = null;
	while (!json.isAtContextEnd()) {
	    // Get the key
	    key = replaceSpecial(jsonToJava(json));
	    if (json.isAtContextEnd()) {
		// Abort...
		break;
	    }
	    if (!(key instanceof String)) {
		throw new IllegalArgumentException(
		    "Object keys must be a String!");
	    }

	    // Get the Colon...
	    if (!(jsonToJava(json).equals(COLON))) {
		throw new IllegalArgumentException(
		    "Object keys must be followed by a colon (:)!");
	    }

	    // Get the value
	    value = replaceSpecial(jsonToJava(json));

	    // Get the comma between properties (may also be context end)
	    tmp = jsonToJava(json);
	    if ( (!(tmp.equals(COMMA))) && !json.isAtContextEnd()) {
		throw new IllegalArgumentException(
		    "Expected comma (,) or end curly brace (}), but found ("
		    + tmp + ") instead!  Near: (" + json.getContext(30) + ")");
	    }

	    // Add the value to the Map...
	    map.put((String) key, value);
	}

	// Remove the context end and return
	json.popContextEnd();
	return map;
    }

    /**
     *	<p> This function will process a JSON string and convert it into
     *	    an array.</p>
     */
    private static List<Object> readArray(JsonChars json) {
	// Save the ending char...
	json.pushContextEnd(']');

	// Create the List
	List<Object> list = new ArrayList<Object>(10);

	Object tmp = null;
	Object value = null;
	while (!json.isAtContextEnd()) {
	    // Get the value
	    value = replaceSpecial(jsonToJava(json));
	    if (!json.isAtContextEnd()) {

		// Get the comma between properties (may also be context end)
		tmp = jsonToJava(json);
		if (( !(tmp.equals(COMMA))) && !json.isAtContextEnd()) {
		    throw new IllegalArgumentException(
			"Expected comma (,) or end curly brace (}), but found ("
			+ tmp + ") instead!");
		}
	    }

	    // Add the value to the List...
	    if ((value == null) || ( !(value.equals(ABORT_PROCESSING)))) {
		list.add(value);
	    }
	}

	// Remove the context end and return
	json.popContextEnd();
	return list;
    }

    /**
     *	<p> This function reads a String and returns it.</p>
     */
    private static String readString(JsonChars json) {
	// Save the ending char...
	json.pushContextEnd(json.current());

	// Build the String...
	StringBuilder builder = new StringBuilder();
	char ch = json.next();
	while (!json.isAtContextEnd()) {
	    if (ch == '\\') {
		ch = json.next();
		switch (ch) {
		    case 'b' :
			ch = '\b';
			break;
		    case 'f' :
			ch = '\f';
			break;
		    case 'n' :
			ch = '\n';
			break;
		    case 'r' :
			ch = '\r';
			break;
		    case 't' :
			ch = '\t';
			break;
		    case 'u' :
			// Handle unicode characters
			builder.appendCodePoint(Integer.parseInt(""
			    + json.next() + json.next()
			    + json.next() + json.next()));
			continue;
		    case '"' :
		    case '\'' :
		    case '\\' :
		    case '/' :
			// Just allow this char to be added
			break;
		    default:
			// Ignore illegal escape character.
			break;
		}
	    }
	    builder.append(ch);
	    ch = json.next();
	}

	// Return the result
	json.popContextEnd();
	return builder.toString();
    }

    /**
     *	<p> Returns either a Float or an Long depending on the data.</p>
     */
    private static Object readNumber(JsonChars json) {
	StringBuilder builder = new StringBuilder();
	char ch = json.current();
	if (ch == '-') {
	    builder.append('-');
	    ch = json.next();
	}
	boolean hasDecimal = false;
	boolean hasExp = false;
	boolean done = false;
	while (!done) {
	    switch (ch) {
		case '0' :
		case '1' :
		case '2' :
		case '3' :
		case '4' :
		case '5' :
		case '6' :
		case '7' :
		case '8' :
		case '9' :
		    builder.append(ch);
		    break;
		case '.' :
		    if (hasDecimal) {
			throw new IllegalArgumentException(
			    "Error while parsing number!  Found multiple decimal points.");
		    }
		    hasDecimal = true;
		    builder.append(ch);
		    break;
		case 'e' :
		case 'E' :
		    // We have an exponent
		    if (hasExp) {
			throw new IllegalArgumentException(
			    "An attempt was made to parse an Long value, however, it was malformed (had to exponents).");
		    }
		    hasExp = true;
		    builder.append(ch);
		    ch = json.next();
		    if ((ch == '-') || (ch == '+')) {
			builder.append(ch);
			ch = json.next();
		    }
		    if ((ch < '0') || (ch > '9')) {
			throw new IllegalArgumentException(
			    "Required a digit after an exponent, however received: '" + ch + "'.");
		    }
		    builder.append(ch);
		    break;
		default:
		    done = true;
		    continue;
	    }
            try {
                ch = json.next();
            } catch (IndexOutOfBoundsException ioobe) {
                done = true;
            }
	}
	// Numbers don't have an ending delimiter, so we need to push the last
	// value back onto the queue
	json.unread();

	// Return the number...
	return (hasDecimal || hasExp) ?
		(Object) Float.valueOf(builder.toString()) :
		(Object) Long.valueOf(builder.toString());
    }

    /**
     *	<p> This method attempts to read a true/false/null value and returns a
     *	    Boolean for true/false values or {@link #NULL} for null values.</p>
     */
    private static Object readConstant(JsonChars json, String constant) {
	byte[] good = constant.getBytes();
	int len = good.length;
	char ch;
	boolean match = true;
	for (int idx=1; idx<len; idx++) {
	    ch = json.next();
	    if (ch != good[idx]) {
		throw new IllegalArgumentException(
		    "Expected constant (" + constant + ")!");
	    }
	}

	// We compared successfully...
	return constant.equals("null") ? NULL : Boolean.valueOf(constant);
    }

    static class JsonChars {
        private String string;
	private int len;
	private int loc = 0;
	private Stack<Character> endContext = new Stack<Character>();

        /**
	 *  Constructor.
	 */
	JsonChars(String json) {
            string = json;
            len = string.length();
	}

	/**
	 *  <p>	Returns the current byte.</p>
	 */
	char current() {
	    return string.charAt(loc-1);
	}

	/**
	 *  <p>	Returns the current byte and increments the location by 1.</p>
	 */
	char next() {
            return string.charAt(loc++);
	    //return (loc<len) ? string.charAt(loc++) : null;
	}

	/**
	 *  <p>	Backs up the iteration 1 character.</p>
	 */
	void unread() {
	    loc--;
	}

	/**
	 *  <p>	This function returns a String that represents the content
	 *	around the current position.  The <code>width</code> property
	 *	specifies how far before and after the current position that
	 *	should be returned as part of the <code>String</code>.</p>
	 */
	String getContext(int width) {
	    int before = loc - width;
	    if (loc < 0) {
		loc = 0;
	    }
        if (before < 0) {
            before = 0;
        }
	    int after = loc + width;
	    if (after > len) {
		after = len;
	    }
	    return string.substring(before, after - before);
//                    new String(bytes, before, after - before);
	}

	/**
	 *  <p>	Returns the length of the JSON String.</p>
	 */
	int getLength() {
	    return len;
	}

	/**
	 *  <p>	Returns true if there are more characters to be parsed.</p>
	 */
	boolean hasNext() {
	    return loc<len;
	}

	/**
	 *  <p>	Returns true if the end of the current context is reached.  For
	 *	example if the current context is an Object, the ending for an
	 *	Object is a '}' byte.</p>
	 */
	boolean isAtContextEnd() {
	    return !hasNext() || (string.charAt(loc-1) == endContext.peek());
	}
	void pushContextEnd(char end) {
	    endContext.push(end);
	}
	char popContextEnd() {
	    return endContext.pop();
	}
	char peekContextEnd() {
	    return endContext.peek();
	}
    }

    /**
     *	<p> This method substitutes the special Strings to their intended
     *	    representations (null, ':', and ',').  This method does nothing
     *	    except return the given value if the requested value is not a
     *	    "special" value.</p>
     */
    private static Object replaceSpecial(Object val) {
	if (val instanceof String) {
	    String strVal = (String) val;
	    if (COLON.equals(strVal)) {
		val = ':';
	    } else if (COMMA.equals(strVal)) {
		val = ',';
	    } else if (NULL.equals(strVal)) {
		val = null;
	    }
	}
	return val;
    }
}
