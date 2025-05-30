package com.sun.enterprise.deployment.annotation.factory;

import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.ProcessingResult;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.util.logging.Level;

public class JandexAnnotationProcessor implements org.glassfish.apf.AnnotationProcessor {
    private Class<?> type;

    @Override
    public ProcessingContext createContext() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws AnnotationProcessorException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx, Class[] classes) throws AnnotationProcessorException {
        if (classes.length != 1) {
            throw new IllegalArgumentException("Only one class is supported.");
        }
        this.type = classes[0];
        return null;
    }

    @Override
    public void pushAnnotationHandler(AnnotationHandler handler) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public AnnotationHandler getAnnotationHandler(Class<? extends Annotation> type) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void popAnnotationHandler(Class<? extends Annotation> type) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public AnnotatedElement getLastAnnotatedElement(ElementType type) {
        if (type != ElementType.TYPE) {
            throw new IllegalArgumentException("Only ElementType.TYPE is supported.");
        }
        return this.type;
    }

    @Override
    public void log(Level level, AnnotationInfo locator, String localizedMessage) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
