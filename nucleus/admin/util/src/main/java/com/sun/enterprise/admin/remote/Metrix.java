/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 * Remove this class
 */
package com.sun.enterprise.admin.remote;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mmares
 */
public class Metrix {
    
    public static class Stat {
        public final long TIMESTAMP;
        public final String message;
        public final String param;

        public Stat(String message, String param) {
            this.TIMESTAMP = System.currentTimeMillis();
            this.message = message;
            this.param = param;
        }
        
    }
    
    private static final Metrix INSTANCE = new Metrix();
    private static final long TIMESTAMP = System.currentTimeMillis();
    
    private List<Stat> list = new ArrayList<Metrix.Stat>(64);

    private Metrix() {
    }
    
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("duration, delta, event\n");
        long lastTS = TIMESTAMP;
        for (Stat stat : list) {
            res.append(stat.TIMESTAMP - TIMESTAMP).append(", ");
            res.append(stat.TIMESTAMP - lastTS).append(", ");
            res.append(stat.message);
            if (stat.param != null) {
                res.append(" - ").append(stat.param);
            }
            res.append('\n');
            lastTS = stat.TIMESTAMP;
        }
        return res.toString();
    }
    
    // ---------- Static API
    
    public static void event(String message) {
        INSTANCE.list.add(new Stat(message, null));
    }
    
    public static void event(String message, String param) {
        INSTANCE.list.add(new Stat(message, null));
    }
    
    public static Metrix getInstance() {
        return INSTANCE;
    }
    
}
