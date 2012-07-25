/*
 * Remove this
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
        public final long timestamp;
        public final String message;
        public final String param;

        public Stat(String message, String param) {
            this.timestamp = System.currentTimeMillis();
            this.message = message;
            this.param = param;
        }
        
    }
    
    private static final Metrix instance = new Metrix();
    private static final long timestamp = System.currentTimeMillis();
    
    private List<Stat> list = new ArrayList<Metrix.Stat>(64);

    private Metrix() {
    }
    
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("duration, delta, event\n");
        long lastTS = timestamp;
        for (Stat stat : list) {
            res.append(stat.timestamp - timestamp).append(", ");
            res.append(stat.timestamp - lastTS).append(", ");
            res.append(stat.message);
            if (stat.param != null) {
                res.append(" - ").append(stat.param);
            }
            res.append('\n');
            lastTS = stat.timestamp;
        }
        return res.toString();
    }
    
    // ---------- Static API
    
    public static void event(String message) {
        instance.list.add(new Stat(message, null));
    }
    
    public static void event(String message, String param) {
        instance.list.add(new Stat(message, null));
    }
    
    public static Metrix getInstance() {
        return instance;
    }
    
}
