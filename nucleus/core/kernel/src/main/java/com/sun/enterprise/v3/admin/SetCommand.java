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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.config.LegacyConfigurationUpgrade;
import org.glassfish.internal.api.Target;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.WriteableView;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.tiger_types.Types;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;

/**
 * User: Jerome Dochez
 * Date: Jul 11, 2008
 * Time: 4:39:05 AM
 */
@Service(name = "set")
@ExecuteOn(RuntimeType.INSTANCE)
@PerLookup
@I18n("set")
public class SetCommand extends V2DottedNameSupport implements AdminCommand, PostConstruct,
        AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {

    @Inject
    ServiceLocator habitat;

    @Inject
    Domain domain;

    @Inject
    ConfigSupport config;

    @Inject
    Target targetService;

    @Param(primary = true, multiple = true)
    String[] values;
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(SetCommand.class);

    private HashMap<String, Integer> targetLevel = null;

    private final List<SetOperation> setExecutions = new ArrayList<SetOperation>();
    
    @Override
    public void postConstruct() {
        targetLevel = new HashMap<String, Integer>();
        targetLevel.put("applications", 0);
        targetLevel.put("system-applications", 0);
        targetLevel.put("resources", 0);
        targetLevel.put("configs", 3);
        targetLevel.put("clusters", 3);
        targetLevel.put("servers", 3);
        targetLevel.put("nodes", 3);
    }

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        /*
         * For each value from the user create a SetExecution and prepare it.
         * "Preparing it" means processing the value so we know what config
         * changes we will make if we actually execute that part of the "set"
         * operation.  That will allow getAccessChecks to return the correct
         * answer after all the SetExecutions for all the user-provided
         * values have been prepared.
         */
        for (String value : values) {
            if (value.contains(".log-service")) {
                fail(context, localStrings.getLocalString("admin.set.invalid.logservice.command", "For setting log levels/attributes use set-log-levels/set-log-attributes command."));
                return false;
            }
            
            final SetOperation setExecution = new SetOperation();
            if ( ! setExecution.prepare(context, value)) {
                // fast failure
                return false;
            }
            setExecutions.add(setExecution);
        }
        return true;
    }

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final Collection<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        for (SetOperation e : setExecutions) {
            accessChecks.addAll(e.getAccessChecks());
        }
        
        return accessChecks;
    }

    
    @Override
    public void execute(AdminCommandContext context) {
        /*
         * Thanks to the preAuthorization method, we already have one or more
         * SetExecutions prepared.  Now we just run them.
         */
        for (SetOperation e : setExecutions) {
            if ( ! e.run(context)) {
                // fast failure
                return;
            }
        }
    }

    public static void setLeafElement (
                final ConfigBean node,
                final String elementName,
                final String values)
        throws TransactionFailure {

        ConfigBeanProxy readableView = node.getProxy(node.getProxyType());
        ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {

            /**
             * Runs the following command passing the configuration object. The code will be run
             * within a transaction, returning true will commit the transaction, false will abort
             * it.
             *
             * @param param is the configuration object protected by the transaction
             * @return any object that should be returned from within the transaction code
             * @throws java.beans.PropertyVetoException
             *          if the changes cannot be applied
             *          to the configuration
             */
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {

                WriteableView writeableParent = (WriteableView)Proxy.getInvocationHandler(param);

                StringTokenizer st = new StringTokenizer(values, ",");
                List<String> valList = new ArrayList<String>();
                while (st.hasMoreTokens()) valList.add(st.nextToken());

                ConfigBean bean = writeableParent.getMasterView();
                for (Method m : writeableParent.getProxyType().getMethods()) {
                    // Check to see if the method is a setter for the element
                    // An element setter has to have the right name, take a single
                    // collection parameter that parameterized with the right type
                    Class argClasses[] = m.getParameterTypes();
                    Type argTypes[] = m.getGenericParameterTypes();
                    if (!bean.model.toProperty(m).xmlName().equals(elementName) ||
                            argClasses.length != 1 ||
                            !Collection.class.isAssignableFrom(argClasses[0]) ||
                            argTypes.length != 1 ||
                            !(argTypes[0] instanceof ParameterizedType) ||
                            !Types.erasure(Types.getTypeArgument(argTypes[0], 0)).isAssignableFrom(values.getClass())) {
                        continue;
                    }
                    // we have the right method.  Now call it
                    try {
                        m.invoke(writeableParent.getProxy(writeableParent.<ConfigBeanProxy>getProxyType()), valList);
                    } catch (IllegalAccessException e) {
                        throw new TransactionFailure("Exception while setting element", e);
                    } catch (InvocationTargetException e) {
                        throw new TransactionFailure("Exception while setting element", e);
                    }
                    return node;
                }
                throw new TransactionFailure("No method found for setting element");
            }
        }, readableView);
    }

    /*
     * Determine whether this attribute is deprecated.  This method
     * stops looking after it finds the first method or field whose name matches
     * the attribute name. So to make an attribute deprecated, all of the
     * methods (set, get, etc.) must be marked as deprecated.
     */
    private boolean isDeprecatedAttr(Dom dom, String name) {
        if (dom == null || dom.model == null || name == null) return false;
        Class t = dom.getProxyType();
        if (t == null) return false;
        for (Method m : t.getDeclaredMethods()) {
            ConfigModel.Property p = dom.model.toProperty(m);
            if (p != null && name.equals(p.xmlName())) {
                return m.isAnnotationPresent(Deprecated.class);
            }
        }
        for (Field f : t.getDeclaredFields()) {
            if (name.equals(dom.model.camelCaseToXML(f.getName()))) {
                return f.isAnnotationPresent(Deprecated.class);
            }
        }
        return false;
    }

    private String getElementFromString(String name, int index) {
        StringTokenizer token = new StringTokenizer(name, ".");
        String target = null;
        for (int j = 0; j < index; j++) {
            if (token.hasMoreTokens())
                target = token.nextToken();
        }
        return target;
    }

    private boolean replicateSetCommand(AdminCommandContext context, String targetName, String value) {
        // "domain." on the front of the attribute name is optional.  So if it is
        // there, strip it off. 
        List<Server> replicationInstances = null;
        String tName;
        if (targetName.startsWith("domain.")) {
            tName = targetName.substring("domain.".length());
            if (tName.indexOf('.') == -1) {
                // This is a domain-level attribute, replicate to all instances
                replicationInstances = targetService.getAllInstances();
            }
        }
        else {
            tName = targetName;
        }
        if (replicationInstances == null) {
            int dotIdx = tName.indexOf('.');
            String firstElementOfName = dotIdx != -1 ? tName.substring(0, dotIdx) : tName;
            Integer targetElementLocation = targetLevel.get(firstElementOfName);
            if (targetElementLocation == null)
                targetElementLocation = 1;
            if (targetElementLocation == 0) {
                if ("resources".equals(firstElementOfName)) {
                    replicationInstances = targetService.getAllInstances();
                }
                if ("applications".equals(firstElementOfName)) {
                    String appName = getElementFromString(tName, 3);
                    if (appName == null) {
                        fail(context, localStrings.getLocalString("admin.set.invalid.appname",
                                "Unable to extract application name from {0}", targetName));
                        return false;
                    }
                    replicationInstances = targetService.getInstances(domain.getAllReferencedTargetsForApplication(appName));
                }
            } else {
                String target = getElementFromString(tName, targetElementLocation);
                if (target == null) {
                    fail(context, localStrings.getLocalString("admin.set.invalid.target",
                            "Unable to extract replication target from {0}", targetName));
                    return false;
                }
                replicationInstances = targetService.getInstances(target);
            }
        }

        if (replicationInstances != null && !replicationInstances.isEmpty()) {
            ParameterMap params = new ParameterMap();
            params.set("DEFAULT", targetName + "=" + value);
            ActionReport.ExitCode ret = ClusterOperationUtil.replicateCommand("set", FailurePolicy.Error,
                    FailurePolicy.Warn, FailurePolicy.Ignore, replicationInstances, context, params, habitat);
            if (ret.equals(ActionReport.ExitCode.FAILURE))
                return false;
        }
        return true;
    }

    private void runLegacyChecks(AdminCommandContext context) {
        final Collection<LegacyConfigurationUpgrade> list = habitat.<LegacyConfigurationUpgrade>getAllServices(LegacyConfigurationUpgrade.class);
        for (LegacyConfigurationUpgrade upgrade : list) {
            upgrade.execute(context);
        }
    }

    /**
     * Find the rightmost unescaped occurrence of specified character in target
     * string.
     * <p/>
     * XXX Doesn't correctly interpret escaped backslash characters, e.g. foo\\.bar
     *
     * @param target string to search
     * @param ch     a character
     * @return index index of last unescaped occurrence of specified character
     *         or -1 if there are no unescaped occurrences of this character.
     */
    private static int trueLastIndexOf(String target, char ch) {
        int i = target.lastIndexOf(ch);
        while (i > 0) {
            if (target.charAt(i - 1) == '\\') {
                i = target.lastIndexOf(ch, i - 1);
            } else {
                break;
            }
        }
        return i;
    }

    /**
     * Indicate in the action report that the command failed.
     */
    private static void fail(AdminCommandContext context, String msg) {
        fail(context, msg, null);
    }

    /**
     * Indicate in the action report that the command failed.
     */
    private static void fail(AdminCommandContext context, String msg,
                             Exception ex) {
        context.getActionReport().setActionExitCode(
                ActionReport.ExitCode.FAILURE);
        if (ex != null)
            context.getActionReport().setFailureCause(ex);
        context.getActionReport().setMessage(msg);
    }

    /**
     * Indicate in the action report a warning message.
     */
    private void warning(AdminCommandContext context, String msg) {
        ActionReport ar = context.getActionReport().addSubActionsReport();
        ar.setActionExitCode(ActionReport.ExitCode.WARNING);
        ar.setMessage(msg);
    }

    /**
     * Indicate in the action report that the command succeeded and
     * include the target property and it's value in the report
     */
    private void success(AdminCommandContext context, String target, String value) {
        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport.MessagePart part = context.getActionReport().getTopMessagePart().addChild();
        part.setChildrenType("DottedName");
        part.setMessage(target + "=" + value);
    }
    
    /**
     * Represents the preparation and execution of a "set" operation on a 
     * single name=value pair. 
     * <p>
     * Each SetOperation has exactly one SetAction, which itself can be either
     * a single action on a property element or possibly multiple config
     * actions related to a single config bean (adding or deleting children, etc.).  
     */
    private class SetOperation {
        
        private SetAction setAction = null; // utlimately this is the one to use
        
        private Collection<? extends AccessCheck> getAccessChecks() {
            return setAction.getAccessChecks();
        }
        
        private boolean run(final AdminCommandContext context) {
            return setAction.set(context);
        }
        
        private boolean prepare(AdminCommandContext context, String nameval) {

            int i = nameval.indexOf('=');
            if (i < 0) {
                //ail(context, "Invalid attribute " + nameval);
                fail(context, localStrings.getLocalString("admin.set.invalid.namevalue", "Invalid name value pair {0}. Missing expected equal sign.", nameval));
                return false;
            }
            String target = nameval.substring(0, i);
            String value = nameval.substring(i + 1);
            // so far I assume we always want to change one attribute so I am removing the
            // last element from the target pattern which is supposed to be the
            // attribute name
            int lastDotIndex = trueLastIndexOf(target, '.');
            if (lastDotIndex == -1) {
                // error.
                //fail(context, "Invalid attribute name " + target);
                fail(context, localStrings.getLocalString("admin.set.invalid.attributename", "Invalid attribute name {0}", target));
                return false;
            }
            String attrName = target.substring(lastDotIndex + 1).replace("\\.", ".");
            String pattern = target.substring(0, lastDotIndex);
            if (attrName.replace('_', '-').equals("jndi-name")) {
                //fail(context, "Cannot change a primary key\nChange of " + target + " is rejected.");
                fail(context, localStrings.getLocalString("admin.set.reject.keychange", "Cannot change a primary key\nChange of {0}", target));
                return false;
            }
            boolean isProperty = false;
            if ("property".equals(pattern.substring(trueLastIndexOf(pattern, '.') + 1))) {
                // we are looking for a property, let's look it it exists already...
                pattern = target.replaceAll("\\\\\\.", "\\.");
                isProperty = true;
            }

            // now
            // first let's get the parent for this pattern.
            TreeNode[] parentNodes = getAliasedParent(domain, pattern);

            // reset the pattern.
            String prefix;
            boolean lookAtSubNodes = true;
            if (parentNodes[0].relativeName.length() == 0 ||
                    parentNodes[0].relativeName.equals("domain")) {
                // handle the case where the pattern references an attribute of the top-level node
                prefix = "";
                // pattern is already set properly
                lookAtSubNodes = false;
            }
            else if(!pattern.startsWith(parentNodes[0].relativeName)) {
                prefix = pattern.substring(0, pattern.indexOf(parentNodes[0].relativeName));
                pattern = parentNodes[0].relativeName;
            }
            else {
                prefix = "";
                pattern = parentNodes[0].relativeName;
            }
            String propTargetName = prefix + pattern;

            Map<Dom, String> matchingNodes;
            boolean applyOverrideRules = false;
            Map<Dom, String> dottedNames = new HashMap<Dom, String>();
            if (lookAtSubNodes) {
                for (TreeNode parentNode : parentNodes) {
                    dottedNames.putAll(getAllDottedNodes(parentNode.node));
                }
                matchingNodes = getMatchingNodes(dottedNames, pattern);
                applyOverrideRules = true;
            } else {
                matchingNodes = new HashMap<Dom, String>();
                for (TreeNode parentNode : parentNodes) {
                    matchingNodes.put(parentNode.node, pattern);
                }
            }

            if (matchingNodes.isEmpty()) {
                // it's possible they are trying to create a property object.. lets check this.
                // strip out the property name
                pattern = target.substring(0, trueLastIndexOf(target, '.'));
                if (pattern.endsWith("property")) {
                    pattern = pattern.substring(0, trueLastIndexOf(pattern, '.'));
                    parentNodes = getAliasedParent(domain, pattern);
                    pattern = parentNodes[0].relativeName;
                    matchingNodes = getMatchingNodes(dottedNames, pattern);
                    if (matchingNodes.isEmpty()) {
                        //fail(context, "No configuration found for " + targetName);
                        fail(context, localStrings.getLocalString("admin.set.configuration.notfound", "No configuration found for {0}", propTargetName));
                        return false;
                    }
                    // need to find the right parent.
                    Dom parentNode = null;
                    for (Map.Entry<Dom, String> node : matchingNodes.entrySet()) {
                        if (node.getValue().equals(pattern)) {
                            parentNode = node.getKey();
                        }
                    }
                    if (parentNode == null) {
                        //fail(context, "No configuration found for " + targetName);
                        fail(context, localStrings.getLocalString("admin.set.configuration.notfound", "No configuration found for {0}", propTargetName));
                        return false;
                    }

                    if (value == null || value.length() == 0) {
                        // setting to the empty string means to remove the property, so don't create it
                        recordAutoSuccessAction(propTargetName, value);
                        success(context, propTargetName, value);
                        return true;
                    }
                    // create and set the property
                    if ( ! (parentNode instanceof ConfigBean)) {
                        throw new ClassCastException(
                                localStrings.getLocalString("admin.set.DomNotConfigBean",
                                    "Expected object of type Dom ({0}) to be a ConfigBean but it is not",
                                    parentNode.getClass().getName()));
                    }
                    recordConfigBeanCreateAndSet((ConfigBean) parentNode, attrName,
                            value, propTargetName);
                    return true;
                }
            }
            
            /*
             * We are processing a "set" applied to something other than a
             * property.  
             */
            final NonPropertyAction nonPropAction = new NonPropertyAction(value);
            setAction = nonPropAction;
            
            
            boolean delProperty = false;
            Map<String, String> attrChanges = new HashMap<String, String>();
            if (isProperty) {
                attrName = "value";
                if ((value == null) || (value.length() == 0)) {
                    delProperty = true;
                }
                attrChanges.put(attrName, value);
            }

            List<Map.Entry> mNodes = new ArrayList(matchingNodes.entrySet());
            if (applyOverrideRules) {
                mNodes = applyOverrideRules(mNodes);
            }
            for (Map.Entry<Dom, String> node : mNodes) {
                final Dom targetNode = node.getKey();

                for (String name : targetNode.model.getAttributeNames()) {
                    String finalDottedName = node.getValue() + "." + name;
                    if (matches(finalDottedName, pattern)) {
                        if (attrName.equals(name) ||
                                attrName.replace('_', '-').equals(name.replace('_', '-')))  {
                            if (isDeprecatedAttr(targetNode, name)) {
                               warning(context, localStrings.getLocalString("admin.set.deprecated",
                                       "Warning: The attribute {0} is deprecated.", finalDottedName));
                            }

                            if (!isProperty) {
                                nonPropAction.targetName = prefix + finalDottedName;

                                if (value != null && value.length() > 0) {
                                    attrChanges.put(name, value);
                                } else {
                                    attrChanges.put(name, null);
                                }
                            } else {
                                nonPropAction.targetName = prefix + node.getValue();
                            }

                            if (delProperty) {
                                // delete property element
                                String str = node.getValue();
                                if (trueLastIndexOf(str, '.') != -1) {
                                    str = str.substring(trueLastIndexOf(str, '.') + 1);
                                }
                                if (str != null) {
                                    recordNodeDeletion(nonPropAction.beanActions, (ConfigBean) targetNode);
                                }
                            } else {
                                nonPropAction.changes.put((ConfigBean) node.getKey(), attrChanges);
                            }

                        }
                    }
                }

                for (String name : targetNode.model.getLeafElementNames()) {
                    String finalDottedName = node.getValue() + "." + name;
                    if (matches(finalDottedName, pattern)) {
                        if (attrName.equals(name) ||
                                attrName.replace('_', '-').equals(name.replace('_', '-')))  {
                            if (isDeprecatedAttr(targetNode, name)) {
                               warning(context, localStrings.getLocalString("admin.set.elementdeprecated",
                                       "Warning: The element {0} is deprecated.", finalDottedName));
                            }
                            recordLeafElementSet(nonPropAction.beanActions, (ConfigBean) targetNode, name, value);
                            break;
                        }
                    }
                }
            }
            return true;
        }
        
        private void recordAutoSuccessAction(final String targetName, 
                final String value) {
            setAction = new AutoSuccessAction(targetName, value);
        }
        
        private void recordConfigBeanCreateAndSet(
                final ConfigBean parentNode, 
                final String attrName,
                final String value,
                final String targetName) {
            final Map<String,String> attributes = new HashMap<String,String>();
            attributes.put("value", value);
            attributes.put("name", attrName);
            setAction = new PropertyCreationAction(
                    parentNode, attributes, targetName, value);
        }

        private void recordLeafElementSet(
                final Collection<BeanAction> beanActions,
                final ConfigBean targetNode,
                final String name,
                final String value) {
            beanActions.add(new LeafElementSetAction(targetNode, name, value));
        }

        private void recordNodeDeletion(
                final Collection<BeanAction> beanActions,
                final ConfigBean targetNode) {
            beanActions.add(new NodeDeleteAction(targetNode));
        }
    }
    /**
     * Represents the "set" logic as applied to one of the name=value pairs
     * supplied by the user.
     * <p>
     * We have to be able to provide the list of AccessChecks for
     * authorization before we perform the actual changes to the configuration.
     * So where the code used to actually make changes to the config it now
     * creates an instance of the right kind of set action.  Depending on
     * what dotted name the user provides, the corresponding set action can either apply to
     * a property or something else, and these are handled quite differently.
     * <p>
     * The original code was fairly complicated, so I have tried to disrupt
     * it as little as possible while describing the general approach in comments
     * as I moved the update logic into these action-related classes.  My
     * goal was to retain essentially the same code paths for the various cases,
     * except that originally the intricate logic led to the actual config updates;
     * now essentially that same logic leads to the creation of "action" objects
     * representing the config updates to apply.  Once the actions that the
     * command needs to perform are known so are the access checks to be 
     * submitted as part of authorization.  If the authorization succeeds then
     * the command just runs the correct, previously-created set action to
     * perform the config update(s).
     */
    private abstract class SetAction {
        
        abstract Collection<? extends AccessCheck> getAccessChecks();
        
        abstract boolean set(AdminCommandContext context);
    }
    
    /**
     * Set action for a property.
     * <p>
     * A property action can be either a no-op (which is reported as successful)
     * or an actual creation of a property element.  
     */
    private abstract class PropertyAction  extends SetAction {
        protected String targetName;
        protected String value;
        
        private PropertyAction(final String targetName, final String value) {
            this.targetName = targetName;
            this.value = value;
        }
        
        @Override
        Collection<? extends AccessCheck> getAccessChecks() {
            return Arrays.asList(new AccessCheck(targetName.replace('.', '/'), "update"));
        }
    }
    
    /**
     * No-op action.
     * <p>
     * This is here primarily to report success for processing the assignment.
     */
    private class AutoSuccessAction extends PropertyAction {

        private AutoSuccessAction(final String targetName, final String value) {
            super(targetName, value);
        }
        
        @Override
        boolean set(AdminCommandContext context) {
            success(context, targetName, value);
            return true;
        }
    }
    
    /**
     * Action for creating a new property.
     * <p>
     * The user's name=value pair can specify a "xxx.property" name which is
     * actually implemented as a <property> element under its parent.  This 
     * class provides the config change logic for creating that new element
     * and, if appropriate, propagating it to other instances.
     */
    private class PropertyCreationAction extends PropertyAction {
        private ConfigBean parentNode;
        private Map<String,String> attributes;
        
        private PropertyCreationAction(final ConfigBean parentNode, 
            final Map<String,String> attributes,
            final String targetName,
            final String value) {
            super(targetName, value);
            this.parentNode = parentNode;
            this.attributes = attributes;
        }

        @Override
        boolean set(final AdminCommandContext context) {

            try {
                ConfigSupport.createAndSet(parentNode, Property.class, attributes);
                success(context, targetName, value);
                runLegacyChecks(context);
                if (targetService.isThisDAS() && !replicateSetCommand(context, targetName, value)) {
                    return false;
                }
                return true;
            } catch (TransactionFailure transactionFailure) {
                //fail(context, "Could not change the attributes: " +
                //    transactionFailure.getMessage(), transactionFailure);
                fail(context, localStrings.getLocalString("admin.set.attribute.change.failure", "Could not change the attributes: {0}",
                        transactionFailure.getMessage()), transactionFailure);
                return false;
            }
        }
    }
    
    /**
     * Organizes logic to handle a name=value pair that specifies something
     * other than a property to be set: an attribute or an element.
     * <p>
     * Each non-property action can trigger multiple changes to the config, for
     * example 
     */
    private class NonPropertyAction extends SetAction{
        String targetName;
        private final String value;
        private final Map<ConfigBean, Map<String, String>> changes = 
                new HashMap<ConfigBean, Map<String,String>>();
        private final Collection<BeanAction> beanActions = new ArrayList<BeanAction>();
        
        private NonPropertyAction(final String value) {
            this.value = value;
        }

        @Override
        Collection<? extends AccessCheck> getAccessChecks() {
            final Collection<AccessCheck> result = new ArrayList<AccessCheck>();
            for (BeanAction beanAction : beanActions) {
                result.addAll(beanAction.getAccessChecks());
            }
            for (Map.Entry<ConfigBean,Map<String,String>> entry : changes.entrySet()) {
                result.add(new AccessCheck(AccessRequired.Util.resourceNameFromDom(entry.getKey()), "update"));
            }

            return result;
        }
        
        
        @Override
        boolean set(final AdminCommandContext context) {
            final AtomicBoolean delPropertySuccess = new AtomicBoolean(false);
            final AtomicBoolean setElementSuccess = new AtomicBoolean(false);
            for (BeanAction action : beanActions) {
                
                if ( ! action.set(context, delPropertySuccess, setElementSuccess)) {
                    // fast failure
                    return false;
                }
            }
            if (!changes.isEmpty()) {
                try {
                    config.apply(changes);
                    success(context, targetName, value);
                    runLegacyChecks(context);
                } catch (TransactionFailure transactionFailure) {
                    //fail(context, "Could not change the attributes: " +
                    //        transactionFailure.getMessage(), transactionFailure);
                    fail(context, localStrings.getLocalString("admin.set.attribute.change.failure", "Could not change the attributes: {0}",
                            transactionFailure.getMessage()), transactionFailure);
                    return false;
                }
            } else if (delPropertySuccess.get() || setElementSuccess.get()) {
                success(context, targetName, value);
                return true;
            } else {
                fail(context, localStrings.getLocalString("admin.set.configuration.notfound", "No configuration found for {0}", targetName));
                return false;
            }
            if (targetService.isThisDAS() && !replicateSetCommand(context, targetName, value)) {
                // fast failure
                return false;
            }
            return true;
        }
    }

    /**
     * A single bean-related update action to the config.
     * <p>
     * Either NodeDeleteAction or a LeafElementSetAction.
     */
    private abstract class BeanAction {
        abstract Collection<? extends AccessCheck> getAccessChecks();
        abstract boolean set(AdminCommandContext context, 
                AtomicBoolean delPropertySuccess, AtomicBoolean setElementSuccess);
    }

    private class NodeDeleteAction extends BeanAction {
        private final Dom targetNode;

        private NodeDeleteAction(final Dom targetNode) {
            this.targetNode = targetNode;
        }

        @Override
        Collection<? extends AccessCheck> getAccessChecks() {
            return Arrays.asList(new AccessCheck(targetNode.getKey().replace('.','/'), "update"));
        }

        @Override
        boolean set(AdminCommandContext context, final AtomicBoolean delPropertySuccess,
                final AtomicBoolean setElementSuccess) {
            try {
                ConfigSupport.deleteChild((ConfigBean) targetNode.parent(), (ConfigBean) targetNode);
                delPropertySuccess.set(true);
                return true;
            } catch (IllegalArgumentException ie) {
                fail(context, localStrings.getLocalString("admin.set.delete.property.failure", "Could not delete the property: {0}",
                        ie.getMessage()), ie);
                return false;
            } catch (TransactionFailure transactionFailure) {
                fail(context, localStrings.getLocalString("admin.set.attribute.change.failure", "Could not change the attributes: {0}",
                        transactionFailure.getMessage()), transactionFailure);
                return false;
            }
        }
    }

    private class LeafElementSetAction extends BeanAction {

        final ConfigBean targetNode;
        final String name;
        final String value;

        private LeafElementSetAction(final ConfigBean targetNode,
                final String name,
                final String value) {
            this.targetNode = targetNode;
            this.name = name;
            this.value = value;
        }

        @Override
        Collection<? extends AccessCheck> getAccessChecks() {
            return Arrays.asList(new AccessCheck(name.replace('.','/'),"update"));
        }

        @Override
        boolean set(AdminCommandContext context, final AtomicBoolean delPropertySuccess,
                final AtomicBoolean setElementSuccess) {
            try {
                setLeafElement((ConfigBean)targetNode, name, value);
                setElementSuccess.set(true);
                return true;
            } catch (TransactionFailure ex) {
                fail(context, localStrings.getLocalString("admin.set.badelement", "Cannot change the element: {0}",
                        ex.getMessage()), ex);
                return false;
            }
        }
    }
        
}
