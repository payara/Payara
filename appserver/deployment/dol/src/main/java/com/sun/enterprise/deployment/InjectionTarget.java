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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.util.TypeUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * This class holds information about an injection target like the class name
 * of the injected target, and field/method information used for injection.
 *
 * @author Jerome Dochez
 */
public class InjectionTarget implements Serializable {
    
    private String className=null;
    private String targetName=null;
    private String fieldName=null;
    private String methodName=null;
    private MetadataSource metadataSource = MetadataSource.XML;
    
    // runtime info, not persisted
    private transient Field field=null;
    private transient Method method=null;
    
    /**
     * Returns true if the field can be injected into
     * @return 
     */
    public boolean isFieldInjectable() {
        return fieldName!=null;
    }

    /**
     * Returns true if method can be injected into
     * @return 
     */
    public boolean isMethodInjectable() {
        return methodName!=null;
    }
    
    /**
     * Gets the class name that is being injected into
     * @return 
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Sets the class name that is being injected into
     * @param className 
     */
    public void setClassName(String className) {
        this.className = className;
    }
    
   /**
     * This is the form used by the .xml injection-group elements to
     * represent the target of injection.   It either represents the
     * javabeans property name of the injection method or the name
     * of the injected field.  This value is set on the descriptor
     * during .xml processing and converted into the appropriate 
     * field/method name during validation.
     * @return 
     */
    public String getTargetName() {
        return targetName;
    }
    
    /**
     * Sets the name of that is being injected into
     * @see #setFieldName(String) 
     * @see #setMethodName(String) 
     * @param targetName 
     */
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    /**
     * Gets the name of the field that is being injected into
     * @return 
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Sets the name of the field that is being injected into
     * @param fieldName 
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
        this.targetName = fieldName;
    }

    /*
     * runtime cached information for faster lookup
     */
    /**
     * Gets the field that is being injected into
     * @return 
     */
    public Field getField() {
        return field;
    }
    
    /**
     * Sets the field that is being injected into
     * @param field 
     */
    public void setField(Field field) {
        this.field = field;
    }

    /** 
     * Inject method name is the actual java method name of the setter method,
     * not the bean property name.  E.g., for @Resource void setFoo(Bar b)
     * it would be "setFoo", not the property name "foo".
     * @return 
     */
    public String getMethodName() {
        return methodName;
    }
    public void setMethodName(String methodName) {
        this.methodName = methodName;
        // Method name follows java beans setter syntax
        this.targetName = TypeUtil.setterMethodToPropertyName(methodName);
    }

    // runtime cached information
    public Method getMethod() {
        return method;
    }
    public void setMethod(Method method) {
        this.method = method;
    }

    public MetadataSource getMetadataSource() {
        return metadataSource;
    }

    /**
     * Sets where the information about the injection is coming from
     * @param metadataSource XML, ANNOTATION or PROGRAMMATIC
     */
    public void setMetadataSource(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InjectionTarget)) {
            return false;
        } else {
            // Note that from xml, one only have className and targetName.
            // From annotation processing, one may define methodName,
            // fieldName and a different metadataSource.
            // Since an applicient container also does annotation processing
            // itself, one would like to avoid duplication from xml and
            // annotation processing.
            // So, one will only check className and targetName here.

            InjectionTarget injTarget = (InjectionTarget)o;
            return equals(className, injTarget.className) &&
                   equals(targetName, injTarget.targetName);
        }
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37*result + (className == null ? 0 : className.hashCode());
        result = 37*result + (targetName == null ? 0: targetName.hashCode());
        result = 37*result + (fieldName == null ? 0: fieldName.hashCode());
        result = 37*result + (methodName == null ? 0: methodName.hashCode());
        return result;
    }


    private boolean equals(String s1, String s2) {
        return (s1 != null && s1.equals(s2) || s1 == null && s2 == null);
    }
}
