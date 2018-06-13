package com.sun.enterprise.admin.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities class for JarFiles
 *
 * @author Sven Diedrichsen
 * @since 13.06.18
 */
public class JarFileUtils {

    private static final Logger LOG = Logger.getLogger(JarFileUtils.class.getName());

    /**
     * Ensures that all cached JarFile instances are closed.
     */
    public static void closeCachedJarFiles() {
        try {
            Map<String, JarFile> files = null;
            Object jarFileFactoryInstance = null;
            Class clazz = Class.forName("sun.net.www.protocol.jar.JarFileFactory", true, URL.class.getClassLoader());
            Field fields[] = clazz.getDeclaredFields();
            for (Field field : fields) {
                if ("fileCache".equals(field.getName())) {
                    field.setAccessible(true);
                    files = (Map<String, JarFile>) field.get(null);
                }
                if ("instance".equals(field.getName())) {
                    field.setAccessible(true);
                    jarFileFactoryInstance = field.get(null);
                }
            }
            if (files != null && !files.isEmpty()) {
                Set<JarFile> jarFiles = new HashSet<>();
                if(jarFileFactoryInstance != null) {
                    synchronized (jarFileFactoryInstance) {
                        jarFiles.addAll(files.values());
                    }
                } else {
                    jarFiles.addAll(files.values());
                }
                for (JarFile file : jarFiles) {
                    if (file != null) {
                        file.close();
                    }
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | SecurityException | IllegalArgumentException | IOException | ConcurrentModificationException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
