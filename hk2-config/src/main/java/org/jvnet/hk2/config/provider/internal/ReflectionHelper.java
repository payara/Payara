/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config.provider.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * Utilities for transaction config reflection.
 * 
 * @author Jeff Trent
 */
/*public*/ class ReflectionHelper {

  static <T extends Annotation> T annotation(Object obj, Class<T> annotation) {
    if (null == obj) {
      return null;
    }
    
    Class<?> clazz = obj.getClass();
    if (Proxy.isProxyClass(clazz) || clazz.isAnonymousClass()) {
      for (Class<?> iface : clazz.getInterfaces()) {
        T t = iface.getAnnotation(annotation);
        if (null != t) {
          return t;
        }
      }
      
      if (clazz.isAnonymousClass()) {
        clazz = clazz.getSuperclass();
      }
    }
    
    return clazz.getAnnotation(annotation);
  }

  @SuppressWarnings("unchecked")
  static void annotatedWith(Set<Class<?>> contracts, Object obj, Class annotation) {
    if (null != obj) {
      Class<?> clazz = obj.getClass();
      
      while (Object.class != clazz) {
        if (!clazz.isAnonymousClass()) {
          Object t = clazz.getAnnotation(annotation);
          if (null != t) {
            contracts.add(clazz);
          } else {
            annotatedWith(contracts, annotation, clazz);
          }
          
          for (Class<?> iface : clazz.getInterfaces()) {
            t = iface.getAnnotation(annotation);
            if (null != t) {
              contracts.add(iface);
            } else {
              annotatedWith(contracts, annotation, iface);
            }
          }
        }
        
        clazz = clazz.getSuperclass();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void annotatedWith(Set<Class<?>> contracts, Class annotation, Class<?> clazz) {
    if (!Proxy.isProxyClass(clazz)) {
      Annotation[] annArr = clazz.getAnnotations();
      for (Annotation ann : annArr) {
        Class<?> x = ann.annotationType();
        Object t = x.getAnnotation(annotation);
        if (null != t) {
          contracts.add(clazz);
          return;
        }
      }
    }
  }

  static String nameOf(Object configBean) {
    String name = null;
    
    if (null != configBean) {
      try {
        Method m = configBean.getClass().getMethod("getName", (Class<?>[])null);
        name = String.class.cast(m.invoke(configBean, (Object[])null));
      } catch (Exception e) {
        // swallow
      }
    }
    
    return name;
  }

}
