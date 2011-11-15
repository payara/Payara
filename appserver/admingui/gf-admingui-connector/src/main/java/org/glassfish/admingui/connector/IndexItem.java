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
 *  <p>	This class is configured via XML (i.e. a console-config.xml file).
 *  	This is done via the HK2 <code>ConfigParser</code>.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
@Configured(name="indexitem")
public class IndexItem {

    /**
     *	<p> Accessor for child {@link TOCItem}s.</p>
     */
    public List<IndexItem> getIndexItems() {
	return this.indexItems;
    }

    /**
     *	<p> {@link IntegrationPoint}s setter.</p>
     */
    @Element("indexitem")
    public void setIndexItems(List<IndexItem> indexItems) {
	this.indexItems = indexItems;
    }

    /**
     *
     */
    public String getTarget() {
	return this.target;
    }

    /**
     *
     */
    @Attribute(required=true)
    void setTarget(String target) {
	this.target = target;
    }


    /**
     *
     */
    public String getText() {
	return this.text;
    }

    /**
     *
     */
    @Attribute(required=true)
    void setText(String text) {
	this.text = text;
    }

    public String getHtmlFileForTarget() {
        return htmlFileForTarget;
    }

    public void setHtmlFileForTarget(String htmlFileForTarget) {
        this.htmlFileForTarget = htmlFileForTarget;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IndexItem other = (IndexItem) obj;
        if ((this.target == null) ? (other.target != null) : !this.target.equals(other.target)) {
            return false;
        }
        if ((this.text == null) ? (other.text != null) : !this.text.equals(other.text)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.target != null ? this.target.hashCode() : 0);
        hash = 89 * hash + (this.text != null ? this.text.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return getText() + " " + getTarget();
    }

    private String htmlFileForTarget;
    private String target;
    private String text;
    private List<IndexItem> indexItems;

}
