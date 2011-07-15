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

package com.sun.enterprise.deployment;


public class ConnectorConfigProperty extends EnvironmentProperty {

    private boolean ignore = false;
    private boolean supportsDynamicUpdates = false;
    private boolean confidential = false;
    private boolean setIgnoreCalled = false;
    private boolean setConfidentialCalled = false;
    private boolean setSupportsDynamicUpdatesCalled = false;

    /**
    ** copy constructor.
    */
    public ConnectorConfigProperty(ConnectorConfigProperty other) {
        super(other);
    }

    public ConnectorConfigProperty(EnvironmentProperty other){
        super(other);
    }

    /**
    ** Construct an connector  config-property if type String and empty string value and no description.
    */

    public ConnectorConfigProperty() {
    }  

     /**
    ** Construct an connector config-property of given name value and description.
    */

    public ConnectorConfigProperty(String name, String value, String description) {
	    this(name, value, description, null);
    }

    /**
    ** Construct an connector config-property of given name value and description and type.
    ** Throws an IllegalArgumentException if bounds checking is true and the value cannot be
    ** reconciled with the given type.
    */

    public ConnectorConfigProperty(String name, String value, String description, String type) {
    	super(name, value, description, type);
    }

    /**
    ** Construct an connector config-property of given name value and description and type.
    ** Throws an IllegalArgumentException if bounds checking is true and the value cannot be
    ** reconciled with the given type.
    */
    public ConnectorConfigProperty(String name, String value, String description, String type,
                                   boolean ignore, boolean supportsDynamicUpdates, boolean confidential) {
    	super(name, value, description, type);
        this.ignore = ignore;
        this.supportsDynamicUpdates = supportsDynamicUpdates;
        this.confidential = confidential;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
        setSetIgnoreCalled(true);
    }

    public boolean isSupportsDynamicUpdates() {
        return supportsDynamicUpdates;
    }

    public void setSupportsDynamicUpdates(boolean supportsDynamicUpdates) {
        this.supportsDynamicUpdates = supportsDynamicUpdates;
        setSetSupportsDynamicUpdatesCalled(true);
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
        setSetConfidentialCalled(true);
    }

    public boolean isSetIgnoreCalled() {
        return setIgnoreCalled;
    }

    public void setSetIgnoreCalled(boolean setIgnoreCalled) {
        this.setIgnoreCalled = setIgnoreCalled;
    }

    public boolean isSetConfidentialCalled() {
        return setConfidentialCalled;
    }

    public void setSetConfidentialCalled(boolean setConfidentialCalled) {
        this.setConfidentialCalled = setConfidentialCalled;
    }

    public boolean isSetSupportsDynamicUpdatesCalled() {
        return setSupportsDynamicUpdatesCalled;
    }

    public void setSetSupportsDynamicUpdatesCalled(boolean setSupportsDynamicUpdatesCalled) {
        this.setSupportsDynamicUpdatesCalled = setSupportsDynamicUpdatesCalled;
    }
}
