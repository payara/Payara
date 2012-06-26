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
package org.glassfish.contextpropagation.internal;

// TODO LATER(After initial checkin) this class will need minor adjustments in most of its accessors once we merge with Glassfish

import org.glassfish.contextpropagation.bootstrap.ContextAccessController;
import org.glassfish.contextpropagation.bootstrap.DependencyProvider;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter;
import org.glassfish.contextpropagation.bootstrap.ThreadLocalAccessor;
import org.glassfish.contextpropagation.internal.AccessControlledMap.ContextAccessLevel;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.glassfish.contextpropagation.wireadapters.wls.WLSWireAdapter;
import org.jvnet.hk2.annotations.Service;

//@Singleton
//@Named("myService")
//@Default from javax.enterprise.inject may be appropriate
/**
 * Provides the context-propagation dependencies in Glassfish. Other products
 * should consider replacing this implementation with their own.
 */
@Service
public class DependencyProviderImpl implements DependencyProvider {
  
  private boolean isClosedSource;

  public DependencyProviderImpl() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      isClosedSource = cl.loadClass("weblogic.workarea.WorkContextMap") != null;
    } catch (ClassNotFoundException e) {
      isClosedSource = false;
    }
  }

  @Override
  public LoggerAdapter getLoggerAdapter() {
    return new LoggerAdapter() {
      @Override
      public boolean isLoggable(Level level) {
        return true;
      }

      @Override
      public void log(Level level, MessageID messageID, Object... args) {
        System.out.println(format(messageID.defaultMessage, args));

      }

      private String format(String defaultMessage, Object... args) {
        String formatString = defaultMessage.replaceAll("%([0-9]*)", "%$1\\$s"); // $1 refers to the group %1 is equivalent to %1$s
        return String.format(formatString, args);
      }

      @Override
      public void log(Level level, Throwable t, MessageID messageID, Object... args) {
        log(level, messageID, args);
        t.printStackTrace();
      }
    };
  }

  @Override
  public ThreadLocalAccessor getThreadLocalAccessor() {
    return new ThreadLocalAccessor() {
      private ThreadLocal<AccessControlledMap> mapThreadLocal = new ThreadLocal<AccessControlledMap>();

      @Override
      public void set(AccessControlledMap contextMap) {
        mapThreadLocal.set(contextMap);    
      }

      @Override
      public AccessControlledMap get() {
        return mapThreadLocal.get();
      }
    };
  }

  @Override
  public ContextAccessController getContextAccessController() {
    return new ContextAccessController() {
      @Override
      public boolean isAccessAllowed(String key, AccessControlledMap.ContextAccessLevel type) {
        if (type == ContextAccessLevel.READ && isEveryoneAllowedToRead(key)) {
          return true; // First do a quick check for read access
        }
        return true;
      }

      @Override
      public boolean isEveryoneAllowedToRead(String key) {
        return false;
      }
    };
  }

  @Override
  public WireAdapter getDefaultWireAdapter() {
    return isClosedSource ? new WLSWireAdapter() : new DefaultWireAdapter();
  }

  @Override
  public String getGuid() {
    return "guid";
  }
}
