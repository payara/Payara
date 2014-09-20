/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.gadget;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.LayoutDefinitionManager;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerDefinition;
import com.sun.jsftemplating.layout.descriptors.handler.OutputTypeManager;
import com.sun.jsftemplating.util.FileUtil;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.JSONUtil;
import org.glassfish.admingui.connector.GadgetModule;

import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.DomDocument;

/**
 *  <p>	This class provides access to {@link GadgetModule}s.  It also has a
 *	nice JSON utility for converting JavaBean Objects (and primitives,
 *	Collections, Maps, Strings, etc.) to JSON.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
public class GadgetHandlers {

    /**
     *	<p> Default constructor.</p>
     */
    public GadgetHandlers() {
    }

    /**
     *	<p> This handler returns a {@link GadgetModule} for the named
     *	    gadget.  The <code>name</code> should either be a complete URL,
     *	    or a context-root relative path to the gadget XML file (this
     *	    also includes .xml files stored in .jar's / plugins).</p>
     */
    @Handler(id="gf.getGadgetModule",
	input = {
	    @HandlerInput(name="name", type=String.class, required=true)
	},
	output = {
	    @HandlerOutput(name="module", type=GadgetModule.class)
	})
    public static void getGadgetModule(HandlerContext handlerCtx) {
	String gadgetName = (String) handlerCtx.getInputValue("name");
	URL url = null;
	try {
	    if (!gadgetName.contains("://")) {
		// Treat as a path...
		url = FileUtil.searchForFile(gadgetName, null);
	    }
	    if (url == null) {
		url = new URL(gadgetName);
	    }
	} catch (Exception ex) {
	    throw new IllegalArgumentException("Cannot creaqte URL from '"
		+ gadgetName + "'!", ex);
	}
	GadgetModule module = getGadgetModule(url);
	handlerCtx.setOutputValue("module", module);
    }

    /**
     *	<p> This method returns a {@link GadgetModule} for the given URL.</p>
     */
    public static GadgetModule getGadgetModule(URL url) {
	if (url == null) {
	    return null;
	}
// FIXME: Cache?
	// Get our parser...
	ConfigParser parser = new ConfigParser(GuiUtil.getHabitat());
	String id = null;

	// Read the document...
	DomDocument doc = parser.parse(url);

	// Get the GadgetModule
	GadgetModule module = (GadgetModule) doc.getRoot().get();

	return module;
    }

    /**
     *	<p> This handler will invoke another handler.  This allows a generic
     *	    handler to invoke another one and return the response(s), if
     *	    any.</p>
     *
     *	<p> The following are the inputs are supported:</p>
     *	    <ul><li><b>handler</b> - (required) This input specifies the
     *		    handler which should be invoked.</li>
     *		<li><b>args</b> - (required) This specifies all of the
     *		    arguments to be passed to the handler (both input and
     *		    output arguments).  The value of this should be a String
     *		    formatted as a comma separated list of name-value pairs
     *		    (which are themselves separated by colons (:).  The value
     *		    of the name-value pairs should be URL encoded (so that
     *		    commas are escaped).</li>
     *		<li><b>depth</b> - (optional) This property specifies the max
     *		    depth of nesting for any output values from the handler.
     *		    Output values are encoded in JSON.  This prevents infinite
     *		    looping in the case where an Object refers to itself (or in
     *		    the case wehre there is unnecessarily deep data
     *		    structures).</li></ul>
     */
    @Handler(id="gf.invokeHandler",
	input = {
	    @HandlerInput(name="handler", type=String.class, required=true),
	    @HandlerInput(name="args", type=String.class, required=true),
	    @HandlerInput(name="depth", type=Integer.class, required=false)
	},
	output = {
	    @HandlerOutput(name="values", type=String.class)
	})
    public static Object invokeHandler(HandlerContext handlerCtx) {
	// First find the HandlerDefinition
	String handlerName = (String) handlerCtx.getInputValue("handler");
	HandlerDefinition handlerDef = LayoutDefinitionManager.getGlobalHandlerDefinition(handlerName);
	if (handlerDef == null) {
	    throw new IllegalArgumentException("Handler '" + handlerName
		    + "' not found!");
	}

	// Before working with the new Handler, save the old Handler...
	com.sun.jsftemplating.layout.descriptors.handler.Handler oldHandler =
		handlerCtx.getHandler();

	// Create the Handler to invoke...
	com.sun.jsftemplating.layout.descriptors.handler.Handler handler =
	    new com.sun.jsftemplating.layout.descriptors.handler.Handler(handlerDef);

	// Now try to get the inputs / outputs
	List<String> outputNames = new ArrayList<String>();
	String args = (String) handlerCtx.getInputValue("args");
	StringTokenizer tok = new StringTokenizer(args, ",");
	String nvp, name, value;
    Object val = null;
	int colon;
	while (tok.hasMoreTokens()) {
	    // Get the NVP...
	    nvp = tok.nextToken();
	    colon = nvp.indexOf(':');
	    if (colon == -1) {
		throw new IllegalArgumentException(
		    "Handler I/O name:value must be separated by a ':'!");
	    }
	    name = nvp.substring(0, colon).trim();
	    value = nvp.substring(colon+1).trim();

	    // URL decode 'value'...
	    try {
		value = URLDecoder.decode(value, "UTF-8");
	    } catch (UnsupportedEncodingException ex) {
		throw new IllegalArgumentException(
		    "Unable to decode value, this is not normal!", ex);
	    }

	    // See if it is an input...
	    if (handlerDef.getInputDef(name) != null) {
		    // It's an input...
            if (value.startsWith("{") && value.endsWith("}")) {
                Object t = parseString(value.substring(1, (value.length()) - 1));
                val = t;
            } else {
                val = value;
            }
            handler.setInputValue(name, val);
        } else {
		// Assume it's an output mapping...
		handler.setOutputMapping(name, val.toString(), OutputTypeManager.EL_TYPE);
		outputNames.add(name);
	    }
	}

	// We have the new handler (yea!), invoke it...
	List<com.sun.jsftemplating.layout.descriptors.handler.Handler> handlers =
	    new ArrayList<com.sun.jsftemplating.layout.descriptors.handler.Handler>(1);
	handlers.add(handler);
	Object result = handlerCtx.getLayoutElement().
		dispatchHandlers(handlerCtx, handlers);

	// Now... lets get the output values from the "child" handler...
	Map<String, Object> outputValues = new HashMap<String, Object>();
	String outName;
	Iterator<String> it = outputNames.iterator();
	while (it.hasNext()) {
	    // For each output specified, save it in a Map to be encoded later
	    outName = it.next();
	    outputValues.put(outName, handler.getOutputValue(handlerCtx, outName));
	}

	// Now we're done with the "child" Handler, restore this Handler...
	handlerCtx.setHandler(oldHandler);

	// Finally, translate the Map to JSON and set the String as an output
	Integer depth = (Integer) handlerCtx.getInputValue("depth");
	if (depth == null) {
	    depth = 10;
	}
	handlerCtx.setOutputValue("values", JSONUtil.javaToJSON(outputValues, depth));

	return result;
    }
    public static Object parseString(String test) {
        Map newMap = new HashMap();
        String[] strs = test.split(",");
        for (String str : strs) {
            str = str.trim();
            int end = str.length();
            int index = str.indexOf("=");
            newMap.put(str.substring(0, index), str.substring(index+1, end));
        }
        return newMap;
    }
}
