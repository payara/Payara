/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.common.util.admin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.ParameterMap;

/**
 * Extracts a ParameterMap corresponding to the
 * parameters defined on injected objects, such as AdminCommand or
 * CommandParameters implementations.
 * <p>
 * This can be useful from a supplemental command which needs to create a new
 * command invocation, with parameters similar to its own, for execution on
 * a cluster.
 *
 * @author Tim Quinn
 */
public class ParameterMapExtractor {

    private final Object[] injectionTargets;

    /**
     * Creates a new extractor based on the injected values in one or more
     * injected targets, typically AdminCommand or CommandParameters
     * implementations.
     * <p>
     * Note that the objects are processed in the order specified, and any
     * values set in later objects will override values that were set from
     * earlier objects.
     *
     * @param targets the objects to inspect for injected @Param values
     * @throws IllegalArgumentException if a null is passed for the targets
     */
    public ParameterMapExtractor(final Object... targets) {
        if (targets == null) {
            throw new IllegalArgumentException();
        }
        injectionTargets = targets;
    }

    /**
     * Creates a ParameterMap from the @Param fields defined on the
     * injected objects provided in the constructor call.
     * @return ParameterMap for the parameters injected into the admin object
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public ParameterMap extract() throws IllegalArgumentException, IllegalAccessException {
        return extract(Collections.EMPTY_SET);
    }

    /**
     * Creates a ParameterMap from the @Param fields defined on the
     * injected objects provided in the constructor call, excluding selected parameters.
     * @param parameterNamesToExclude parameter names to exclude from the parameter map
     * @return ParameterMap for the parameters injected into the targets passed to the constructor
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public ParameterMap extract(final Collection<String> parameterNamesToExclude)
            throws IllegalArgumentException, IllegalAccessException {
        final ParameterMap paramMap = new ParameterMap();

        for (Object target : injectionTargets) {
            if (target != null) {
                extract(target, parameterNamesToExclude, paramMap);
            }
        }
        return paramMap;
    }

    private void extract(final Object target,
            final Collection<String> parameterNamesToExclude,
            final ParameterMap paramMap) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : target.getClass().getFields()) {
            final Param param = f.getAnnotation(Param.class);
            if (param != null &&
                    ! parameterNamesToExclude.contains(f.getName())) {
                final Object fieldValue = f.get(target);
                if (fieldValue != null) {
                    final String paramName = param.primary() ? "DEFAULT" :  CommandModel.getParamName(param, f);
                    if (param.multiple()) {
                        paramMap.set(paramName, multipleValue(param, f.get(target)));
                    } else {
                        paramMap.set(paramName, singleValue(param, f.get(target)));
                    }
                }
            }
        }
    }
    
    private String singleValue(final Param p, final Object value) {
        if (value.getClass().isAssignableFrom(String.class)) {
            return (String) value;
        }

        if (value.getClass().isAssignableFrom(File.class)) {
            return ((File) value).getAbsolutePath();
        }

        if (value.getClass().isAssignableFrom(Properties.class)) {
            return propertiesValue((Properties) value, p.separator());
        }

        if (value.getClass().isAssignableFrom(List.class)) {
            return listValue((List) value, p.separator());
        }

        if (value.getClass().isAssignableFrom(Boolean.class)) {
            return ((Boolean) value).toString();
        }

        if (value.getClass().isAssignableFrom(String[].class)) {
            return stringListValue((String[]) value, p.separator());
        }

        return value.toString();
    }

    private String listValue(final List list, final char sep) {
        final StringBuilder sb = new StringBuilder();
        String currentSep = "";
        for (Object o : list) {
            sb.append(currentSep).append(o.toString());
            currentSep = String.valueOf(sep);
        }
        return sb.toString();
    }

    private String stringListValue(final String[] value, final char sep) {
        final StringBuilder sb = new StringBuilder();
        String currentSep = "";
        for (String s : value) {
            sb.append(currentSep).append(s);
            currentSep = String.valueOf(sep);
        }
        return sb.toString();
    }

    public static String propertiesToPropertiesString(final Properties props, final char sep) {
        final StringBuilder sb = new StringBuilder();
        final String sepString = String.valueOf(sep);
        final String sepQuote = new StringBuilder("\\\\").append(sep).toString();
        String currentSep = "";
        for (Enumeration en = props.propertyNames(); en.hasMoreElements();) {
            final Object key = en.nextElement();
            final Object v = props.get(key);
            /*
             * Must escape the separator character where it appears in the
             * value...such as URIs.
             */
            sb.append(currentSep).append(key.toString()).append("=").
                    append(v.toString().replaceAll(sepString, sepQuote));
            currentSep = sepString;
        }
        return sb.toString();
    }

    public String propertiesValue(final Properties props, final char sep) {
        return propertiesToPropertiesString(props, sep);
    }

    private Collection<? extends String> multipleValue(final Param p, final Object value) {

        final Collection<String> result = new ArrayList<String>();
        final List<? extends Object> multiValue = (List<? extends Object>) value;
        for (Object o : multiValue) {
            result.add(singleValue(p, o));
        }
        return result;
    }
}
