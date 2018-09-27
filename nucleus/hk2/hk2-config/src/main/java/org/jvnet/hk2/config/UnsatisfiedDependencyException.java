/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.jvnet.hk2.config;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.util.Locale;

/**
 * Exception thrown by the injection manager when a dependency is not satisfied when
 * performing injection.
 *
 * @author Jerome Dochez
 */
@SuppressWarnings("serial")
public class UnsatisfiedDependencyException extends ConfigurationException {

    final AnnotatedElement member;

    /**
     * @deprecated
     */
    public UnsatisfiedDependencyException(AnnotatedElement target) {
        this(target, null, null);
    }
    
    public UnsatisfiedDependencyException(AnnotatedElement target, Annotation inject) {
      this(target, inject, null);
  }
    
    public UnsatisfiedDependencyException(AnnotatedElement target, Annotation inject, Throwable cause) {
        super(injection_failed_msg(target, inject, cause), cause);
        this.member = target;
    }

    public UnsatisfiedDependencyException(Type target, Class<?> targetClass, Annotation inject, Throwable cause) {
        super(injection_failed_msg(target, inject, null), inject, cause);
        this.member = targetClass;
    }

    static String injection_failed_msg(Object t, Annotation inject, Throwable cause) {
      String name = null;
      //name = (null == name || name.isEmpty()) ? null : name;
      String msg;
      if (Field.class.isInstance(t)) {
        Field target = Field.class.cast(t);
        msg = "injection failed on " + target.getDeclaringClass().getName() + "." + 
            target.getName() + " with " + target.getGenericType() + "";
      } else {
        msg = "injection failed on " + t + "";
      }
      return msg;
    }

    public boolean isField() {
        return member instanceof Field;
    }

    public boolean isMethod() {
        return member instanceof Method;
    }

    public String getUnsatisfiedName() {
        String name = (member instanceof Member) ? ((Member)member).getName() : member.toString();
        if (isMethod()) {
            return name.substring(3).toLowerCase(Locale.ENGLISH);
        }
        return name;
    }

    public AnnotatedElement getUnsatisfiedElement() {
        try {
            return AnnotatedElement.class.cast(member);
        } catch(ClassCastException e) {
            return null;
        }
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        AnnotatedElement annotated = getUnsatisfiedElement();
        if (annotated!=null) {
            return annotated.getAnnotation(annotationType);
        } else {
            return null;
        }
    }
}
