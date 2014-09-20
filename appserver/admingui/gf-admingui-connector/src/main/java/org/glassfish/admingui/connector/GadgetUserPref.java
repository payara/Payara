/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.connector;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;

import java.util.List;


/**
 *  <p>	This class is configured via XML.  This is done via the HK2
 *	<code>ConfigParser</code>.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
@Configured(name="UserPref")
public class GadgetUserPref {

    /**
     *	<p> Getter for the name.</p>
     */
    public String getName() {
	return this.name;
    }

    /**
     *	<p> Required name of the user preference. Displayed during editing if
     *	    no "display_name" is defined. Must only contain letters, number and
     *	    underscores. The value must be unique for this gadget.</p>
     */
    @Attribute(value="name", required=true, key=true)
    void setName(String name) {
	this.name = name;
    }

    private String name;

    /**
     *	<p> Getter for the display_name.</p>
     */
    public String getDisplayName() {
	return this.displayName;
    }

    /**
     *	<p> Optional string to display in the user preferences edit window.</p>
     */
    @Attribute(value="display_name", required=false)
    void setDisplayName(String displayName) {
	this.displayName = displayName;
    }

    private String displayName;

    /**
     *	<p> Getter for the urlparam property.</p>
     */
    public String getURLParam() {
	return this.urlparam;
    }

    /**
     *	<p> Optional string to pass as the parameter name for content
     *	    type="url" (currently not supported).</p>
     */
    @Attribute(value="urlparam", required=false)
    void setURLParam(String urlparam) {
	this.urlparam = urlparam;
    }

    private String urlparam;

    /**
     *	<p> Getter for the datatype property.</p>
     */
    public String getDataType() {
	return this.datatype;
    }

    /**
     *	<p> Optional string that indicates the data type of this attribute.
     *	    Can be string, bool, enum, hidden (not shown to user), or list
     *	    (dynamic array generated from user input). The default is
     *	    string.</p>
     */
    @Attribute(value="datatype", required=false)
    void setDataType(String datatype) {
	this.datatype = datatype;
    }

    private String datatype;

    /**
     *	<p> Getter for the required property.</p>
     */
    public boolean getRequired() {
	return this.required;
    }

    /**
     *	<p> Boolean property indicating if the preference is required. The
     *	    default is false. </p>
     */
    @Attribute(value="required", required=false, dataType=Boolean.class, defaultValue="false")
    void setRequired(boolean required) {
	this.required = required;
    }

    private boolean required;

    /**
     *	<p> Getter for the default value of this preference.</p>
     */
    public String getDefaultValue() {
	return this.defaultValue;
    }

    /**
     *	<p> Setter for the defaultValue.</p>
     */
    @Attribute(value="default_value", required=false)
    void setDefaultValue(String defaultValue) {
	this.defaultValue = defaultValue;
    }

    private String defaultValue;
}
