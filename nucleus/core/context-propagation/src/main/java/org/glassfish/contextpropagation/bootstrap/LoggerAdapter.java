/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.contextpropagation.bootstrap;


/**
 * Implementor should forward the calls to the appropriate debug logger.
 */
public interface LoggerAdapter {
  public boolean isLoggable(Level level);    
  public void log(Level level, MessageID messageID, Object... args);
  public void log(Level level, Throwable t, MessageID messageID, Object... args);

  enum Level {
    ERROR, WARN, DEBUG; // From most critical to least critical
  }

  enum MessageID {
    // Non-Debug messages must be internationalized
    // Error Messages:
    ERROR_NO_WORK_CONTEXT_FACTORY("Could not find a factory to create the work context named %1 of class %2."), 
    ERROR_UNABLE_TO_INSTANTIATE_CONTEXT_FROM_THE_WIRE("Unable to instatiate a context from the wire."), 
    ERROR_CLASSNOTFOUND("Could not instantiate the context associated to key, %1, because its definition is not available to the class loader. "), 
    ERROR_IOEXCEPTION_WHILE_READING_ENTRY("Could not read the context associated to key, %1, due to an IOException."), 
    ERROR_IOEXCEPTION_WHILE_READING_KEY("Could not read the key following context with key, %1, due to an IOException"),
    ERROR_NO_MORE_ITEMS("There are no more items to read according to the catalog."), 
    UNEXPECTED_LACK_OF_CREDENTIALS("Unexpected Error: Internal code lacks the necessary credentials to perform a restricted ContextMap operation."),
    UNEXPECTED_SERIALIZABLE_WORKCONTEXT_VERSION("Read a SerializableWorkContextVersion, %2 that is not the expected version. %1. This may cause subsequent problems."), 
    // Warning Messages:
    WARN_FACTORY_ALREADY_REGISTERED_FOR_NAME("The following ContextViewFactory is already registered for name, %1: %2. It will be replaced by %3."),
    WARN_FACTORY_ALREADY_REGISTERED_FOR_CLASS("The following ContextViewFactory is already registered for class name, %1: %2. It will be replaced by %3."),
    WARN_FACTORY_ALREADY_REGISTERED_FOR_PREFIX("The following ContextViewFactory is already registered for prefix, %1: %2. It will be replaced by %3."), 
    // Debug Messages:"
    PUT("put(%1, %2) -> %3"),
    OPERATION("%1(%2) -> %3"),
    WLS_UNSUPPORTED_TYPE("Weblogic Server does not support Work Contexts of type, %2. Wrapping work context %1: %3"), 
    READING_OPAQUE_TYPE("Unable to read work context named, %1. It will be stored as an OPAQUE type."), 
    USING_WIRE_ADAPTER("%1 adapter %2."),  // ex.: Writing to adapter myAdapter.
    WRITING_KEY("Writing key: %1"),
    READ_CONTEXT_TYPE("Read context type: %1"), 
    READ_KEY("Read key: <%1>."), 
    INTERCEPTING_CLASS("While %1, intercepting class %2 and replacing it with class %3."), 
    READ_CATALOG("Read catalog: %1"), 
    READ_VALUE("Read value: %1"), 
    READ_PROP_MODES("Read propagation mode: %1"), 
    READ_CATALOG_VERSION("Read catalog version: %1"), 
    WRITE_ENTRY("Wrote Entry #%1, %2: %3"), 
    WRITE_CATALOG("Wrote catalog: %1"), 
    PROPAGATION_STARTED("%1 propagation started."),
    PROPAGATION_COMPLETED("%1 propagation completed"), // Consider including the number of bytes processed . %2 bytes processed.
    WRITE_PROPAGATION_MODES("Wrote propagation modes: %1"), 
    WRITE_FOOTER("Wrote footer"), 
    ATTEMPT_TO_SKIP_TO_NEXT_ITEM("Attempting to skip to item %1 from byte %2 to byte %3 excluded."), 
    CLEARED_ENTRIES("The following entries were cleared to prepare for an incoming response: %1."), 
    RESTORING_CONTEXTS("Restoring contexts. The following contexts are under consideration: %1"), 
    CREATING_NEW_CONTEXT_MAP("Created a new context map: %1"), 
    READING_SERIALIZED("Reading %1 bytes of serialized data: %2"), 
    WRITING_SERIALIZED("Writing %1 bytes of serialized data: %2"), 
    NO_CATALOG("Could not recover from error because this context map does not have a catalog.");

    public String defaultMessage;
    MessageID(String defaultMessage) {
      this.defaultMessage = defaultMessage;
    }
  }
}
