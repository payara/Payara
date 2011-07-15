/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;

/**
 * @deprecated Mixin interface indicating that there is at least one anonymous non-singleton
 * leaf element present eg a &lt;jvm-options>.  Supports any number of such elements.
 * <p/>
 * Examples:<br>
 * <pre>
 * setJVMOptions( new String[] { COLLECTION_OP_REPLACE, "-client", "-Xmx" } // replaces all with these
 * setJVMOptions( new String[] { "-client", "-Xmx" }   // adds these
 * setJVMOptions( new String[] { COLLECTION_OP_REMOVE, "-client", "-Xmx" }   // removes these
 * </pre>
 */
@Deprecated
public interface AnonymousElementList {

    /**
     * indicates that the values are to be added to the existing ones
     */
    public static final String OP_ADD = "add";
    /**
     * indicates that the values are to be remove from the existing ones
     */
    public static final String OP_REMOVE = "remove";
    /**
     * indicates that all values are to be replaced with the specified ones
     */
    public static final String OP_REPLACE = "replace";

    /**
     * Return all values of the element list specified by the element name, which must be an
     * anonymous non-singleton simple element eg:
     * <pre>
     * &lt;jvm-options>--client&lt;/jvm-options>
     * &lt;jvm-options>-Dfoo=bar&lt;/jvm-options>
     * <p/>
     * &lt;some-elem>-Dfoo=bar&lt;/some-elem>
     * &lt;some-elem>-Dfoo=bar&lt;/some-elem>
     * ...
     * </pre>
     * For example getCollection( "JVMOptions" ) for certain AMXConfig that have &lt;jvm-options> elements.
     * This is the generic operation; an AMXConfig can choose to implement an Attribute as
     * well eg getJVMOptions().  Either the AMX Attribute name (if present) may be used,
     * or the XML element name may be used. It is suggested that all such element names
     * have a String[] getter so they can at least be treated as read-only Attributes.
     */
    public String[] getAnonymousElementList(final String elementName);

    /**
     * Modify the collection as specified.  To perform a set()-style operation, use
     * modifyCollection(name, COLLECTION_OP_REPLACE, new String[] {...}).
     *
     * @param elementName the name of the collection eg "JVMOptions"
     * @param cmd         the operation to perform
     * @param args        values used by the command
     * @return the entire list
     */
    public String[] modifyAnonymousElementList(final String elementName, final String cmd, final String[] args);
}



