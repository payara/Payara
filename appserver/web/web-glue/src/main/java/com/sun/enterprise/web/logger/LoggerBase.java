/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright 2022 Payara Foundation and/or its affiliates

package com.sun.enterprise.web.logger;

import jakarta.servlet.ServletException;
import org.apache.catalina.LifecycleException;
import org.apache.juli.logging.Log;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Convenience base class for <b>Logger</b> implementations.  The only
 * method that must be implemented is 
 * <code>write(String msg, Level level)</code>, plus any property
 * setting and lifecycle methods required for configuration.
 *
 */

abstract class LoggerBase implements Log {

    /**
     * The descriptive information about this implementation.
     */
    protected static final String info = "com.sun.enterprise.web.logger.LoggerBase/2.0";

    protected Logger logger;

    LoggerBase(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isFatalEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    @Override
    public void trace(Object message) {
        write(message.toString(), Level.FINER);
    }

    @Override
    public void trace(Object message, Throwable throwable) {
        write(message.toString(), throwable, Level.FINER);
    }

    @Override
    public void debug(Object message) {
        write(message.toString(), Level.FINE);
    }

    @Override
    public void debug(Object message, Throwable throwable) {
        write(message.toString(), throwable, Level.FINE);
    }

    @Override
    public void info(Object message) {
        write(message.toString(), Level.INFO);
    }

    @Override
    public void info(Object message, Throwable throwable) {
        write(message.toString(), throwable, Level.INFO);
    }

    @Override
    public void warn(Object message) {
        write(message.toString(), Level.WARNING);
    }

    @Override
    public void warn(Object message, Throwable throwable) {
        write(message.toString(), throwable, Level.WARNING);
    }

    @Override
    public void error(Object message) {
        write(message.toString(), Level.SEVERE);
    }

    @Override
    public void error(Object message, Throwable throwable) {
        write(message.toString(), throwable, Level.SEVERE);
    }

    @Override
    public void fatal(Object message) {
        write(message.toString(), Level.SEVERE);
    }

    @Override
    public void fatal(Object message, Throwable throwable) {
        write(message.toString(), throwable, Level.SEVERE);
    }

    /**
     * Return descriptive information about this Logger implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }

    /**
     * Writes the specified message to a servlet log file, usually an event
     * log.  The name and type of the servlet log is specific to the
     * servlet container. 
     *
     * @param msg A <code>String</code> specifying the message to be
     *  written to the log file
     */
    public void log(String msg) {
        debug(msg);
    }

    /**
     * Writes the specified exception, and message, to a servlet log file.
     * The implementation of this method should call
     * <code>log(msg, exception)</code> instead.  This method is deprecated
     * in the ServletContext interface, but not deprecated here to avoid
     * many useless compiler warnings.  This message will be logged
     * unconditionally.
     *
     * @param exception An <code>Exception</code> to be reported
     * @param msg The associated message string
     */
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }

    /**
     * Writes an explanatory message and a stack trace for a given
     * <code>Throwable</code> exception to the servlet log file.  The name
     * and type of the servlet log file is specific to the servlet container,
     * usually an event log.  This message will be logged unconditionally.
     *
     * @param msg A <code>String</code> that describes the error or
     *  exception
     * @param throwable The <code>Throwable</code> error or exception
     */
    public void log(String msg, Throwable throwable) {
        write(msg, throwable, Level.SEVERE);
    }

    /**
     * Writes the specified message to the servlet log file, usually an event
     * log, if the logger is set to a verbosity level equal to or higher than
     * the specified value for this message.
     *
     * @param message A <code>String</code> specifying the message to be
     *  written to the log file
     * @param level Verbosity {@link Level} of this message
     */
    public void log(String message, Level level) {
        write(message, level);
    }

    /**
     * Writes the specified message and exception to the servlet log file,
     * usually an event log, if the logger is set to a verbosity level equal
     * to or higher than the specified value for this message.
     *
     * @param message A <code>String</code> that describes the error or
     *  exception
     * @param throwable The <code>Throwable</code> error or exception
     * @param level Verbosity {@link Level} of this message
     */
    public void log(String message, Throwable throwable, Level level) {
        write(message, throwable, level);
    }

    protected void write(String msg, Throwable throwable, Level level) {
        CharArrayWriter buf = new CharArrayWriter();
        PrintWriter writer = new PrintWriter(buf);
        writer.println(msg);
        throwable.printStackTrace(writer);
        Throwable rootCause = null;
        if (throwable instanceof LifecycleException)
            rootCause = throwable.getCause();
        else if (throwable instanceof ServletException)
            rootCause = ((ServletException) throwable).getRootCause();
        if (rootCause != null) {
            writer.println("----- Root Cause -----");
            rootCause.printStackTrace(writer);
        }
        write(buf.toString(), level);
    }

    /**
     * Logs the given message at the given verbosity level.
     */
    protected abstract void write(String msg, Level level);

}
