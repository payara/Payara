package com.sun.enterprise.deployment.annotation.factory;

import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationProcessor;
import org.glassfish.apf.ErrorHandler;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.Scanner;
import org.glassfish.apf.context.AnnotationContext;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;

class AnnotationProcessingContext implements ProcessingContext {
    private final AnnotatedElementHandler handler;
    private final ReadableArchive archive;
    private final Deque<AnnotatedElementHandler> handlers = new ArrayDeque<>();
    private final AnnotationProcessor processor = new JandexAnnotationProcessor();

    AnnotationProcessingContext(AnnotatedElementHandler handler, ReadableArchive archive) {
        this.handler = handler;
        this.archive = archive;
        pushHandler(handler);
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
    public void pushHandler(AnnotatedElementHandler handler) {
        if (handler instanceof AnnotationContext) {
            ((AnnotationContext) handler).setProcessingContext(this);
        }
        handlers.push(handler);
    }

    @Override
    public AnnotatedElementHandler getHandler() {
        if (handlers.isEmpty())
            return null;

        return handlers.peek();
    }

    @Override
    public AnnotatedElementHandler popHandler() {
        if (handlers.isEmpty())
            return null;

        return handlers.pop();
    }

    @Override
    public <U extends AnnotatedElementHandler> U getHandler(Class<U> handlerType) throws ClassCastException {

        if (handlers.isEmpty())
            return null;
        if (AnnotationUtils.shouldLog("handler")) {
            AnnotationUtils.getLogger().log(Level.FINER, "Top handler is {0}", handlers.peek());
        }
        return handlerType.cast(handlers.peek());
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

    void popAllHandlers() {
        while (handlers.peek() != handler) {
            handlers.pop();
        }
    }

    int handlersSize() {
        return handlers.size();
    }
}
