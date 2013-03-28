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
package org.glassfish.admin.rest.resources.custom;

import com.sun.enterprise.server.logging.logviewer.backend.LogFilter;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.LogManager;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import org.glassfish.admin.rest.logviewer.LogRecord;

/**
 * REST resource to get Log records
 * simple wrapper around internal  LogFilter query class
 *
 * @author ludovic Champenois
 */
public class StructuredLogViewerResource {

    protected ServiceLocator habitat = Globals.getDefaultBaseServiceLocator();
    
    @Context
    protected ServiceLocator injector;

    @Path("lognames/")
    public LogNamesResource getLogNamesResource() {
        LogNamesResource resource = injector.createAndInitialize(LogNamesResource.class);
        return resource;
    }

    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public String getJson(
            @QueryParam("logFileName") @DefaultValue("${com.sun.aas.instanceRoot}/logs/server.log") String logFileName,
            @QueryParam("startIndex") @DefaultValue("-1") long startIndex,
            @QueryParam("searchForward") @DefaultValue("false") boolean searchForward,
            @QueryParam("maximumNumberOfResults") @DefaultValue("40") int maximumNumberOfResults,
            @QueryParam("onlyLevel") @DefaultValue("false") boolean onlyLevel,
            @QueryParam("fromTime") @DefaultValue("-1") long fromTime,
            @QueryParam("toTime") @DefaultValue("-1") long toTime,
            @QueryParam("logLevel") @DefaultValue("INFO") String logLevel,
            @QueryParam("anySearch") @DefaultValue("") String anySearch,
            @QueryParam("listOfModules") String listOfModules, //default value is empty for List
            @QueryParam("instanceName") @DefaultValue("") String instanceName) throws IOException {

        return getWithType(
                logFileName,
                startIndex,
                searchForward,
                maximumNumberOfResults,
                fromTime,
                toTime,
                logLevel, onlyLevel, anySearch, listOfModules, instanceName, "json");

    }

    @GET
    @Produces({MediaType.APPLICATION_XML})
    public String getXML(
            @QueryParam("logFileName") @DefaultValue("${com.sun.aas.instanceRoot}/logs/server.log") String logFileName,
            @QueryParam("startIndex") @DefaultValue("-1") long startIndex,
            @QueryParam("searchForward") @DefaultValue("false") boolean searchForward,
            @QueryParam("maximumNumberOfResults") @DefaultValue("40") int maximumNumberOfResults,
            @QueryParam("onlyLevel") @DefaultValue("true") boolean onlyLevel,
            @QueryParam("fromTime") @DefaultValue("-1") long fromTime,
            @QueryParam("toTime") @DefaultValue("-1") long toTime,
            @QueryParam("logLevel") @DefaultValue("INFO") String logLevel,
            @QueryParam("anySearch") @DefaultValue("") String anySearch,
            @QueryParam("listOfModules") String listOfModules, //default value is empty for List,
            @QueryParam("instanceName") @DefaultValue("") String instanceName) throws IOException {

        return getWithType(
                logFileName,
                startIndex,
                searchForward,
                maximumNumberOfResults,
                fromTime,
                toTime,
                logLevel, onlyLevel, anySearch, listOfModules, instanceName, "xml");

    }

    private String getWithType(
            String logFileName,
            long startIndex,
            boolean searchForward,
            int maximumNumberOfResults,
            long fromTime,
            long toTime,
            String logLevel, boolean onlyLevel, String anySearch, String listOfModules,
            String instanceName,
            String type) throws IOException {
        if (habitat.getService(LogManager.class) == null) {
            //the logger service is not install, so we cannot rely on it.
            //return an error
            throw new IOException("The GlassFish LogManager Service is not available. Not installed?");
        }
        
        List<String> modules = new ArrayList<String>();
        if ((listOfModules != null) && !listOfModules.isEmpty()) {
            modules.addAll(Arrays.asList(listOfModules.split(",")));
            
        }

        Properties nameValueMap = new Properties();

        boolean sortAscending = true;
        if (!searchForward) {
            sortAscending = false;
        }
        LogFilter logFilter = habitat.getService(LogFilter.class);
        if (instanceName.equals("")) {
            final AttributeList result = logFilter.getLogRecordsUsingQuery(logFileName,
                    startIndex,
                    searchForward, sortAscending,
                    maximumNumberOfResults,
                    fromTime == -1 ? null : new Date(fromTime),
                    toTime == -1 ? null : new Date(toTime),
                    logLevel, onlyLevel, modules, nameValueMap, anySearch);
            return convertQueryResult(result, type);
        } else {
            final AttributeList result = logFilter.getLogRecordsUsingQuery(logFileName,
                    startIndex,
                    searchForward, sortAscending,
                    maximumNumberOfResults,
                    fromTime == -1 ? null : new Date(fromTime),
                    toTime == -1 ? null : new Date(toTime),
                    logLevel, onlyLevel, modules, nameValueMap, anySearch, instanceName);
            return convertQueryResult(result, type);
        }

    }

    private <T> List<T> asList(final Object list) {
        return (List<T>) List.class.cast(list);
    }

/*    private String quoted(String s) {
        return "\"" + s + "\"";
    }*/

    private String convertQueryResult(final AttributeList queryResult, String type) {
        // extract field descriptions into a String[]
        StringBuilder sb = new StringBuilder();
        String sep = "";
        if (type.equals("json")) {
            sb.append("{\"records\": [");
        } else {
            sb.append("<records>\n");
        }

	if (queryResult.size() > 0) {
	    final AttributeList fieldAttrs = (AttributeList) ((Attribute) queryResult.get(0)).getValue();
	    String[] fieldHeaders = new String[fieldAttrs.size()];
	    for (int i = 0; i < fieldHeaders.length; ++i) {
		final Attribute attr = (Attribute) fieldAttrs.get(i);
		fieldHeaders[i] = (String) attr.getValue();
	    }

	    List<List<Serializable>> srcRecords = asList(((Attribute) queryResult.get(1)).getValue());

	    // extract every record
	    for (int recordIdx = 0; recordIdx < srcRecords.size(); ++recordIdx) {
		List<Serializable> record = srcRecords.get(recordIdx);

		assert (record.size() == fieldHeaders.length);
		//Serializable[] fieldValues = new Serializable[fieldHeaders.length];

		LogRecord rec = new LogRecord();
		int fieldIdx = 0;
		rec.setRecordNumber(((Long) record.get(fieldIdx++)).longValue());
		rec.setLoggedDateTime((Date) record.get(fieldIdx++));
		rec.setLoggedLevel((String) record.get(fieldIdx++));
		rec.setProductName((String) record.get(fieldIdx++));
		rec.setLoggerName((String) record.get(fieldIdx++));
		rec.setNameValuePairs((String) record.get(fieldIdx++));
		rec.setMessageID((String) record.get(fieldIdx++));
		rec.setMessage((String) record.get(fieldIdx++));
		if (type.equals("json")) {
		    sb.append(sep);
		    sb.append(rec.toJSON());
		    sep = ",";
		} else {
		    sb.append(rec.toXML());

		}
	    }
	}

        if (type.equals("json")) {
            sb.append("]}\n");
        } else {
            sb.append("\n</records>\n");

        }

        return sb.toString();
    }
}
