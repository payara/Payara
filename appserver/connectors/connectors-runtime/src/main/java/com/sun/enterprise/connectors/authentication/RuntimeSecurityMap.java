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

package com.sun.enterprise.connectors.authentication;

import java.util.HashMap;

/**
 * @author Kanwar Oberoi
 */
public class RuntimeSecurityMap {

    private HashMap userMap;

    private HashMap groupMap;

    public RuntimeSecurityMap() {
        this.userMap = new HashMap();
        this.groupMap = new HashMap();
    }

    public RuntimeSecurityMap(HashMap userMap, HashMap groupMap) {
        this.userMap = (HashMap) userMap.clone();
        this.groupMap = (HashMap) groupMap.clone();
    }

    public boolean equals(Object map) {
        if (map instanceof RuntimeSecurityMap) {
            RuntimeSecurityMap rsm = (RuntimeSecurityMap) map;
            if (!(rsm.userMap.equals(this.userMap)))
                return false;
            else if (!(rsm.groupMap.equals(this.groupMap)))
                return false;
            else
                return true;
        }
        return false;
    }

    public int hashCode(){
        return this.userMap.hashCode() + this.groupMap.hashCode();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        // implement
        return sb.toString();
    }

    public boolean isEmpty() {
        if ((this.userMap.size() == 0) && (this.groupMap.size() == 0))
            return true;
        else
            return false;
    }

    public HashMap getUserMap() {
        return (HashMap) ((this.userMap).clone());
    }

    public HashMap getGroupMap() {
        return (HashMap) ((this.groupMap).clone());
    }

}
