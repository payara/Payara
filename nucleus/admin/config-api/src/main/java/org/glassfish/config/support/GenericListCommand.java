/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.common.util.admin.GenericCommandModel;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.*;

/**
 * Generic list command implementation.
 *
 * @author Jerome Dochez
 * @author Tom Mueller
 */
@PerLookup
public class GenericListCommand  extends GenericCrudCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    @Param(primary=true, optional=true)
    String name;
    @Param(name="long", shortName="l", defaultValue="false", optional=true) 
    boolean longOpt;
    @Param(name="header", shortName="h", defaultValue="true", optional=true)
    boolean headerOpt;
    @Param(name="output", shortName="o", optional=true)
    String outputOpts[];
    
    CommandModel cmdModel;
    ConfigModel targetModel;
    Listing listing;
    
//    @AccessRequired.To("read")
    private ConfigBeanProxy parentBean;

    @Override
    public void postConstruct() {

        super.postConstruct();

        listing = targetMethod.getAnnotation(Listing.class);
        resolverType = listing.resolver();
        try {
	    // we pass false for "useAnnotations" as the @Param declarations on
	    // the target type are not used for the List method parameters.
            cmdModel = new GenericCommandModel(targetType, false, null, listing.i18n(),
                    new LocalStringManagerImpl(targetType),
                    habitat.<DomDocument>getService(DomDocument.class), commandName, 
                    false, listing.resolver(), GenericListCommand.class);
            targetModel = habitat.<DomDocument>getService(DomDocument.class).buildModel(targetType);
            if (logger.isLoggable(level)) {
                for (String paramName : cmdModel.getParametersNames()) {
                    CommandModel.ParamModel param = cmdModel.getModelFor(paramName);
                    logger.log(Level.FINE, "I take {0} parameters", param.getName());
                }
            }
        } catch(Exception e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCreateCommand.command_model_exception",
                    "Exception while creating the command model for the generic command {0} : {1}",
                    commandName, e.getMessage());
            LogHelper.log(logger, Level.SEVERE,ConfigApiLoggerInfo.GENERIC_CREATE_CMD_FAILED, e, commandName);
            throw new RuntimeException(msg, e);

        }      
    }

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final Collection<AccessCheck> checks = new ArrayList<AccessCheck>();
        checks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(parentBean), "read"));
        if (longOpt) {
            try {
                List<ConfigBeanProxy> children = (List<ConfigBeanProxy>) targetMethod.invoke(parentBean);
                for (ConfigBeanProxy child : children) {
                    if (name == null || name.equals(Dom.unwrap(child).getKey())) {
                        checks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(child), "read"));
                    }
                }
            } catch (Exception ex) { 
                String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericListCommand.accesschecks",
                    "Exception while creating access checks for generic command {0}: {1}",
                    commandName, ex.getMessage());
                LogHelper.log(logger, Level.SEVERE, ConfigApiLoggerInfo.ACCESS_CHK_CREATE_FAILED, ex, commandName);
                throw new RuntimeException(msg, ex);
            } 
        }
        return checks;
    }
    
    
    @Override
    void prepareInjection(final AdminCommandContext ctx) {
        super.prepareInjection(ctx);

        parentBean = resolver.resolve(ctx, parentType);
        
    }
    
    @Override
    public void execute(final AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        if (parentBean==null) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCreateCommand.target_object_not_found",
                    "The CrudResolver {0} could not find the configuration object of type {1} where instances of {2} should be added",
                    resolver.getClass().toString(), parentType, targetType);
            report.failure(logger, msg);
            return;
        }
        // Force longOpt if output option is specified
        if (outputOpts != null) {
            longOpt = true;
        }
        List<ColumnInfo> cols = null;
        ColumnFormatter colfm = null;
        if (longOpt) {
            cols = getColumnInfo(targetType);
            if (!isOutputOptsValid(cols, outputOpts)) {
                String collist = arrayToString(getColumnHeadings(cols));
                String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericListCommand.invalidOutputOpts",
                    "Invalid output option. Choose from the following columns: {0}",
                    collist);
                report.failure(logger, msg);
                return;
            }
            cols = filterColumns(cols, outputOpts);
            // sort the columns based on column ordering
            Collections.sort(cols, new Comparator<ColumnInfo>() {
                @Override
                public int compare(ColumnInfo o1, ColumnInfo o2) {
                    return Integer.compare(o1.order,o2.order);
                }
            });
            colfm = headerOpt ? new ColumnFormatter(getColumnHeadings(cols)) : new ColumnFormatter();
        }
       
        List<Map> list = new ArrayList<Map>();
        Properties props = report.getExtraProperties();
        if (props == null) {
            props = new Properties();
            report.setExtraProperties(props);
        }
        
        try {       
            List<ConfigBeanProxy> children = (List<ConfigBeanProxy>) targetMethod.invoke(parentBean);
            for (ConfigBeanProxy child : children) {
                if (name != null && !name.equals(Dom.unwrap(child).getKey())) {
                    continue;
                }
                Map<String,String> map = new HashMap<String,String>();
                if (longOpt) {
                    String data[] = getColumnData(child, cols);
                    colfm.addRow(data);
                    for (int i = 0; i < data.length; i++) {
                        map.put(cols.get(i).xmlName, data[i]);
                    }
                } else {
                    Dom childDom = Dom.unwrap(child);
                    String key = childDom.getKey();
                    if (key==null) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericListCommand.element_has_no_key",
                                "The element {0} has no key attribute",
                                targetType);
                        report.failure(logger, msg);
                        return;
                    }
                    report.addSubActionsReport().setMessage(key);
                    map.put("key", key);
                }
                list.add(map);
            }
            if (longOpt) {
                report.appendMessage(colfm.toString());
            }
            if (!list.isEmpty()) {
                props.put(elementName(Dom.unwrap(parentBean).document, parentType, targetType), list);
            }
        } catch (Exception e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.method_invocation_exception",
                    "Exception while invoking {0} method : {1}",
                    targetMethod.toString(), e.toString());
            report.failure(logger, msg, e);
        }
    }

    @Override
     public CommandModel getModel() {
        return cmdModel;
    }
    
    /*
     * Return the list of columns available from the ConfigBeanProxy clazz
     */
    private List<ColumnInfo> getColumnInfo(Class<? extends ConfigBeanProxy> clazz) {
        List<ColumnInfo> cols = new ArrayList<ColumnInfo>();
        
        for (String aname : targetModel.getAttributeNames()) {
            ColumnInfo ci = new ColumnInfo();
            ci.cprop = targetModel.findIgnoreCase(aname);
            ci.order = targetModel.key.equals("@" + aname) ? ColumnInfo.KEY_ORDER : ColumnInfo.NONKEY_ORDER;
            ci.xmlName = aname;
            ci.heading = aname.toUpperCase(Locale.ENGLISH);
            cols.add(ci);
        }
        
        for (Method m : targetType.getMethods()) {
            ListingColumn lc = m.getAnnotation(ListingColumn.class);
            if (lc != null) {
                String cname = targetModel.camelCaseToXML(targetModel.trimPrefix(m.getName()));
                ColumnInfo mci = null;
                for (ColumnInfo ci : cols) {
                    if (cname.equalsIgnoreCase(ci.xmlName)) {
                        mci = ci;
                        break;
                    }
                }
                if (mci == null) {
                    mci = new ColumnInfo();
                    mci.xmlName= cname;
                    mci.heading = cname.toUpperCase(Locale.ENGLISH);
                    try {
                        mci.duckGetter = targetModel.getDuckMethod(m);
                    } catch (ClassNotFoundException ex) { // @todo Java SE 7 multicatch
                        ConfigApiLoggerInfo.getLogger().log(Level.SEVERE, 
                                ConfigApiLoggerInfo.CANNOT_IDENTIFY_LIST_COL_GETTER, ex);
                    } catch (NoSuchMethodException ex) {
                        ConfigApiLoggerInfo.getLogger().log(Level.SEVERE, 
                                ConfigApiLoggerInfo.CANNOT_IDENTIFY_LIST_COL_GETTER, ex);
                    }
                    cols.add(mci);
                }
                mci.lcAnn = lc;
                mci.order = lc.order();
                if (lc.header() != null && lc.header().length() > 0) {
                    mci.heading = lc.header();
                }
            }
            
        }
        return cols;
    }

    /*
     * Modify the cols list to include only those columns specified in the outputOpts
     */
    private List<ColumnInfo> filterColumns(List<ColumnInfo> cols, String[] outputOpts) {
        List<ColumnInfo> newcols = new ArrayList<ColumnInfo>(cols.size());
        for (ColumnInfo ci : cols) {
            if (ci.isIncluded(outputOpts)) {
                newcols.add(ci);
            }
        }
        return newcols;
    }

    private String[] getColumnHeadings(List<ColumnInfo> cols) {
        String rv[] = new String[cols.size()];
        for (int i = 0; i < rv.length; i++) {
            rv[i] = cols.get(i).heading;
        }
        return rv;
    }

    private String[] getColumnData(ConfigBeanProxy child, List<ColumnInfo> cols) {
        String rv[] = new String[cols.size()];
        for (int i = 0; i < rv.length; i++) {
            rv[i] = cols.get(i).getValue(child);
        }
        return rv;
    }

    /* 
     * Validate that all specified output options are valid column names
     */
    private boolean isOutputOptsValid(List<ColumnInfo> cols, String[] outputOpts) {
        if (outputOpts == null) {
            return true;
        }
        for (int i = 0; i < outputOpts.length; i++) {
            boolean found = false;
            for (ColumnInfo ci : cols) { 
                if (!ci.isExcluded() && ci.heading.equalsIgnoreCase(outputOpts[i])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /*
     * Convert an array of String to a String containing a comma separated list
     */
    private String arrayToString(String[] a) {
        if (a == null || a.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sb.append(", ").append(a[i]);
        }
        return sb.toString();
    }
    
    static class ColumnInfo {
        static final int KEY_ORDER = 0;
        static final int NONKEY_ORDER = 1;
        String heading;
        String xmlName;
        int order;
        ConfigModel.Property cprop;
        Method duckGetter;
        ListingColumn lcAnn;

        private String getValue(ConfigBeanProxy bean) {
            if (cprop != null) {
                return (String)cprop.get(Dom.unwrap(bean), String.class);
            }
            if (duckGetter != null) {
                try {
                    return (String)duckGetter.invoke(null, bean);
                } catch (IllegalAccessException ex) {
                    LogHelper.log(ConfigApiLoggerInfo.getLogger(), Level.SEVERE, 
                            ConfigApiLoggerInfo.ERR_INVOKE_GETTER, ex, duckGetter.getName());
                } catch (IllegalArgumentException ex) {
                    LogHelper.log(ConfigApiLoggerInfo.getLogger(), Level.SEVERE, 
                            ConfigApiLoggerInfo.ERR_INVOKE_GETTER, ex, duckGetter.getName());
                } catch (InvocationTargetException ex) {
                    LogHelper.log(ConfigApiLoggerInfo.getLogger(), Level.SEVERE, 
                            ConfigApiLoggerInfo.ERR_INVOKE_GETTER, ex, duckGetter.getName());
                }
            }
            return "";
        }

        private boolean isExcluded() {
            return lcAnn != null && lcAnn.exclude();
        }
        
        private boolean isIncluded(String[] outputOpts) {
            if (isExcluded()) {
                return false;
            }
            if (outputOpts == null || outputOpts.length == 0) {
                return lcAnn == null || lcAnn.inLongByDefault();
            }
            for (String s : outputOpts) {
                if (s.equalsIgnoreCase(heading)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    @Override
    public Class getDecoratorClass() {
        return null; //No decorator support
    }
    
}
