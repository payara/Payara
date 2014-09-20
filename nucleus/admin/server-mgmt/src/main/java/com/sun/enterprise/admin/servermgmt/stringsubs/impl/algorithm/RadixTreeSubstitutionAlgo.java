/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.stringsubs.impl.algorithm;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import com.sun.enterprise.admin.servermgmt.stringsubs.StringSubstitutionException;
import com.sun.enterprise.admin.servermgmt.stringsubs.Substitutable;
import com.sun.enterprise.admin.servermgmt.stringsubs.SubstitutionAlgorithm;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Perform's string substitution by constructing the {@link RadixTree} of change-value
 * pair.
 * 
 * @see RadixTreeSubstitution
 */
public class RadixTreeSubstitutionAlgo implements SubstitutionAlgorithm {

    private RadixTree _tree;
    private static final LocalStringsImpl _strings = new LocalStringsImpl(RadixTreeSubstitutionAlgo.class);

    /**
     * Construct {@link RadixTreeSubstitutionAlgo} for the given substitutable key/value
     * pair by constructing the radix tree for the same.
     *
     * @param substitutionMap Map of substitutable key/value pairs.
     */
    public RadixTreeSubstitutionAlgo(Map<String, String> substitutionMap) {
        if (substitutionMap == null || substitutionMap.isEmpty()) {
            throw new IllegalArgumentException(_strings.get("noKeyValuePairForSubstitution"));
        }
        _tree = new RadixTree();
        for (Map.Entry<String, String> entry : substitutionMap.entrySet()) {
            _tree.insert(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void substitute(Substitutable substitutable)
            throws StringSubstitutionException {
        Reader reader = substitutable.getReader();
        Writer writer = substitutable.getWriter();
        RadixTreeSubstitution sub = new RadixTreeSubstitution(_tree);
        String output = null;
        char[] cbuffer = new char[8192];
        int count = 0;
        try {
            while ((count = reader.read(cbuffer)) > 0) {
                for (int i = 0; i < count; i++) {
                    output = sub.substitute(cbuffer[i]);
                    if (output != null) {
                        writer.write(output);
                    }
                }
            }
            output = sub.substitute(null);
            if (output != null) {
                writer.write(output);
            }
            writer.flush();
        } catch (IOException e) {
            throw new StringSubstitutionException(_strings.get("errorInStringSubstitution", substitutable.getName()), e);
        }
    }
}