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

package org.glassfish.loadbalancer.admin.cli.beans;

import org.netbeans.modules.schema2beans.*;
import java.util.*;

// BEGIN_NOI18N
public class Cluster extends org.netbeans.modules.schema2beans.BaseBean {

    static Vector comparators = new Vector();
    static public final String NAME = "Name";	// NOI18N
    static public final String POLICY = "Policy";	// NOI18N
    static public final String POLICYMODULE = "PolicyModule";	// NOI18N
    static public final String INSTANCE = "Instance";	// NOI18N
    static public final String INSTANCENAME = "InstanceName";	// NOI18N
    static public final String INSTANCEENABLED = "InstanceEnabled";	// NOI18N
    static public final String INSTANCEDISABLETIMEOUTINMINUTES = "InstanceDisableTimeoutInMinutes";	// NOI18N
    static public final String INSTANCELISTENERS = "InstanceListeners";	// NOI18N
    static public final String INSTANCEWEIGHT = "InstanceWeight";	// NOI18N
    static public final String WEB_MODULE = "WebModule";	// NOI18N
    static public final String HEALTH_CHECKER = "HealthChecker";	// NOI18N
    static public final String HEALTHCHECKERURL = "HealthCheckerUrl";	// NOI18N
    static public final String HEALTHCHECKERINTERVALINSECONDS = "HealthCheckerIntervalInSeconds";	// NOI18N
    static public final String HEALTHCHECKERTIMEOUTINSECONDS = "HealthCheckerTimeoutInSeconds";	// NOI18N

    public Cluster() {
        this(Common.USE_DEFAULT_VALUES);
    }

    public Cluster(int options) {
        super(comparators, new org.netbeans.modules.schema2beans.Version(1, 2, 0));
        // Properties (see root bean comments for the bean graph)
        this.createProperty("instance", // NOI18N
                INSTANCE,
                Common.TYPE_0_N | Common.TYPE_BOOLEAN | Common.TYPE_KEY,
                Boolean.class);
        this.createAttribute(INSTANCE, "name", "Name",
                AttrProp.CDATA | AttrProp.REQUIRED,
                null, null);
        this.createAttribute(INSTANCE, "enabled", "Enabled",
                AttrProp.CDATA,
                null, "true");
        this.createAttribute(INSTANCE, "disable-timeout-in-minutes", "DisableTimeoutInMinutes",
                AttrProp.CDATA,
                null, "31");
        this.createAttribute(INSTANCE, "listeners", "Listeners",
                AttrProp.CDATA | AttrProp.REQUIRED,
                null, null);
        this.createAttribute(INSTANCE, "weight", "Weight",
                AttrProp.CDATA,
                null, "100");
        this.createProperty("web-module", // NOI18N
                WEB_MODULE,
                Common.TYPE_0_N | Common.TYPE_BEAN | Common.TYPE_KEY,
                WebModule.class);
        this.createAttribute(WEB_MODULE, "context-root", "ContextRoot",
                AttrProp.CDATA | AttrProp.REQUIRED,
                null, null);
        this.createAttribute(WEB_MODULE, "enabled", "Enabled",
                AttrProp.CDATA,
                null, "true");
        this.createAttribute(WEB_MODULE, "disable-timeout-in-minutes", "DisableTimeoutInMinutes",
                AttrProp.CDATA,
                null, "31");
        /* Big hack, this is to not set default value of ""
        for error-url Must be removed later  Refer to bug #6171814.

        this.createAttribute(WEB_MODULE, "error-url", "ErrorUrl",
        AttrProp.CDATA,
        null, "");

         */
        this.createProperty("health-checker", // NOI18N
                HEALTH_CHECKER,
                Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY,
                Boolean.class);
        this.createAttribute(HEALTH_CHECKER, "url", "Url",
                AttrProp.CDATA,
                null, "/");
        this.createAttribute(HEALTH_CHECKER, "interval-in-seconds", "IntervalInSeconds",
                AttrProp.CDATA,
                null, "30");
        this.createAttribute(HEALTH_CHECKER, "timeout-in-seconds", "TimeoutInSeconds",
                AttrProp.CDATA,
                null, "10");
        this.initialize(options);
    }

    // Setting the default values of the properties
    void initialize(int options) {
    }

    // This attribute is mandatory
    public void setName(java.lang.String value) {
        setAttributeValue(NAME, value);
    }

    //
    public java.lang.String getName() {
        return getAttributeValue(NAME);
    }

    public void setPolicy(java.lang.String value) {
        setAttributeValue(POLICY, value);
    }

    //
    public java.lang.String getPolicy() {
        return getAttributeValue(POLICY);
    }

    public void setPolicyModule(java.lang.String value) {
        setAttributeValue(POLICYMODULE, value);
    }

    //
    public java.lang.String getPolicyModule() {
        return getAttributeValue(POLICYMODULE);
    }

    // This attribute is an array, possibly empty
    public void setInstance(int index, boolean value) {
        this.setValue(INSTANCE, index, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
    }

    //
    public boolean isInstance(int index) {
        Boolean ret = (Boolean) this.getValue(INSTANCE, index);
        if (ret == null) {
            ret = (Boolean) Common.defaultScalarValue(Common.TYPE_BOOLEAN);
        }
        return ((java.lang.Boolean) ret).booleanValue();
    }

    // This attribute is an array, possibly empty
    public void setInstance(boolean[] value) {
        Boolean[] values = null;
        if (value != null) {
            values = new Boolean[value.length];
            for (int i = 0; i < value.length; i++) {
                values[i] = Boolean.valueOf(value[i]);
            }
        }
        this.setValue(INSTANCE, values);
    }

    //
    public boolean[] getInstance() {
        boolean[] ret = null;
        Boolean[] values = (Boolean[]) this.getValues(INSTANCE);
        if (values != null) {
            ret = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                ret[i] = values[i].booleanValue();
            }
        }
        return ret;
    }

    // Return the number of properties
    public int sizeInstance() {
        return this.size(INSTANCE);
    }

    // Add a new element returning its index in the list
    public int addInstance(boolean value) {
        return this.addValue(INSTANCE, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
    }

    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeInstance(boolean value) {
        return this.removeValue(INSTANCE, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
    }

    //
    // Remove an element using its index
    //
    public void removeInstance(int index) {
        this.removeValue(INSTANCE, index);
    }

    // This attribute is an array, possibly empty
    public void setInstanceName(int index, java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(INSTANCE) == 0) {
            addValue(INSTANCE, "");
        }
        setAttributeValue(INSTANCE, index, "Name", value);
    }

    //
    public java.lang.String getInstanceName(int index) {
        // If our element does not exist, then the attribute does not exist.
        if (size(INSTANCE) == 0) {
            return null;
        } else {
            return getAttributeValue(INSTANCE, index, "Name");
        }
    }

    // This attribute is an array, possibly empty
    public void setInstanceEnabled(int index, java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(INSTANCE) == 0) {
            addValue(INSTANCE, "");
        }
        setAttributeValue(INSTANCE, index, "Enabled", value);
    }

    //
    public java.lang.String getInstanceEnabled(int index) {
        // If our element does not exist, then the attribute does not exist.
        if (size(INSTANCE) == 0) {
            return null;
        } else {
            return getAttributeValue(INSTANCE, index, "Enabled");
        }
    }

    // This attribute is an array, possibly empty
    public void setInstanceDisableTimeoutInMinutes(int index, java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(INSTANCE) == 0) {
            addValue(INSTANCE, "");
        }
        setAttributeValue(INSTANCE, index, "DisableTimeoutInMinutes", value);
    }

    //
    public java.lang.String getInstanceDisableTimeoutInMinutes(int index) {
        // If our element does not exist, then the attribute does not exist.
        if (size(INSTANCE) == 0) {
            return null;
        } else {
            return getAttributeValue(INSTANCE, index, "DisableTimeoutInMinutes");
        }
    }

    // This attribute is an array, possibly empty
    public void setInstanceListeners(int index, java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(INSTANCE) == 0) {
            addValue(INSTANCE, "");
        }
        setAttributeValue(INSTANCE, index, "Listeners", value);
    }

    //
    public java.lang.String getInstanceListeners(int index) {
        // If our element does not exist, then the attribute does not exist.
        if (size(INSTANCE) == 0) {
            return null;
        } else {
            return getAttributeValue(INSTANCE, index, "Listeners");
        }
    }

    public void setInstanceWeight(int index, java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(INSTANCE) == 0) {
            addValue(INSTANCE, "");
        }
        setAttributeValue(INSTANCE, index, "Weight", value);
    }

    //
    public java.lang.String getInstanceWeight(int index) {
        // If our element does not exist, then the attribute does not exist.
        if (size(INSTANCE) == 0) {
            return null;
        } else {
            return getAttributeValue(INSTANCE, index, "Weight");
        }
    }

    // This attribute is an array, possibly empty
    public void setWebModule(int index, WebModule value) {
        this.setValue(WEB_MODULE, index, value);
    }

    //
    public WebModule getWebModule(int index) {
        return (WebModule) this.getValue(WEB_MODULE, index);
    }

    // This attribute is an array, possibly empty
    public void setWebModule(WebModule[] value) {
        this.setValue(WEB_MODULE, value);
    }

    //
    public WebModule[] getWebModule() {
        return (WebModule[]) this.getValues(WEB_MODULE);
    }

    // Return the number of properties
    public int sizeWebModule() {
        return this.size(WEB_MODULE);
    }

    // Add a new element returning its index in the list
    public int addWebModule(org.glassfish.loadbalancer.admin.cli.beans.WebModule value) {
        return this.addValue(WEB_MODULE, value);
    }

    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeWebModule(org.glassfish.loadbalancer.admin.cli.beans.WebModule value) {
        return this.removeValue(WEB_MODULE, value);
    }

    // This attribute is optional
    public void setHealthChecker(boolean value) {
        this.setValue(HEALTH_CHECKER, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
    }

    //
    public boolean isHealthChecker() {
        Boolean ret = (Boolean) this.getValue(HEALTH_CHECKER);
        if (ret == null) {
            ret = (Boolean) Common.defaultScalarValue(Common.TYPE_BOOLEAN);
        }
        return ((java.lang.Boolean) ret).booleanValue();
    }

    // This attribute is mandatory
    public void setHealthCheckerUrl(java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(HEALTH_CHECKER) == 0) {
            setValue(HEALTH_CHECKER, "");
        }
        setAttributeValue(HEALTH_CHECKER, "Url", value);
    }

    //
    public java.lang.String getHealthCheckerUrl() {
        // If our element does not exist, then the attribute does not exist.
        if (size(HEALTH_CHECKER) == 0) {
            return null;
        } else {
            return getAttributeValue(HEALTH_CHECKER, "Url");
        }
    }

    // This attribute is mandatory
    public void setHealthCheckerIntervalInSeconds(java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(HEALTH_CHECKER) == 0) {
            setValue(HEALTH_CHECKER, "");
        }
        setAttributeValue(HEALTH_CHECKER, "IntervalInSeconds", value);
    }

    //
    public java.lang.String getHealthCheckerIntervalInSeconds() {
        // If our element does not exist, then the attribute does not exist.
        if (size(HEALTH_CHECKER) == 0) {
            return null;
        } else {
            return getAttributeValue(HEALTH_CHECKER, "IntervalInSeconds");
        }
    }

    // This attribute is mandatory
    public void setHealthCheckerTimeoutInSeconds(java.lang.String value) {
        // Make sure we've got a place to put this attribute.
        if (size(HEALTH_CHECKER) == 0) {
            setValue(HEALTH_CHECKER, "");
        }
        setAttributeValue(HEALTH_CHECKER, "TimeoutInSeconds", value);
    }

    //
    public java.lang.String getHealthCheckerTimeoutInSeconds() {
        // If our element does not exist, then the attribute does not exist.
        if (size(HEALTH_CHECKER) == 0) {
            return null;
        } else {
            return getAttributeValue(HEALTH_CHECKER, "TimeoutInSeconds");
        }
    }

    //
    public static void addComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
        comparators.add(c);
    }

    //
    public static void removeComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
        comparators.remove(c);
    }

    public void validate() throws org.netbeans.modules.schema2beans.ValidateException {
        boolean restrictionFailure = false;
        // Validating property name
        if (getName() == null) {
            throw new org.netbeans.modules.schema2beans.ValidateException("getName() == null", "name", this);	// NOI18N
        }
        // Validating property instance
        for (int _index = 0; _index < sizeInstance(); ++_index) {
            boolean element = isInstance(_index);
        }
        // Validating property instanceName
        // Validating property instanceEnabled
        // Validating property instanceDisableTimeoutInMinutes
        // Validating property instanceListeners
        // Validating property webModule
        for (int _index = 0; _index < sizeWebModule(); ++_index) {
            org.glassfish.loadbalancer.admin.cli.beans.WebModule element = getWebModule(_index);
            if (element != null) {
                element.validate();
            }
        }
        // Validating property healthChecker
        // Validating property healthCheckerUrl
        if (getHealthCheckerUrl() == null) {
            throw new org.netbeans.modules.schema2beans.ValidateException("getHealthCheckerUrl() == null", "healthCheckerUrl", this);	// NOI18N
        }
        // Validating property healthCheckerIntervalInSeconds
        if (getHealthCheckerIntervalInSeconds() == null) {
            throw new org.netbeans.modules.schema2beans.ValidateException("getHealthCheckerIntervalInSeconds() == null", "healthCheckerIntervalInSeconds", this);	// NOI18N
        }
        // Validating property healthCheckerTimeoutInSeconds
        if (getHealthCheckerTimeoutInSeconds() == null) {
            throw new org.netbeans.modules.schema2beans.ValidateException("getHealthCheckerTimeoutInSeconds() == null", "healthCheckerTimeoutInSeconds", this);	// NOI18N
        }
    }

    // Dump the content of this bean returning it as a String
    @Override
    public void dump(StringBuffer str, String indent) {
        String s;
        Object o;
        org.netbeans.modules.schema2beans.BaseBean n;
        str.append(indent);
        str.append("Instance[" + this.sizeInstance() + "]");	// NOI18N
        for (int i = 0; i < this.sizeInstance(); i++) {
            str.append(indent + "\t");
            str.append("#" + i + ":");
            str.append(indent + "\t");	// NOI18N
            str.append((this.isInstance(i) ? "true" : "false"));
            this.dumpAttributes(INSTANCE, i, str, indent);
        }

        str.append(indent);
        str.append("WebModule[" + this.sizeWebModule() + "]");	// NOI18N
        for (int i = 0; i < this.sizeWebModule(); i++) {
            str.append(indent + "\t");
            str.append("#" + i + ":");
            n = (org.netbeans.modules.schema2beans.BaseBean) this.getWebModule(i);
            if (n != null) {
                n.dump(str, indent + "\t");	// NOI18N
            } else {
                str.append(indent + "\tnull");	// NOI18N
            }
            this.dumpAttributes(WEB_MODULE, i, str, indent);
        }

        str.append(indent);
        str.append("HealthChecker");	// NOI18N
        str.append(indent + "\t");	// NOI18N
        str.append((this.isHealthChecker() ? "true" : "false"));
        this.dumpAttributes(HEALTH_CHECKER, 0, str, indent);

    }

    @Override
    public String dumpBeanNode() {
        StringBuffer str = new StringBuffer();
        str.append("Cluster\n");	// NOI18N
        this.dump(str, "\n  ");	// NOI18N
        return str.toString();
    }
}
// END_NOI18N

