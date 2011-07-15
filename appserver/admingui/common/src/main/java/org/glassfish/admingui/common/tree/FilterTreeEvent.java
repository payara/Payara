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

package org.glassfish.admingui.common.tree;
//

import com.sun.jsftemplating.layout.event.EventObjectBase;
import com.sun.jsftemplating.layout.event.UIComponentHolder;
import java.util.List;
import javax.faces.component.UIComponent;

/**
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
public class FilterTreeEvent extends EventObjectBase {

    /**
     *	<p> Constructor.</p>
     *
     *	@param	component   The <code>UIComponent</code> associated with this
     *			    <code>EventObject</code>.
     */
    public FilterTreeEvent(UIComponent component, List childObjects) {
	super(component);
        setChildObjects(childObjects);
    }

    /**
     *	<p> This method provides access to an array of Objects that are to
     *	    become child <code>TreeNode</code>s.  This allows you to manipluate
     *	    them (filter them) before they are processed.  You may return a new
     *	    List from your handler that processes this event.  Note that
     *	    you NOT set the child object array using this event.</p>
     */
    public List getChildObjects() {
        return _childObjects;
    }
    
    /**
     *	<p> This method is protected because it is only meaningful to set this
     *	    array during the creation of this event.  Setting it any other
     *	    time would not effect the original data structure and would serve
     *	    no purpose.  To provide a different object array, return a new
     *	    <code>Object[]</code> from your handler that processes this
     *	    event.</p>
     */
    protected void setChildObjects(List objects) {
        _childObjects = objects;
    }

    /**
     *	<p> The "filterTree" event type. ("filterTree")</p>
     */
    public static final String	EVENT_TYPE  = "filterTree";
    
    private List _childObjects = null;
}
