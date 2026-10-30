/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package com.sun.common.util.logging;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SortedLoggingProperties extends Properties {

    private final List<Pattern> patterns;

    private final Properties properties;

    public SortedLoggingProperties(Properties properties) {
        this.properties = properties;

        patterns = Stream.of("handlers"
                , "handlerServices"
                ,"java\\.util\\.logging\\.ConsoleHandler\\.formatter"
                , "java\\.util\\.logging\\.FileHandler.*"
                , "com\\.sun\\.enterprise\\.server\\.logging\\.GFFileHandler.*"
                , "com\\.sun\\.enterprise\\.server\\.logging\\.SyslogHandler.*"
                , "log4j\\.logger\\.org\\.hibernate\\.validator\\.util\\.Version"
                , "com\\.sun\\.enterprise\\.server\\.logging\\.UniformLogFormatter.*"
                , "fish\\.payara\\.enterprise\\.server\\.logging\\.PayaraNotificationFileHandler.*"
                , "fish\\.payara\\.deprecated\\.jsonlogformatter\\.underscoreprefix"
                , "(.*)?\\.level"
        ).map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        List<Object> keys = Collections.list(properties.keys());

        List<Integer> keyPatternIndex
                = keys.stream().map(k -> defineIndex(k.toString())).collect(Collectors.toList());
        LoggingKeySorter keySorter = new LoggingKeySorter(keys, keyPatternIndex);
        keys.sort(keySorter);
        return Collections.enumeration(keys);
    }

    @Override
    public Set<java.util.Map.Entry<Object, Object>> entrySet() {
        List<Object> keys = Collections.list(properties.keys());

        List<Integer> keyPatternIndex
                = keys.stream().map(k -> defineIndex(k.toString())).collect(Collectors.toList());
        LoggingKeySorter keySorter = new LoggingKeySorter(keys, keyPatternIndex);

        TreeMap<Object, Object> propertiesMap = new TreeMap<>(keySorter);
        propertiesMap.putAll(properties);
        Set<Map.Entry<Object, Object>> propertiesSet = propertiesMap.entrySet();
        return propertiesSet;
    }

    private Integer defineIndex(String key) {
        int result = Integer.MAX_VALUE; // Unknowns at the end.
        for (int idx = 0; idx < patterns.size(); idx++) {
            if (patterns.get(idx).matcher(key).matches()) {
                result = idx;
                break;
            }
        }
        return result;
    }

    @Override
    public synchronized Object get(Object key) {
        return properties.get(key);
    }

    private static class LoggingKeySorter implements Comparator<Object>{
        private List<Object> keys;
        private List<Integer> keyPatternIndex;

        public LoggingKeySorter(List<Object> keys, List<Integer> keyPatternIndex) {
            this.keys = new ArrayList<>(keys);
            this.keyPatternIndex = keyPatternIndex;
        }

        @Override
        public int compare(Object k1, Object k2) {
            Integer v1 = keyPatternIndex.get(keys.indexOf(k1));
            Integer v2 = keyPatternIndex.get(keys.indexOf(k2));
            int result = v1.compareTo(v2);
            if (result == 0) {
                result = k1.toString().compareTo(k2.toString());
            }
            return result;
        }
    }
}
