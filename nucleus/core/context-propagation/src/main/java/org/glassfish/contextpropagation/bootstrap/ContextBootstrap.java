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


import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.Level;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.internal.DependencyProviderImpl;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;

/**
 * The classes in this package were designed to facilitate the integration of 
 * the context propagation feature in a server. Through the use of inversion of 
 * control, we were able to abstract all the dependencies needed by context
 * propagation thus making this feature easy to port to another server. In this
 * way, we hope to encourage the use of context propagation on other servers.
 */
public class ContextBootstrap {
  private static LoggerAdapter loggerAdapter;
  private static ThreadLocalAccessor threadLocalAccessor;
  private static ContextAccessController contextAccessController;
  private static boolean isConfigured;
  private static WireAdapter wireAdapter;
  private static String guid;
  public static boolean IS_DEBUG;
  
  private static DependencyProvider dependencyProvider;  
  static {
    if (dependencyProvider == null) {
      dependencyProvider = new DependencyProviderImpl(); // The service should have been injected, But we are not taking any chances.
    }
    if (dependencyProvider != null) {
      configure(dependencyProvider.getLoggerAdapter(),
          dependencyProvider.getDefaultWireAdapter(),
          dependencyProvider.getThreadLocalAccessor(),
          dependencyProvider.getContextAccessController(),
          dependencyProvider.getGuid());
    }
  }
  
  /**
   * This function must be called by the server prior to using context propagation.
   * @param loggerAdapter An adaptor to the logger that is appropriate for 
   * context propagation messages.
   * @param tla An adaptor to the thread management system that allows safe 
   * storage of the ContextMap on the current thread.
   * @param contextAccessController An adaptor to the security manager that 
   * is used to determine access to particular work contexts by the user
   * associated to the current thread.
   * @param aGuid a unique identifier for this process that is suitable for
   * transmission over the wire.
   */
  public static void configure(LoggerAdapter aLoggerAdapter, 
      WireAdapter aWireAdapter, ThreadLocalAccessor aThreadLocalAccessor,
      ContextAccessController aContextAccessController, String aGuid) { 
    if (isConfigured) {
      throw new IllegalStateException("WorkArea is already configured");
    }
    if (aLoggerAdapter == null || aWireAdapter == null ||
        aThreadLocalAccessor == null || aContextAccessController == null ) {
      throw new IllegalArgumentException(
          "logger and wire adapters, threadLocalAccessor and " +
          "contextAccessController must be specified.");
    }
    loggerAdapter = aLoggerAdapter;
    wireAdapter = aWireAdapter;
    threadLocalAccessor = aThreadLocalAccessor;
    contextAccessController = aContextAccessController;      
    guid = aGuid;
    IS_DEBUG = loggerAdapter.isLoggable(Level.DEBUG);

    isConfigured = true;
}
  
  /**
   * @return The bootstrapped WireAdapter
   */
  public static WireAdapter getWireAdapter() {
    checkIfConfigured();
    return wireAdapter; 
  }

  private static void checkIfConfigured() {
    if (!isConfigured) {
      throw new IllegalStateException("Context propagation is not yet configured.");
    }    
  }
  
  /**
   * @return The bootstrapped LoggerAdapter
   */
  public static LoggerAdapter getLoggerAdapter() {
    checkIfConfigured();
    return loggerAdapter;
  }
  
  /**
   * @param messageID a MessageID
   * @param args The objects to in the message
   */
  public static void debug(MessageID messageID, Object... args) {
    if (loggerAdapter.isLoggable(Level.DEBUG)) {
      loggerAdapter.log(Level.DEBUG, messageID, args);
    }
  }
  
  /**
   * @param t a Throwable to include in the debug message
   * @param messageID a MessageID
   * @param args The objects to in the message
   */
  public static void debug(Throwable t, MessageID messageID, Object... args) {
    if (loggerAdapter.isLoggable(Level.DEBUG)) {
      loggerAdapter.log(Level.DEBUG, t, messageID, args);
    }
  }

  /**
   * @return The adaptor to access the ContextMap stored on the curren thread
   */
  public static ThreadLocalAccessor getThreadLocalAccessor() {
    checkIfConfigured();
      return threadLocalAccessor; 
  }

  /**
   * @return The adapter that checks acccess permissions.
   */
  public static ContextAccessController getContextAccessController() {
    checkIfConfigured();
    return contextAccessController;
  }
  
  /**
   * @return a String that uniquely identifies this process
   */
  public static String getGuid() {
    checkIfConfigured();
    return guid;
  }


}
