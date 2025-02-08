package org.glassfish.apf.hk2;

import org.glassfish.hk2.classmodel.reflect.util.ResourceLocator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;

class ClassloaderResourceLocatorAdapter implements ResourceLocator {
    private static String[] BLACKLIST = {"java/","com/sun"};

    private ClassLoader delegate;

    ClassloaderResourceLocatorAdapter(ClassLoader delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream openResourceStream(String name) throws IOException {
        return isAllowed(name) ? delegate.getResourceAsStream(name) : null;
    }

    @Override
    public URL getResource(String name) {
        return isAllowed(name) ? delegate.getResource(name) : null;
    }

    private static boolean isAllowed(String name) {
        return Stream.of(BLACKLIST).noneMatch(p -> name.startsWith(p));
    }
}
