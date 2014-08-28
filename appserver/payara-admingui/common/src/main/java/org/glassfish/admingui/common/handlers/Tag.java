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

package org.glassfish.admingui.common.handlers;

import java.util.ArrayList;
import java.util.List;

/**
 *  <p>	Tag Class.</p>
 *
 *  @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class Tag implements java.io.Serializable {

    private static final long serialVersionUID = 7437635853139196986L;
    private String tagViewId = null;
    private String tagName = null;
    private String displayName = null;
    private List<String> users = null;

    /**
     *	<p> Default constructor.</p>
     */
    Tag() {
    }

    /**
     *	<p> The constructor that should normally be used.</p>
     */
    public Tag(String tagName, String tagViewId, String displayName, String user) {
        this.tagName = tagName;
        this.tagViewId = tagViewId;
        this.displayName = displayName;
        if (user != null) {
            this.users = new ArrayList<String>();
            this.users.add(user);
        }
    }

    /**
     *	<p> Allows an additional user to be added as a Tag creator.</p>
     */
    public void addUser(String name) {
        if (users == null) {
            users = new ArrayList<String>();
        }
        users.add(name);
    }

    /**
     *	<p> Provides access to all the users that have created this Tag.  This
     *	    may be null.</p>
     */
    public List<String> getUsers() {
        return users;
    }

    /**
     *  <p>	Checks to see if the given user is an owner of this Tag.</p>
     */
    public boolean containsUser(String name) {
        return (users == null) ? false : users.contains(name);
    }

    /**
     *  <p> This method ensures the specified <code>user</code> is removed
     *	    from the list of users for this <code>Tag</code>.</p>
     *
     *  <p> While a <code>Tag</code> is of little or no use when 0 users own
     *	    the <code>Tag</code>, it is not the responsibility of this method
     *	    to remove the <code>Tag</code> if this state occurs as a result of
     *	    a call to this method.</p>
     *
     *	@return	The <code>List</code> of users remaining after removing this
     *		user, or <code>null</code> if none.
     */
    public List<String> removeUser(String name) {
        if (users != null) {
            users.remove(name);
            if (users.size() == 0) {
                users = null;
            }
        }
        return users;
    }

    /**
     *	<p> This implementation of equals only checks the tagName and the
     *	    tagViewId for equality.  This means 2 tags with different user
     *	    Lists are still considered equal.  The Display Name is also of no
     *	    importance to this implementation of equality.</p>
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof Tag) {
            Tag testTag = (Tag) obj;
            result = getTagName().equals(testTag.getTagName()) && getTagViewId().equals(testTag.getTagViewId());
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.tagViewId != null ? this.tagViewId.hashCode() : 0);
        hash = 89 * hash + (this.tagName != null ? this.tagName.hashCode() : 0);
        hash = 89 * hash + (this.displayName != null ? this.displayName.hashCode() : 0);
        return hash;
    }

    /**
     *  <p>	String representation of this Tag.</p>
     */
    @Override
    public String toString() {
        return "[" + getTagName() + ", " + getTagViewId() + ", " + getDisplayName() + ", Users: {" + users + "}]";
    }

    /**
     *	<p> This provides access to the tag name.</p>
     */
    public String getTagName() {
        return tagName;
    }

    /**
     *	<p> This provides access to the TagViewId value.</p>
     */
    public String getTagViewId() {
        return tagViewId;
    }

    /**
     *	<p> This returns a <code>String</code> that is meaningful to the user
     *	    which represents the content of this <code>Tag</code> instance.</p>
     */
    public String getDisplayName() {
        // FIXME: I have I18N concerns about this... perhaps it's acceptible to
        // FIXME: store a String localized at the time the page is tagged.
        // FIXME: Multiple language environments may not like this.  To fix
        // FIXME: this correctly, we not only need the ValueExpression (i.e.
        // FIXME: #{i18n.foo}), but we also need the resource bundle to use.
        return displayName;
    }
}
