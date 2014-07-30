/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import org.glassfish.web.loader.WebappClassLoader;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.bootstrap.api.Singleton;
import org.glassfish.javaee.full.deployment.EarLibClassLoader;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ClassLoaderHierarchy;

import java.util.Map;
import java.util.Hashtable;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Singleton provider that uses Application ClassLoader to differentiate
 * between applications. It is different from
 * {@link org.jboss.weld.bootstrap.api.helpers.TCCLSingletonProvider}.
 * We can't use TCCLSingletonProvider because thread's context class loader can
 * be different for different modules of a single application (ear).
 * To support Application Scoped beans, Weld needs to be bootstrapped
 * per application as opposed to per module. We rely on the fact that
 * all these module class loaders have a common parent which is per application.
 * We use that parent ApplicationClassLoader to identify the singleton scope.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ACLSingletonProvider extends SingletonProvider
{
    /*
     * See https://glassfish.dev.java.net/issues/show_bug.cgi?id=10192
     * for more details about this class.
     *
     * IMPLEMENTATION NOTE:
     * This class assumes a certain delegation hierarchy of application
     * class loaders. So, deployment team should be aware of this class
     * and change it if application class loader hierarchy changes.
     */

    public <T> ACLSingleton<T> create(Class<? extends T> expectedType) {
        return new ACLSingleton<T>();
    }

    private static class ACLSingleton<T> implements Singleton<T> {

      // use Hashtable for concurrent access
      private final Map<ClassLoader, T> store = new Hashtable<ClassLoader, T>();
      private ClassLoader ccl = Globals.get(ClassLoaderHierarchy.class).getCommonClassLoader();

      // Can't assume bootstrap loader as null. That's more of a convention.
      // I think either android or IBM JVM does not use null for bootstap loader
      private static ClassLoader bootstrapCL;

      static {
        SecurityManager sm = System.getSecurityManager();
        bootstrapCL = (sm != null) ?
          AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run()
            {
              return Object.class.getClassLoader();
            }
          }) : Object.class.getClassLoader();
      }

      @Override
      public T get( String id )
      {
        ClassLoader acl = getClassLoader();
        T instance = store.get(acl);
        if (instance == null)
        {
          throw new IllegalStateException("Singleton not set for " + acl);
        }
        return instance;
      }

      /**
       * This is the most significant method of this class. This is what
       * distingushes it from TCCLSIngleton. It tries to obtain a class loader
       * that's common to all modules of an application (ear). Since it runs
       * in the context of Java EE, it can assume that Thread's context
       * class loader is always set as application class loader. In GlassFish,
       * the class loader can vary for each module of an Ear. Thread's
       * context class loader is set depending on which module is handling
       * the request. But, fortunately all those embedded module class loaders
       * have a common parent in their delegation chain. That parent
       * is of type EarLibClassLoader. So, this code walks up the delegation
       * chain until it hits either a EarLibClassLoader type of parent or
       * bootstrapClassLoader. If former is the case, it returns that
       * instance of EarLibClassLoader. If latter is the case, it assumes
       * that this is a standalone module and hence it returns the thread's
       * context class loader.
       *
       * @return a class loader that's common to all modules of a Java EE app
       */
      private ClassLoader getClassLoader()
      {
        SecurityManager sm = System.getSecurityManager();
        final ClassLoader tccl = (sm != null) ?
          AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
          {
            public ClassLoader run() {
              return Thread.currentThread().getContextClassLoader();
            }
          }) : Thread.currentThread().getContextClassLoader();
        if (tccl == null) {
          throw new RuntimeException("Thread's context class loader is null");
        }

        ClassLoader cl = tccl;
        ClassLoader appClassLoader = tccl;

        // most of the time, class loader of application (whether it is a
        // standalone module or an ear) has common class loader in their
        // delegation chain. So, we can break the loop early for them.
        // There are exceptions like hybrid app to this rule.
        // So, we have to walk upto bootstrapCL in worst case.
        while (cl != ccl && cl != bootstrapCL) {
          if (cl instanceof EarLibClassLoader) {
//                    System.out.println("ACLSingletonProvider.getClassLoader():\n" +
//                            "Application Class Loader = [ " + cl + "],\n" +
//                            "Thread Context Class Loader = [" + tccl + "]");
            return cl;
          } else {
            if (cl instanceof WebappClassLoader) {
              // we do this because it's possible for an app to change the thread's context class loader
              appClassLoader = cl;
            }
          }
          cl = getParent(cl);
        }
        return appClassLoader;
      }

      private ClassLoader getParent(final ClassLoader cl)
      {
        SecurityManager sm = System.getSecurityManager();
        return sm != null ?
          AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run()
            {
              return cl.getParent();
            }
          }) : cl.getParent();
      }

      @Override
      public boolean isSet(String id) {
        return store.containsKey(getClassLoader());
      }

      @Override
      public void set(String id, T object) {
        store.put(getClassLoader(), object);
      }

      @Override
      public void clear(String id) {
        store.remove(getClassLoader());
      }
    }
}
