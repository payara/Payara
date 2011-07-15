/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.util.List;

/**
 *  <p>	This class contains handlers for managing preferences.</p>
 *
 * @author jasonlee
 * @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class PreferencesHandler {

    /**
     *	<p> This handler should be used whenever you want to add a Tag to a
     *	    page.  If the exact same Tag is added twice, it will be
     *	    ignored.  If "user" is not specified the current principal user
     *	    will be used for this value.</p>
     */
    @Handler(id = "gf.addTag",
        input = {
            @HandlerInput(name="tagName", type=String.class, required=true),
            @HandlerInput(name="tagViewId", type=String.class, required=true),
            @HandlerInput(name="displayName", type=String.class),
            @HandlerInput(name="user", type=String.class)
        }
    )
    public static void saveTagInformation(HandlerContext handlerCtx) {
	String user = (String) handlerCtx.getInputValue("user");
	if (user == null) {
	    user = handlerCtx.getFacesContext().getExternalContext().
		    getUserPrincipal().getName();
	}
	TagSupport.addTag(
	    (String) handlerCtx.getInputValue("tagName"), 
	    (String) handlerCtx.getInputValue("tagViewId"), 
	    (String) handlerCtx.getInputValue("displayName"), 
	    user);
    }

    /**
     *	<p> This handler provides a way to search for tags.  All 3 properties
     *	    are optional.  If none are specified, all tags will be returned.
     *	    If more than one are specified, tags matching all specified
     *	    criteria will be returned.</p>
     */
    @Handler(id="gf.queryTags",
        input = {
	    @HandlerInput(name="tagName", type=String.class),
	    @HandlerInput(name="tagViewId", type=String.class),
	    @HandlerInput(name="user", type=String.class)
	    },
        output = {
	    @HandlerOutput(name="results", type=List.class) })
    public static void searchTags(HandlerContext handlerCtx) {
	// Perform Search
	List<Tag> results = TagSupport.queryTags(
	    (String) handlerCtx.getInputValue("tagName"),
	    (String) handlerCtx.getInputValue("tagViewId"), 
	    (String) handlerCtx.getInputValue("user"));

	// Set the results...
        handlerCtx.setOutputValue("results", results);
    }

    /**
     *	<p> This handler provides a way to remove tags.  If the user is not
     *	    specified, the current "principal user" will be used.</p>
     */
    @Handler(id="gf.removeTag",
	input = {
	    @HandlerInput(name="tagName", type=String.class, required=true),
	    @HandlerInput(name="tagViewId", type=String.class, required=true),
	    @HandlerInput(name="user", type=String.class) } )
    public static void removeTag(HandlerContext handlerCtx) {
	// Make sure we have the user...
	String user = (String) handlerCtx.getInputValue("user");
	if (user == null) {
	    user = handlerCtx.getFacesContext().getExternalContext().
		    getUserPrincipal().getName();
	}

	// Delete...
	TagSupport.removeTag(
	    (String) handlerCtx.getInputValue("tagName"),
	    (String) handlerCtx.getInputValue("tagViewId"), 
	    user);
    }

    /**
     *	<p> This handler normalizes the given tagViewId.  This is required in
     *	    order to ensure tagViewId's are compared the same way every
     *	    time.</p>
     */
    @Handler(id="gf.normalizeTagViewId",
        input = {
	    @HandlerInput(name="viewId", type=String.class, required=true) },
	output = {
	    @HandlerOutput(name="tagViewId", type=String.class )})
    public static void normalizeTagViewId(HandlerContext handlerCtx) {
	handlerCtx.setOutputValue("tagViewId",
	    TagSupport.normalizeTagViewId(
		    (String) handlerCtx.getInputValue("viewId")));
    }
}
