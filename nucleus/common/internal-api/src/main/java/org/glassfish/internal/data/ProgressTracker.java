/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.data;

import java.util.*;
import java.util.logging.Logger;

/**
 * Simple tracker objects to remember what operations were performed
 *
 * @author Jerome Dochez
 */
public abstract class ProgressTracker {

    Map<String, List<Object>> subjects = new HashMap<String, List<Object>>();

    public synchronized <T> void add(String name, Class<T> type, T subject) {
        if (!subjects.containsKey(name)) {
            subjects.put(name, new ArrayList());
        }
        subjects.get(name).add(subject);
        
    }


    public <T> void add(Class<T> type, T subject) {
        add(type.getName(), type, subject);
    }

    public <T> void addAll(Class<T> type, Iterable<T> subjects) {
        for (T subject : subjects) {
            add(type, subject);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> get(String name, Class<T> type) {
        if (subjects.containsKey(name)) {
            return (List<T>) subjects.get(name);
        } else {
            return Collections.emptyList();
        }
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> get(Class<T> type) {
        return get(type.getName(), type);
    }

    public abstract void actOn(Logger logger);
}
