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
 *	<code>ConfigParser</code>.  This is the root node of a "gadget".
 *	The "text" can be retrived via getText (or #{module.text} via
 *	EL).</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
@Configured(name="Module")
public class GadgetModule {

    /**
     *	<p> Accessor for the {@link GadgetUserPref}s.</p>
     */
    public List<GadgetUserPref> getGadgetUserPrefs() {
	return this.userPrefs;
    }

    /**
     *	<p> {@link GadgetUserPref}s setter.</p>
     */
    @Element("UserPref")
    void setGadgetUserPref(List<GadgetUserPref> userPrefs) {
	this.userPrefs = userPrefs;
    }

    private List<GadgetUserPref> userPrefs = null;

    /**
     *	<p> Accessor for the {@link GadgetModulePrefs}.</p>
     */
    public GadgetModulePrefs getGadgetModulePrefs() {
	return this.prefs;
    }

    /**
     *	<p> {@link GadgetModulePrefs} setter.</p>
     */
    @Element("ModulePrefs")
    void setGadgetModulePrefs(GadgetModulePrefs prefs) {
	this.prefs = prefs;
    }

    private GadgetModulePrefs prefs = null;

    /**
     *	<p> A unique identifier for the content.</p>
    public GadgetContent getContent() {
	return this.content;
    }
     */

    /**
     *	<p> Setter for the content.</p>
FIXME: I can't seem to get the attributes while also getting the body content...
    @Element("Content")
    void setContent(GadgetContent content) {
	this.content = content;
    }

    private GadgetContent content;
     */

    /**
     *	<p> A unique identifier for the text.</p>
     */
    public String getText() {
	return this.text;
    }

    /**
     *	<p> Setter for the text.</p>
     */
    @Element("Content")
    void setText(String text) {
	this.text = text;
    }

    private String text;
}
