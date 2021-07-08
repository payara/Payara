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
