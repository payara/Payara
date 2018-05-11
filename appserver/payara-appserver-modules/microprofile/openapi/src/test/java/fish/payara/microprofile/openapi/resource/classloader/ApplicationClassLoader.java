package fish.payara.microprofile.openapi.resource.classloader;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class ApplicationClassLoader extends ClassLoader {

    private Set<Class<?>> appClasses;

    public ApplicationClassLoader(Application app, Set<Class<?>> extraClasses) {
        super();
        appClasses = new HashSet<>();
        appClasses.add(app.getClass());
        appClasses.addAll(app.getClasses());
        if (extraClasses != null) {
            appClasses.addAll(extraClasses);
        }
        for (Class<?> clazz : appClasses) {
            try {
                loadClass(clazz.getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public ApplicationClassLoader(Application app) {
        this(app, null);
    }

    private Class<?> getClass(String name) throws ClassNotFoundException {
        String file = name.replace('.', File.separatorChar) + ".class";
        byte[] b = null;
        try {
            b = loadClassData(file);
            Class<?> c = defineClass(name, b, 0, b.length);
            resolveClass(c);
            return c;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (Class<?> clazz : appClasses) {
            if (clazz.getName().equals(name)) {
                return getClass(name);
            }
        }
        return super.loadClass(name);
    }

    private byte[] loadClassData(String name) throws IOException {
        // Opening the file
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        int size = stream.available();
        byte buff[] = new byte[size];
        DataInputStream in = new DataInputStream(stream);
        // Reading the binary data
        in.readFully(buff);
        in.close();
        return buff;
    }

}