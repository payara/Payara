/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

/*
 * UtilHandlers.java
 *
 * Created on August 31, 2006, 2:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.glassfish.admingui.common.handlers;

import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.JSONUtil;
import org.glassfish.admingui.common.util.RestUtil;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.LayoutDefinitionManager;
import com.sun.jsftemplating.layout.ViewRootUtil;
import com.sun.jsftemplating.layout.descriptors.LayoutElement;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerDefinition;
import com.sun.jsftemplating.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.logging.Level;

import javax.faces.component.UIViewRoot;

/**
 *
 * @author Jennifer Chou
 */
public class UtilHandlers {
    
    /** Creates a new instance of UtilHandlers */
    public UtilHandlers() {
    }
    

    
    
    /**
     *	<p> Adds the specified (signed) amount of time to the given calendar 
     *      field, based on the calendar's rules and returns the resulting Date.
     *      See <code>java.util.GregorianCalendar</code> add(int field, int amount). </p>
     *
     *  <p> Input value: "Field" -- Type: <code>Integer</code> 
     *          - <code>java.util.Calendar</code> field</p>
     *  <p> Input value: "Amount" -- Type: <code>Integer</code>
     *          - the amount of date or time to be added to the field.</p>
     *  <p> Output value: "Date" -- Type: <code>java.util.Date</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="calendarAdd",
    	input={
	    @HandlerInput(name="Field", type=Integer.class, required=true),
            @HandlerInput(name="Amount", type=Integer.class, required=true)},
        output={
            @HandlerOutput(name="Date", type=java.util.Date.class)})
    public static void calendarAdd(HandlerContext handlerCtx) {
        int field = ((Integer) handlerCtx.getInputValue("Field")).intValue();
        int amount = ((Integer) handlerCtx.getInputValue("Amount")).intValue();
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(field, amount);
        handlerCtx.setOutputValue("Date", cal.getTime());        
    }
    
    /**
     *	<p> Creates a new File instance by converting the given pathname string 
     *      into an abstract pathname. If the given string is the empty string, 
     *      then the result is the empty abstract pathname. </p>
     *
     *  <p> Input value: "Pathname" -- Type: <code>String</code> 
     *  <p> Output value: "File" -- Type: <code>java.io.File</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getFile",
    	input={
	    @HandlerInput(name="Pathname", type=String.class, required=true)},
        output={
            @HandlerOutput(name="File", type=File.class)})
    public static void getFile(HandlerContext handlerCtx) {
        String pathname = (String) handlerCtx.getInputValue("Pathname");
        handlerCtx.setOutputValue("File", pathname != null ? new File(pathname) : null);        
    }

    /**
     *	<p> This handler serves a resource via JSFTemplating's FileStreamer.</p>
     */
    @Handler(id="gf.serveResource",
    	input={
	    @HandlerInput(name="path", type=String.class, required=true)},
	output={
	    @HandlerOutput(name="content", type=String.class)})
    public static void serveResource(HandlerContext ctx) throws java.io.IOException {
	/*
	  JSF 2.0 impl used to set the writer before the render response phase
	  (in apply request values).  So we couldn't control the output of an
	  Ajax request. :(  Therefor the following is commented out.  On
	  10/6/2009 rlubke fixed this, we need to adjust the JS to handle a
	  direct response then this can be enabled.
	 
	    FacesContext facesCtx = ctx.getFacesContext();
	    UIViewRoot root = new UIViewRoot();
	    root.setRenderKitId("dummy");
	    facesCtx.setViewRoot(root);

	    LayoutViewHandler.serveResource(facesCtx,
		(String) ctx.getInputValue("path"));
	 */
	String path = (String) ctx.getInputValue("path");
	int idx = path.lastIndexOf("://");
	String port = null;
	if (idx != -1) {
	    // Strip off protocol
	    path = path.substring(idx + 3);

	    // Now looks like: host.domain:port/resource
// FIXME: port 80 may be omitted (or 443 for https)
	    if ((idx = path.indexOf(':')) != -1) {
		path = path.substring(idx + 1);
		if ((idx = path.indexOf('/')) != -1) {
		    port = path.substring(0, idx);
		    path = path.substring(idx);
		}
	    }
	}
	URL url = FileUtil.searchForFile(path, null);
	if ((url == null) && (port != null)) {
	    // Attempt to read from localhost
	    path = "http://localhost:" + port + path;
	    try {
		url = new URL(path);
	    } catch (MalformedURLException ex) {
		url = null;
	    }
	}
	String content = "";
	if (url != null) {
            try{
                content = new String(FileUtil.readFromURL(url));
            } catch (FileNotFoundException fnfe) {
                //
            }
	}

	// Set the output
	ctx.setOutputValue("content", content);
    }
    
    /**
     *	<p> Returns the name of the file or directory denoted by this abstract 
     *      pathname. This is just the last name in the pathname's name sequence. 
     *      If the pathname's name sequence is empty, then the empty string is returned. </p>
     *
     *  <p> Input value: "File" -- Type: <code>java.io.File</code> 
     *  <p> Output value: "Name" -- Type: <code>String</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="fileGetName",
    	input={
	    @HandlerInput(name="File", type=File.class, required=true)},
        output={
            @HandlerOutput(name="Name", type=String.class)})
    public static void fileGetName(HandlerContext handlerCtx) {
        File file = (File) handlerCtx.getInputValue("File");
        handlerCtx.setOutputValue("Name", (file == null) ? "" : file.getName() );
    }

    /**
     *	<p> Returns a duplicate copy of the source Map
     *
     *  <p> Input value: "source" -- Type: <code>java.util.Map</code>
     *  <p> Output value: "dest" -- Type: <code>java.util.Map</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="mapCopy",
            input={
                    @HandlerInput(name="source", type=Map.class, required=true)},
            output={
                    @HandlerOutput(name="dest", type=Map.class)})
    public static void mapCopy(HandlerContext handlerCtx) {
        Map source = (Map) handlerCtx.getInputValue("source");
        handlerCtx.setOutputValue("dest",  new HashMap(source));
    }
    /**
     *	<p> Returns the value to which the input map maps the input key. </p>
     *
     *  <p> Input value: "Map" -- Type: <code>java.util.Map</code> 
     *  <p> Input value: "Key" -- Type: <code>Object</code>
     *  <p> Output value: "Value" -- Type: <code>Object</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="mapGet",
    	input={
	    @HandlerInput(name="Map", type=Map.class, required=true),
            @HandlerInput(name="Key", type=Object.class, required=true)},
        output={
            @HandlerOutput(name="Value", type=Object.class)})
    public static void mapGet(HandlerContext handlerCtx) {
        Map map = (Map) handlerCtx.getInputValue("Map");
        Object key = (Object) handlerCtx.getInputValue("Key");
        handlerCtx.setOutputValue("Value", (Object) map.get(key));        
    }

    @Handler(id="mapJoin",
        input={
            @HandlerInput(name="map", type=Map.class, required=true),
            @HandlerInput(name="sep", type=String.class, defaultValue=","),
            @HandlerInput(name="skipBlankValues", type=Boolean.class, defaultValue="true")
        },
        output={
            @HandlerOutput(name="result", type=String.class)
        }
    )
    public static void mapJoin(HandlerContext handlerCtx) {
        Map map = (Map)handlerCtx.getInputValue("map");
        String sep = (String)handlerCtx.getInputValue("sep");
        Boolean skipBlankValues = (Boolean)handlerCtx.getInputValue("skipBlankValues");
        String sepHolder = "";
        assert(map != null);
        StringBuilder result = new StringBuilder();

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            Object value = entry.getValue();
            if (skipBlankValues && ((value == null) || (value.toString().isEmpty()))) {
                continue;
            }
            result.append(sepHolder).append(entry.getKey()).append("=").append(entry.getValue());
            sepHolder = sep;
        }

        handlerCtx.setOutputValue("result", result.toString());
    }

    /**
     * This handler goes through all the Map entries, if the value is null, it will convert that to "false"
     * This is used usually to take care of un-checked checkbox which is set to 'null',  but needs to be set to false when
     * the map is passed in as attrsMap for request.
     *	<p> Returns the map  </p>
     *
     *  <p> Input value: "map" -- Type: <code>java.util.Map</code>
     * <p> Input value: "key" -- Type: <code>java.util.List</code>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="gf.mapValueNullToFalse",
        input={
            @HandlerInput(name="map", type=Map.class, required=true),
            @HandlerInput(name="keys", type=List.class, required=true) },
        output={
            @HandlerOutput(name="result", type=Map.class)} )
    public static void mapValueNullToFalse(HandlerContext handlerCtx) {
        Map map = (Map)handlerCtx.getInputValue("map");
        List<String>  keys = (List)handlerCtx.getInputValue("keys");
        Map result = new HashMap(map);
        for( String key: keys){
            if (map.get(key) == null){
                result.put(key, "false");
            }
        }
        handlerCtx.setOutputValue("result", result);
    }

    /**
     *	<p> Returns the keys  </p>
     *
     *  <p> Input value: "Map" -- Type: <code>java.util.Map</code>
     *  <p> Output value: "Keys" -- Type: <code>Object</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="gf.getMapKeys",
    	input={
	    @HandlerInput(name="Map", type=Map.class, required=true)},
        output={
            @HandlerOutput(name="Keys", type=List.class)})
    public static void getMapKeys(HandlerContext handlerCtx) {
        Map map = (Map) handlerCtx.getInputValue("Map");
        List keyList = new ArrayList();
        if (map == null) {
            map = new HashMap();
        }
        keyList.addAll(map.keySet());
        Collections.sort(keyList);
        handlerCtx.setOutputValue("Keys", keyList);
    }

    /**
     * <p> Adds the given value to a <code>List</code></p>
     * <p> Input list: "list" -- Type: <code>java.util.List</code>
     * <p> Input value: "value" -- Type: <code>java.lang.Object</code>
     * <p> Input value: "index" -- Type: <code>Integer</code>
     * 
     * @param handlerCtx The HandlerContext
     */
    @Handler(id="listAdd",
    	input={
            @HandlerInput(name="list", type=List.class),
            @HandlerInput(name="value", type=Object.class, required=true),
            @HandlerInput(name="index", type=Integer.class),
            @HandlerInput(name="sort", type=boolean.class, defaultValue="false"),

        },
        output={
            @HandlerOutput(name="result", type=List.class)}
    )
    public static void listAdd(HandlerContext handlerCtx) {
        List list = (List)handlerCtx.getInputValue("list");
        Integer index = (Integer)handlerCtx.getInputValue("index");
        if(list == null) {
            list = new ArrayList();
        }
        if (index == null){
            list.add(handlerCtx.getInputValue("value"));
        } else {
            list.add(index, handlerCtx.getInputValue("value"));
        }
        boolean sort = (Boolean) handlerCtx.getInputValue("sort");
        if (sort){
            Collections.sort(list);
        }
        handlerCtx.setOutputValue("result", list);
    }

     /**
     * <p> Remove the given value from a <code>List</code></p> if it is present
     * <p> Input list: "list" -- Type: <code>java.util.List</code>
     * <p> Input value: "value" -- Type: <code>java.lang.Object</code>
     *
     * @param handlerCtx The HandlerContext
     */
    @Handler(id="listRemove",
    	input={
            @HandlerInput(name="list", type=List.class),
            @HandlerInput(name="value", type=Object.class, required=true)
        },
        output={
            @HandlerOutput(name="result", type=List.class)}
    )
    public static void listRemove(HandlerContext handlerCtx) {
        List list = (List)handlerCtx.getInputValue("list");
        if(list == null) {
            list = new ArrayList();
        }
        list.remove(handlerCtx.getInputValue("value"));
        handlerCtx.setOutputValue("result", list);
    }


    /**
     * <p> sort a <code>List</code></p>
     * <p> Input list: "list" -- Type: <code>java.util.List</code>
     *
     * @param handlerCtx The HandlerContext
     */
    @Handler(id="gf.listSort",
    	input={
            @HandlerInput(name="list", type=List.class, required=true)}
    )
    public static void listSort(HandlerContext handlerCtx) {
        List list = (List)handlerCtx.getInputValue("list");
        Collections.sort(list);
    }

    /**
     * <p> Combine 2 lists <code>List</code> by adding the object in the 2nd list to the first list</p>
     * <p> Input value: "list" -- Type: <code>java.util.List</code>
     * <p> Input value: "list2" -- Type: <code>java.util.List</code>
     *
     * @param handlerCtx The HandlerContext
     */

    @Handler(id="gf.listCombine",
    	input={
            @HandlerInput(name="list", type=List.class, required=true),
            @HandlerInput(name="list2", type=List.class, required=true)
        },
        output={
            @HandlerOutput(name="result", type=List.class)}
    )
    public static void listCombine(HandlerContext handlerCtx) {
        List list = (List)handlerCtx.getInputValue("list");
        List list2 = (List)handlerCtx.getInputValue("list2");
        if (list == null) {
            list = new ArrayList();            
        }
        if (list2 != null) {
            for(Object one : list2) {
                    list.add(one);
            }
        }
        handlerCtx.setOutputValue("result", list);
    }

    /**
     * <p> Test if a list <code>List</code>contains the string </p>
     * <p> Input value: "list" -- Type: <code>java.util.List</code>
     * <p> Input value: "testStr" -- Type: <code>String</code>
     * <p> Output value: "contain" -- Type: <code>Boolean</code>
     */
    @Handler(id="gf.containedIn",
    	input={
            @HandlerInput(name="list", type=List.class, required=true),
            @HandlerInput(name="testStr", type=String.class, required=true)},
        output={
        @HandlerOutput(name="contain", type=Boolean.class)})
    public static void containedIn(HandlerContext handlerCtx) {
        List list = (List)handlerCtx.getInputValue("list");

        boolean contain = (list==null) ? false : list.contains(handlerCtx.getInputValue("testStr"));
        handlerCtx.setOutputValue("contain",  contain);
    }


    /**
     *	<p> Compare if 2 objects is equal </p>
     *
     *  <p> Input value: "obj1" -- Type: <code>Object</code> 
     *  <p> Input value: "obj2" -- Type: <code>Object</code>
     *  <p> Output value: "equal" -- Type: <code>Object</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="compare",
    	input={
	    @HandlerInput(name="obj1", type=Object.class, required=true),
            @HandlerInput(name="obj2", type=Object.class, required=true)},
        output={
            @HandlerOutput(name="objEqual", type=Boolean.class)})
    public static void compare(HandlerContext handlerCtx) {
        boolean ret = false;
        Object obj1 = (Object) handlerCtx.getInputValue("obj1");
        Object obj2 = (Object) handlerCtx.getInputValue("obj2");
        if (obj1 != null){
            ret = obj1.equals(obj2);
        }else{
            if (obj2 == null)
                ret = true;
        }
        handlerCtx.setOutputValue("objEqual", ret);        
    }
    
    /**
     * <p> This method displays the save successful message when the page refresh.
     * @param handlerCtx The HandlerContext.
     */
   @Handler(id="prepareSuccessfulMsg")
    public static void prepareSuccessful(HandlerContext handlerCtx){
        GuiUtil.prepareSuccessful(handlerCtx);
    }

    /** 
     * <p> This method sets the attributes that will be used by the alert component
     *     display the message to user.
     *     If type is not specifed, it will be 'info' by default.
     * <p> Input value: "summary" -- Type: <code>java.lang.String</code></p>
     * <p> Input value: "type" -- Type: <code>java.lang.String</code></p>
     * <p> Input value: "detail" -- Type: <code>java.lang.String</code></p>
     * @param handlerCtx The HandlerContext.
     */
     @Handler(id="prepareAlertMsg",
     input={
        @HandlerInput(name="summary", type=String.class, required=true),
        @HandlerInput(name="type",  type=String.class),
        @HandlerInput(name="detail",  type=String.class)
      })
    public static void prepareAlertMsg(HandlerContext handlerCtx){
        String summary = (String) handlerCtx.getInputValue("summary");
        String type = (String) handlerCtx.getInputValue("type");
        String detail = (String) handlerCtx.getInputValue("detail");
        GuiUtil.prepareAlert(type, summary, detail);
    }

    /**
     *	<p> This handler will test if a String starts with another String.</p>
     */
    @Handler(id="startsWith",
    	input={
            @HandlerInput(name="testStr", type=String.class, required=true),
            @HandlerInput(name="pattern", type=String.class, required=true)},
        output={
            @HandlerOutput(name="result", type=Boolean.class)})
    public static void startsWith(HandlerContext handlerCtx) {
        String testStr = ((String) handlerCtx.getInputValue("testStr"));
        String pattern = ((String) handlerCtx.getInputValue("pattern"));
        handlerCtx.setOutputValue("result", testStr.startsWith(pattern));
    }

    /**
     * <p> This method decodes a String using "UTF-8" as default
     * if scheme is not specified.
     */
     @Handler(id="decodeString",
     input={
        @HandlerInput(name="str", type=String.class, required=true),
        @HandlerInput(name="scheme", type=String.class)},
     output={
        @HandlerOutput(name="output", type=String.class)
	    })
    public static void decodeString(HandlerContext handlerCtx) {
        String str = (String) handlerCtx.getInputValue("str");
        String scheme = (String) handlerCtx.getInputValue("scheme");
        if (GuiUtil.isEmpty(str)){
            handlerCtx.setOutputValue("output", "");
            return;
        }
        
        if (GuiUtil.isEmpty(scheme))
            scheme = "UTF-8";
        try{
            String output=URLDecoder.decode(str, scheme);
            handlerCtx.setOutputValue("output", output);
        }catch(UnsupportedEncodingException ex) {
            handlerCtx.setOutputValue("output", str);
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.decodeString") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
     }

    
    @Handler(id="roundTo2DecimalPoint",
    input={
        @HandlerInput(name="input", type=Double.class)},
    output={
        @HandlerOutput(name="output", type=String.class)
    })
    public static void roundTo2DecimalPoint(HandlerContext handlerCtx) {
        DecimalFormat df= new DecimalFormat();
        df.setMaximumFractionDigits(2);
        try{
            Double input = (Double) handlerCtx.getInputValue("input");
            String output = (input==null)? "": df.format(input);
            handlerCtx.setOutputValue("output", output);
        }catch (Exception ex){
            handlerCtx.setOutputValue("output", "");
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.roundTo2DecimalPoint") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
    }

    /*
     * Add an empty string as the first element to the list.
     * This is useful as the labels/values of a dropdown list, where user is not required
     * to select a value in the list.  eg. virtualServer in the deployment screen,
     * defaultWebModule in the server etc.
     */

    @Handler(id = "addEmptyFirstElement",
    input = {
        @HandlerInput(name = "in", type = List.class, required = true)},
    output = {
        @HandlerOutput(name = "out", type = List.class)
    })
    public static void addEmptyFirstElement(HandlerContext handlerCtx) {
        List<String> in = (List) handlerCtx.getInputValue("in");
        ArrayList ar = null;
        if (in == null){
             ar = new ArrayList();
        }else{
             ar = new ArrayList(in);
        }
        ar.add(0, "");
        handlerCtx.setOutputValue("out", ar);
    }



    @Handler(id = "getListBoxOptions",
    input = {
        @HandlerInput(name = "availableList", type = List.class, required = true),
        @HandlerInput(name = "selectedCommaString", type = String.class, required = true),
        @HandlerInput(name = "addEmptyFirstChoice", type = String.class, defaultValue = "true")},
    output = {
        @HandlerOutput(name = "availableListResult", type = List.class),
        @HandlerOutput(name = "selectedOptions", type = String[].class)
    })
    public static void getListBoxOptions(HandlerContext handlerCtx) {
        String selectedCommaString = (String) handlerCtx.getInputValue("selectedCommaString");
        List<String> availableList = (List) handlerCtx.getInputValue("availableList");
        String addEmptyFirstChoice = (String) handlerCtx.getInputValue("addEmptyFirstChoice");

        String[] selectedOptions = null;
        if ("true".equals(addEmptyFirstChoice)){
            if (availableList == null){
                availableList = new ArrayList();
            }
            availableList.add(0, "");
        }
        if (availableList != null && (availableList.size() > 0) ) {
            selectedOptions = GuiUtil.stringToArray(selectedCommaString, ",");
            if ( !(selectedOptions.length > 0)) {
                //None is selected by default
                selectedOptions = new String[]{availableList.get(0)};
            }
        }
        handlerCtx.setOutputValue("availableListResult", availableList);
        handlerCtx.setOutputValue("selectedOptions", selectedOptions);
    }



    @Handler(id = "convertArrayToCommaString",
    input = {
        @HandlerInput(name = "array", type = String[].class, required = true)},
    output = {
        @HandlerOutput(name = "commaString", type = String.class)})
    public static void convertArrayToCommaString(HandlerContext handlerCtx) {
        String[] array = (String[])handlerCtx.getInputValue("array");
        String commaString = "";
        if( (array != null) && array.length > 0 ) {
                commaString = GuiUtil.arrayToString(array, ",");
        }
        handlerCtx.setOutputValue("commaString", commaString);
    }
    
    @Handler(id = "convertListToCommaString",
    input = {
        @HandlerInput(name = "list", type = List.class, required = true)},
    output = {
        @HandlerOutput(name = "commaString", type = String.class)})
    public static void convertListToCommaString(HandlerContext handlerCtx) {
        List list = (List)handlerCtx.getInputValue("list");
        String commaString = "";
		if( (list != null) && list.size() > 0 ) {
			commaString = GuiUtil.listToString(list, ",");
		}
        handlerCtx.setOutputValue("commaString", commaString);
    }

    @Handler(id = "gf.resolveTokens",
    input = {
        @HandlerInput(name = "tokens", type = List.class, required = true),
        @HandlerInput(name = "endPoint", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "resolvedTokens", type = List.class)})
    public static void resolveTokens(HandlerContext handlerCtx) {
        List<String> tokens = (List<String>)handlerCtx.getInputValue("tokens");
        ArrayList<String> resolvedTokens = new ArrayList();

        String endPoint = (String)handlerCtx.getInputValue("endPoint");
        for (String token : tokens) 
            resolvedTokens.add(RestUtil.resolveToken(endPoint, token));
        handlerCtx.setOutputValue("resolvedTokens", resolvedTokens);
    }

    @Handler(id = "convertListToArray",
    input = {
        @HandlerInput(name = "list", type = List.class, required = true)},
    output = {
        @HandlerOutput(name = "array", type = String[].class)})
    public static void convertListToArray(HandlerContext handlerCtx) {
        List list = (List)handlerCtx.getInputValue("list");

        handlerCtx.setOutputValue("array", list.toArray(new String[list.size()]) );
    }

   /**
     *	<p> This handler takes in a string with delimiter and returns list
     */
    @Handler(id="convertStringtoList",
         input={
            @HandlerInput(name="str", type=String.class),
            @HandlerInput(name="delimiter", type=String.class, defaultValue=",")
            },
        output = {
            @HandlerOutput(name = "result", type = List.class)
            })
    public static void convertStringtoListHandler(HandlerContext handlerCtx) {
        List result = convertStringToList((String) handlerCtx.getInputValue("str"),
                (String) handlerCtx.getInputValue("delimiter"));
        handlerCtx.setOutputValue("result", result);
    }

    private static List<String> convertStringToList(String str, String delimiter) {
        List<String> result = new ArrayList();
        if (str != null) {
            if (delimiter == null) {
                delimiter = ",";
            }
            StringTokenizer tokens = new StringTokenizer(str, delimiter);
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken().trim();
                result.add(token);
            }
        }
        return result;
    }

    @Handler(id="convertStringToMap",
         input={
            @HandlerInput(name="str", type=String.class),
            @HandlerInput(name="delimiter", type=String.class)
            },
        output = {
            @HandlerOutput(name = "result", type = Map.class)
            })
    public static void convertStringToMap(HandlerContext handlerCtx) {
        Map<String, String> output = new HashMap<String,String>();
        List<String> list = convertStringToList((String) handlerCtx.getInputValue("str"),
                (String) handlerCtx.getInputValue("delimiter"));

        for (String item : list) {
            String[] parts = item.split("=");
            String key = parts[0];
            String value = "";
            if (parts.length > 1) {
                value = parts[1];
            }
            output.put(key, value);
        }

        handlerCtx.setOutputValue("result", output);
    }

    
     //This is the reserve of the above method.
    //We want to separator and display each jar in one line in the text box.
    @Handler(id = "formatStringsforDisplay",
    input = {
        @HandlerInput(name = "string", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "formattedString", type = String.class)})
    public static void formatStringsforDisplay(HandlerContext handlerCtx) {
        
        String values = (String) handlerCtx.getInputValue("string");
        if (values == null || GuiUtil.isEmpty(values.trim())) {
            handlerCtx.setOutputValue("formattedString", "");
        } else {
            String s1 = values.trim().replaceAll("\\.jar:", "\\.jar\\$\\{path.separator\\}");
            String s2 = s1.replaceAll("\\.jar;", "\\.jar\\$\\{path.separator\\}");
            String[] strArray = s2.split("\\$\\{path.separator\\}");
            StringBuilder result = new StringBuilder("");
            for (String s : strArray) {
                result.append(s).append("\n");
            }

            handlerCtx.setOutputValue("formattedString", result.toString().trim());


        }
    }
    
    //This converts any tab/NL etc to ${path.separator} before passing to backend for setting.
    //In domain.xml, it will be written out like  c:foo.jar${path.separator}c:bar.jar
    @Handler(id = "formatPathSeperatorStringsforSaving",
    input = {
        @HandlerInput(name = "string", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "formattedString", type = String.class)})
    public static void formatPathSeperatorStringsforSaving(HandlerContext handlerCtx) {
        String values = (String) handlerCtx.getInputValue("string");
        StringBuilder token = new StringBuilder("");
        String sep = "";
        if ((values != null) &&
                (values.trim().length() != 0)) {
            List<String> strList = GuiUtil.parseStringList(values, "\t\n\r\f");
            for(String nextToken : strList){
                token.append(sep).append(nextToken);
                sep = PATH_SEPARATOR;
            }
        }
        handlerCtx.setOutputValue("formattedString", token.toString());
    }    

    /**
     *
     */
    @Handler(id="addHandler",
    input={
        @HandlerInput(name="id", type=String.class, required=true),
        @HandlerInput(name="desc", type=String.class),
        @HandlerInput(name="class", type=String.class, required=true),
        @HandlerInput(name="method", type=String.class, required=true)
	})
    public static void addHandler(HandlerContext handlerCtx) {
	String id = (String) handlerCtx.getInputValue("id");
	String desc = (String) handlerCtx.getInputValue("desc");
	String cls = (String) handlerCtx.getInputValue("class");
	String meth = (String) handlerCtx.getInputValue("method");
	HandlerDefinition def = new HandlerDefinition(id);
	def.setHandlerMethod(cls, meth);
	if (desc != null) {
	    def.setDescription(desc);
	}
	LayoutDefinitionManager.addGlobalHandlerDefinition(def);
    }


    /**
     *	<p> A utility handler that resembles the for() method in Java.  Handler inside the for loop will be executed
     *  in a loop.  start index is specified by "start",  till less than "end".
     * eg. forLoop(start="1"  end="3" varName="foo"){}, handler inside the {} will be executed 2 times.
     *
     *  <p> Input value: "start" -- Type: <code>Integer</code> Start index, default to Zero is not specified
     *  <p> Input value: "end" -- Type: <code>Integer</code> End index.
     *  <p> Input value: "varName" -- Type: <code>String</code>  Variable to be replaced in the for loop by the index.
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="forLoop",
    	input={
	    @HandlerInput(name="start", type=String.class),
        @HandlerInput(name="end", type=Integer.class, required=true),
        @HandlerInput(name="varName", type=String.class, required=true)}
        )
    public static boolean forLoop(HandlerContext handlerCtx) {

        String startInt = (String) handlerCtx.getInputValue("start");
        int start = (startInt == null) ? 0 : Integer.parseInt(startInt);
        int end = ((Integer) handlerCtx.getInputValue("end")).intValue();
        String varName = ((String) handlerCtx.getInputValue("varName"));

        List<com.sun.jsftemplating.layout.descriptors.handler.Handler> handlers = handlerCtx.getHandler().getChildHandlers();
		if (handlers.size() > 0) {
            LayoutElement elt = handlerCtx.getLayoutElement();
            Map<String, Object> requestMap = handlerCtx.getFacesContext().getExternalContext().getRequestMap();
            for(int ix=start;  ix<=end; ix++){
                requestMap.put(varName, ix);
                //ignore whats returned by the handler.
                elt.dispatchHandlers(handlerCtx, handlers);
		    }
		}
        return false;
    }

    /**
     *	<p> This handler provides the foreach loop functionality.  You should
     *	    specify a request attribute 'var' that will be used as the key for
     *	    storing each token in the list.  You can then retreive each value
     *	    as the loop iterates by requesting the request scoped attribute
     *	    keyed by the value you suplied for 'var'.  You must also specify
     *	    the <code>List&lt;Object&gt;</code> to iterate over.</p>
     */
    @Handler(id="foreach",
	input={
	    @HandlerInput(name="var", type=String.class, required=false, defaultValue="idx"),
	    @HandlerInput(name="list", type=Collection.class, required=true) })
    public static boolean foreach(HandlerContext handlerCtx) {
	String var = (String) handlerCtx.getInputValue("var");
	Collection<Object> list = (Collection<Object>) handlerCtx.getInputValue("list");

	List<com.sun.jsftemplating.layout.descriptors.handler.Handler> handlers =
            handlerCtx.getHandler().getChildHandlers();
        if (handlers.size() > 0) {
            // We have child handlers in the loop... execute while we iterate
            LayoutElement elt = handlerCtx.getLayoutElement();
            Map<String, Object> requestMap = handlerCtx.getFacesContext().
                    getExternalContext().getRequestMap();
	    if (list != null) {
		for (Object obj : list) {
		    requestMap.put(var, obj);

		    // Ignore whats returned by the handler... we need to return
		    // false anyway to prevent children from being executed again
// FIXME: Consider supporting a "break" type of functionality
		    elt.dispatchHandlers(handlerCtx, handlers);
		}
	    }
        }

        // This will prevent the child handlers from executing again
	return false;
    }

    /**
     *	<p> This handler returns the <code>Set</code> of entries for the given
     *	    <code>Map</code>.</p>
     */
    @Handler(id="mapEntrySet",
	input = {
	    @HandlerInput(name="map", type=Map.class, required=true)},
	output = {
	    @HandlerOutput(name="set", type=Set.class)})
    public static void mapEntrySet(HandlerContext handlerCtx) {
        Map map = (Map) handlerCtx.getInputValue("map");
        handlerCtx.setOutputValue("set", map.entrySet());
    }

    /**
     *	<p> This handler returns the <code>Set</code> of keys for the given
     *	    <code>Map</code>.</p>
     */
    @Handler(id="mapValues",
	input = {
	    @HandlerInput(name="map", type=Map.class, required=true)},
	output = {
	    @HandlerOutput(name="values", type=Object.class)})
    public static void mapValues(HandlerContext handlerCtx) {
        Map map = (Map) handlerCtx.getInputValue("map");
        Object xx = map.values();
        handlerCtx.setOutputValue("values", xx);
    }
    
    @Handler(id = "convertStrToBoolean",
    input = {
        @HandlerInput(name = "str", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "out", type = Boolean.class)})
    public static void convertStrToBoolean(HandlerContext handlerCtx) {
        String str = (String) handlerCtx.getInputValue("str");
        handlerCtx.setOutputValue("out", "true".equals(str));
    }


    @Handler(id="gf.logger",
    input={
        @HandlerInput(name="logString", type=String.class , defaultValue=""),
        @HandlerInput(name="level", type=String.class , defaultValue="INFO")
    },
    output={
        @HandlerOutput(name="string", type=String.class)
    })
    public static void logger(HandlerContext handlerCtx) {

        GuiUtil.getLogger().log(
                Level.parse((String)handlerCtx.getInputValue("level")),
                "" +handlerCtx.getInputValue("logString"));
    }

    /**
     *	<p> This method returns a new UIViewRoot with the basic JSFT settings
     *	    from the current ViewRoot.  If you intend to set this before the
     *	    current view is created (in an effort to swap out the UIViewRoot),
     *	    you should do so during the initPage event (take care to only do
     *	    this during the first request, or you might lose all child
     *	    components).</p>
     */
    @Handler(id = "createDefaultViewRoot",
	output = {
	    @HandlerOutput(name="viewRoot", type=UIViewRoot.class)})
    public static void createDefaultViewRoot(HandlerContext handlerCtx) {
	UIViewRoot oldVR = handlerCtx.getFacesContext().getViewRoot();
	UIViewRoot newVR = new UIViewRoot();
	newVR.setViewId(oldVR.getViewId());
	ViewRootUtil.setLayoutDefinitionKey(newVR, ViewRootUtil.getLayoutDefinitionKey(oldVR));
	newVR.setLocale(oldVR.getLocale());
	newVR.setRenderKitId(oldVR.getRenderKitId());
        handlerCtx.setOutputValue("viewRoot", newVR);
    }

    /**
     *	<p> This handler invokes the {@link GuiUtil#genId(String)} method and
     *	    returns the result.</p>
     */
    @Handler(id="gf.encodeId",
    	input={
            @HandlerInput(name="id", type=String.class, required=true)},
        output={
            @HandlerOutput(name="result", type=String.class)})
    public static void encodeId(HandlerContext handlerCtx) {
        String id = ((String) handlerCtx.getInputValue("id"));
        handlerCtx.setOutputValue("result", GuiUtil.genId(id));
    }

    /**
     *	This method converts a Map into a list of Map with keyName and ValueName.  This is suitable for table dislay.
     */
    @Handler(id="gf.convertMapToListOfMap",
            input={
                    @HandlerInput(name="map", type=Map.class, required=true),
                    @HandlerInput(name="keyName", type=String.class, defaultValue = "key"),
                    @HandlerInput(name="valueName", type=String.class, defaultValue = "value")},
            output={
                    @HandlerOutput(name="result", type=List.class)})
    public static void convertMapToListOfMap(HandlerContext handlerCtx) {
        Map map = ((Map) handlerCtx.getInputValue("map"));
        String keyName = ((String) handlerCtx.getInputValue("keyName"));
        String valueName = ((String) handlerCtx.getInputValue("valueName"));

        List result = new ArrayList();

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            Map oneRow = new HashMap();
            oneRow.put(keyName, entry.getKey());
            oneRow.put(valueName, entry.getValue());
            result.add(oneRow);
        }

        handlerCtx.setOutputValue("result", result);
    }

    /**
     *	<p> This handler will convert a Java object to JSON by using
     *	    {@link JSONUtil#javaToJSON}.</p>
     */
    @Handler(id="javaToJSON",
    	input={
            @HandlerInput(name="obj", type=Object.class, required=true),
            @HandlerInput(name="depth", type=Integer.class, required=false, defaultValue="9")},
        output={
            @HandlerOutput(name="json", type=String.class)})
    public static void javaToJSON(HandlerContext handlerCtx) {
        Object obj = ((Object) handlerCtx.getInputValue("obj"));
        int depth = ((Integer) handlerCtx.getInputValue("depth"));
        handlerCtx.setOutputValue("json", JSONUtil.javaToJSON(obj, depth));
    }
    
    @Handler(id="gf.createPropertyString",
            input={ 
                @HandlerInput(name="properties", type=List.class, required=true)
            },
            output={
                @HandlerOutput(name="string", type=String.class)
            }
    )
    public static void createPropertyString(HandlerContext handlerCtx) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        List<Map<String, String>> properties = (List<Map<String, String>>)handlerCtx.getInputValue("properties");
        for (Map<String, String> property : properties) {
            sb.append(sep)
                    .append(property.get("name"))
                    .append("=")
                    .append(escapePropertyValue(property.get("value")))
                    ;
            sep = ":";
        }
        
        handlerCtx.setOutputValue("string", sb.toString());
    }
    


    /* This is copied from within javaToJSON() */
    public static String escapePropertyValue(String str){
        String chStr;
        int len;
        StringCharacterIterator it = new StringCharacterIterator(str);
        char ch = it.first();
        StringBuilder builder =  new StringBuilder(str.length() << 2);
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
        return builder.toString();
    }

    private static final String PATH_SEPARATOR = "${path.separator}";
}
