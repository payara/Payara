package com.sun.enterprise.deployment.annotation.factory;

import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationProcessor;
import org.glassfish.apf.ErrorHandler;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.Scanner;
import org.glassfish.api.deployment.archive.ReadableArchive;

class AnnotationProcessingContext implements ProcessingContext {
    private final AnnotatedElementHandler handler;
    private final ReadableArchive archive;
    private final AnnotationProcessor processor = new JandexAnnotationProcessor();

    AnnotationProcessingContext(AnnotatedElementHandler handler, ReadableArchive archive) {
        this.handler = handler;
        this.archive = archive;
    }

    @Override
    public AnnotationProcessor getProcessor() {
        return processor;
    }

    @Override
    public Scanner getProcessingInput() {
        return null;
    }

    @Override
    public ReadableArchive getArchive() {
        return archive;
    }

    @Override
    public void setArchive(ReadableArchive archive) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setProcessingInput(Scanner scanner) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    @Deprecated(forRemoval = true)
    public void pushHandler(AnnotatedElementHandler handler) {
        // no-op
    }

    @Override
    public AnnotatedElementHandler getHandler() {
        return handler;
    }

    @Override
    @Deprecated(forRemoval = true)
    public AnnotatedElementHandler popHandler() {
        return handler;
    }

    @Override
    public <U extends AnnotatedElementHandler> U getHandler(Class<U> handlerType) throws ClassCastException {
        return handlerType.cast(handler);
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }
}
