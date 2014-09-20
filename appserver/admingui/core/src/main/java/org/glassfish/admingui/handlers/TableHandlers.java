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
 * TableHandler.java
 *
 * Created on August 10, 2006, 2:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author anilam
 */

package org.glassfish.admingui.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.component.dataprovider.MultipleListDataProvider;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import org.glassfish.admingui.common.util.GuiUtil;

import com.sun.webui.jsf.component.TableRowGroup;

import java.util.HashMap;
import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Iterator;

import com.sun.data.provider.RowKey;

public class TableHandlers {
    /** Creates a new instance of TableHandler */
    public TableHandlers() {
    }
    

    
  /**
     *	<p> This handler looks at the input TableRowGroup, checks which row is selected, and returns a list of the Map. 
     *  <p> Each Map corresponding to one single row of the table.  
     *  <p> This method only works for the table where each row consists of one single map since it only looks at the
     *  <p> first element that is returned by the getObject() method of <code>MultipleListDataProvider<code>.
     * 
     *  <p> Input  value: "TableRowGroup" -- Type: <code> com.sun.webui.jsf.component.TableRowGroup</code></p>
     *  <p> Input  value: "selectedRows" -- Type: <code> java.util.List</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getSelectedSingleMapRows",
    input={
        @HandlerInput(name="TableRowGroup", type=TableRowGroup.class, required=true)},
    output={
        @HandlerOutput(name="selectedRows", type=List.class)})
        public static void getSelectedSingleMapRows(HandlerContext handlerCtx) {
        TableRowGroup trg = (TableRowGroup)handlerCtx.getInputValue("TableRowGroup");
        MultipleListDataProvider dp = (MultipleListDataProvider)trg.getSourceData();
        List selectedList = new ArrayList();
        try {
           RowKey[] rowKeys  = trg.getSelectedRowKeys();
           
           for(int i=0; i<rowKeys.length; i++){
               Object[] multiDataRows =  (Object[]) dp.getObject(rowKeys[i]);
               Object oneMap = multiDataRows[0];
               selectedList.add(oneMap);
            }
            handlerCtx.setOutputValue("selectedRows", selectedList);
        }catch(Exception ex){
            GuiUtil.prepareException(handlerCtx, ex);
        }
       
    }

    /**
     *	<p> This handler returns the selected row keys.</p>
     *
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getSelectedTableRowKeys",
	input={
	    @HandlerInput(name="tableRowGroup", type=TableRowGroup.class, required=true)},
	output={
	    @HandlerOutput(name="rowKeys", type=RowKey[].class)})
    public static void getSelectedTableRowKeys(HandlerContext handlerCtx) {
	TableRowGroup trg =
	    (TableRowGroup) handlerCtx.getInputValue("tableRowGroup");
	handlerCtx.setOutputValue("rowKeys", trg.getSelectedRowKeys());
    }

    /**
     *	<p> This handler deletes the given <code>RowKey</code>s.</p>
     *
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="deleteTableRows",
	input={
	    @HandlerInput(name="tableRowGroup", type=TableRowGroup.class, required=true),
	    @HandlerInput(name="rowKeys", type=RowKey[].class, required=true)})
    public static void deleteTableRows(HandlerContext handlerCtx) {
	TableRowGroup trg =
	    (TableRowGroup) handlerCtx.getInputValue("tableRowGroup");
	RowKey[] keys = (RowKey []) handlerCtx.getInputValue("rowKeys");
        MultipleListDataProvider dp =
	    (MultipleListDataProvider) trg.getSourceData();
	for (RowKey key : keys) {
	    dp.removeRow(key);
	}
    }

    /**
     *	<p> This handler commits the changes to a <code>TableRowGroup</code>'s
     *	    DataProvider.</p>
     *
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="commitTableRowGroup",
	input={
	    @HandlerInput(name="tableRowGroup", type=TableRowGroup.class, required=true)})
    public static void commitTableRowGroup(HandlerContext handlerCtx) {
	TableRowGroup trg =
	    (TableRowGroup) handlerCtx.getInputValue("tableRowGroup");
        MultipleListDataProvider dp =
	    (MultipleListDataProvider) trg.getSourceData();
	dp.commitChanges();
    }
    
    /**
     *	<p> This handler takes in a HashMap, the name-value pair being the Properties.
     *  It turns each name-value pair to one hashMap, representing one row of table data, 
     *  and returns the list of Map. 
     *
     *  <p> Input value: "Properties" -- Type: <code>java.util.Map</code>/</p>
     *  <p> Output value: "TableList" -- Type: <code>java.util.List</code>/</p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getTableListFromProperties",
    input={
        @HandlerInput(name="Properties", type=Map.class, required=true)},
    output={
        @HandlerOutput(name="TableList", type=List.class)})
        public static void getTableListFromProperties(HandlerContext handlerCtx) {
        List data = new ArrayList();
        Map<String, Object> props = (Map)handlerCtx.getInputValue("Properties");	
        if(props != null ){
            for(Map.Entry<String,Object> e : props.entrySet()){
                HashMap oneRow = new HashMap();
                Object value = e.getValue();
                String valString = (value==null)? "" : value.toString();
                oneRow.put("name", e.getKey());
                oneRow.put("value", valString);
                oneRow.put("selected", false);
                data.add(oneRow);
            }
        }
        handlerCtx.setOutputValue("TableList", data);
    }  
    
/**
     *	<p> This handler takes TableRowGroup as input and returns a List of Map objects. 
     *  <p> The List returned contains Map objects with each Map representing one single row.  
     *  <p> This method only works for tables where each row consists of one single map 
     * 
     *  <p> Input  value: "TableRowGroup" -- Type: <code> com.sun.webui.jsf.component.TableRowGroup</code></p>
     *  <p> Output  value: "Rows" -- Type: <code> java.util.List</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getAllSingleMapRows",
    input={
        @HandlerInput(name="TableRowGroup", type=TableRowGroup.class, required=true)},
    output={
        @HandlerOutput(name="Rows", type=List.class)})
        public static void getAllSingleMapRows(HandlerContext handlerCtx) {
        
        TableRowGroup trg = (TableRowGroup)handlerCtx.getInputValue("TableRowGroup");
        MultipleListDataProvider dp = (MultipleListDataProvider)trg.getSourceData();
        List data = dp.getLists();
        try {
            handlerCtx.setOutputValue("Rows", data.get(0));
        }catch(Exception ex){
            //TODO alert user, log exception
            System.out.println("!!!! getAllSingleMapRows() Throws Exception: " + ex.toString());
        }
        
        
        
        
    }
    
    /**
     *	<p> This handler adds one row to  table
     *  <p> Input  value: "TableRowGroup" -- Type: <code> com.sun.webui.jsf.component.TableRowGroup</code></p>
     *  <p> Input value: "NameList" -- Type:<code>java.util.List</code></p>
     *  <p> Input value: "DefaultValueList" -- Type:<code>java.util.List</code></p>
     *  <p> Input value: "HasSelected" -- Type:<code>java.lang.Boolean</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="addRowToTable",
    input={
        @HandlerInput(name="TableRowGroup", type=TableRowGroup.class, required=true),
        @HandlerInput(name="NameList", type=List.class),
        @HandlerInput(name="HasSelected", type=Boolean.class),
        @HandlerInput(name="DefaultValueList", type=List.class)} )
    public static void addRowToTable(HandlerContext handlerCtx) {
        TableRowGroup trg = (TableRowGroup)handlerCtx.getInputValue("TableRowGroup");
        List names = (List)handlerCtx.getInputValue("NameList");
        List defaults = (List)handlerCtx.getInputValue("DefaultValueList");
        Boolean hasSelected = (Boolean)handlerCtx.getInputValue("HasSelected");
        MultipleListDataProvider dp = (MultipleListDataProvider)trg.getSourceData();
        List data = dp.getLists();
        ListIterator li = data.listIterator();
        if (li.hasNext()) {
	    // Get the first List and create a new Map to represent the row
            List list = (List) li.next();
            Map<String, Object> map = new HashMap<String, Object>();
	    if (names != null) {
		// Fill it up...
		if (defaults != null) {
		    if (names.size() != defaults.size()) {
			throw new IllegalArgumentException("NameList.size("
			    + names.size()
			    + ") does not match DefaultValueList.size("
			    + defaults.size() + ")!");
		    }
		    ListIterator ni = names.listIterator();
		    ListIterator dv = defaults.listIterator();
		    while (ni.hasNext() && dv.hasNext()) {
			String name = (String) ni.next();
			String value = (String) dv.next();
                        if ( "#{true}".equals(value)){
                            map.put(name, true);
                        }else
                        if ( "#{false}".equals(value)){
                            map.put(name, false);
                        }else{
                            map.put(name, value);
                        }
		    }
		} else {
		    ListIterator ni = names.listIterator();
		    while(ni.hasNext()) {
			String name = (String) ni.next();
			map.put(name, "");
		    }
		}
	    } else if (defaults == null) {
		// Use a simple name/value default...
                map.put("name", "");
                map.put("value", "");
            }
	    // Add the Map to the List
            list.add(0, map);

	    // See if we have more lists of map... if so put selected in it
	    if (li.hasNext()) {
		list = (List) li.next();
		map = new HashMap<String, Object>();
		list.add(0, map);
	    }

	    // Add selected column (either to the 1st or 2nd map)
            if (hasSelected == null) {
		map.put("selected", false);
            } else {
                if (hasSelected.booleanValue()) {
                    map.put("selected", false);
                }
            }

	    // Add something to the remaining Maps (if any)
	    while (li.hasNext()) {
		list = (List) li.next();
		map = new HashMap<String, Object>();
		list.add(0, map);
	    }
        }
    }

  /**
     *	<p> This handler converts the table list to arraylist.</p>
     *
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="convertListToArrayList",
	input={
	    @HandlerInput(name="TableList", type=List.class, required=true),
            @HandlerInput(name="Name", type=String.class)},        
        output={
            @HandlerOutput(name="NameList",     type=ArrayList.class)})
        public static void convertListToArrayList(HandlerContext handlerCtx) {
        List tableList = (List)handlerCtx.getInputValue("TableList");
        String name = (String)handlerCtx.getInputValue("Name");
        if (GuiUtil.isEmpty(name))
            name = "name";
        if(tableList != null) {
            ListIterator li = tableList.listIterator();
            ArrayList names = new ArrayList();
            while(li.hasNext()) {
                Map props = (Map)li.next();
                String val= (String) props.get(name);
                if (!GuiUtil.isEmpty(val))
                    names.add(val);
            }
            handlerCtx.setOutputValue("NameList", names);
        }
    }      
    
 /**
     *	<p> This handler returns the properties to be removed and added.</p>
     *
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getAddRemoveProps",
	input={
	    @HandlerInput(name="NewList", type=List.class, required=true),
            @HandlerInput(name="OldList", type=Map.class, required=true),
            @HandlerInput(name="NameList", type=ArrayList.class, required=true)},
	output={
	    @HandlerOutput(name="AddProps", type=Map.class),
            @HandlerOutput(name="RemoveProps", type=ArrayList.class)})
        public static void getAddRemoveProps(HandlerContext handlerCtx) {
        List newList = (List)handlerCtx.getInputValue("NewList");
        ArrayList names = (ArrayList)handlerCtx.getInputValue("NameList");
        Map<String, String> oldList = (Map)handlerCtx.getInputValue("OldList");
        ListIterator li = newList.listIterator();
        ArrayList removeProps = new ArrayList();
        Iterator iter = oldList.keySet().iterator();
        Map addProps = new HashMap<String, String>();
        while(li.hasNext()) {
            Map props = (Map)li.next();
            if(!oldList.containsKey(props.get("name"))){
                String name = (String)props.get("name");
                if (name != null && (! name.trim().equals(""))) {
                    addProps.put((String)props.get("name"), (String)props.get("value"));
                }
            }
            if(oldList.containsKey(props.get("name"))){
                String oldvalue = (String)oldList.get(props.get("name"));
                String newvalue = (String)props.get("value");
                if(!oldvalue.equals(newvalue)) {
                    removeProps.add((String)props.get("name"));
                    String name = (String)props.get("name");
                    if (name != null && (! name.trim().equals(""))) {
                        addProps.put((String)props.get("name"), (String)props.get("value"));
                    }
                }
                
            }
        }
        if(iter != null ){
            while(iter.hasNext()){
                Object key = iter.next();
                if(!names.contains(key)) {
                    removeProps.add((String)key);
                }
            }
        }
        handlerCtx.setOutputValue("AddProps", addProps);
        handlerCtx.setOutputValue("RemoveProps", removeProps);
    }
    
  /**
     * <p> This handler converts the table List to a Property map.
     *
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="convertRowsToProperties",
	input={
	    @HandlerInput(name="NewList", type=List.class, required=true)},
	output={
	    @HandlerOutput(name="AddProps", type=Map.class)})
        public static void convertRowsToProperties(HandlerContext handlerCtx) {
        List newList = (List)handlerCtx.getInputValue("NewList");
        ListIterator li = newList.listIterator();
        Map addProps = new HashMap<String, String>();
        while(li.hasNext()) {
            Map props = (Map)li.next();
               String name = (String)props.get("name");
                if (name != null && (! name.trim().equals(""))) {
                    if (addProps.containsKey(name)){
                        //duplicate property name, give error
                        GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.duplicatePropTableKey" , new Object[]{name}));
                        return;
                    }
                    addProps.put(name, (String)props.get("value"));
                }
        }
        handlerCtx.setOutputValue("AddProps", addProps);
    }    

  /**
     * <p> This handler converts the table List to a Properties map.
     *
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getProperties",
	input={
	    @HandlerInput(name="NewList", type=List.class, required=true)},
	output={
	    @HandlerOutput(name="AddProps", type=Map.class)})
        public static void getProperties(HandlerContext handlerCtx) {
        List newList = (List)handlerCtx.getInputValue("NewList");
        ListIterator li = newList.listIterator();
        Map addProps = new Properties();
        while(li.hasNext()) {
            Map props = (Map)li.next();
               String name = (String)props.get("name");
                if (name != null && (! name.trim().equals(""))) {
					String value = (String)props.get("value");
                	if (value != null && (! value.trim().equals(""))) {
                    	addProps.put(name, value);
					}
                }
        }
        handlerCtx.setOutputValue("AddProps", addProps);
    }    
}
