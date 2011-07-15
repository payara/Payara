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

/*
 * JDOCodeGeneratorHelper.java
 *
 * Created on Aug 28, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.util.ResourceBundle;

import com.sun.jdo.spi.persistence.support.ejb.codegen.GeneratorException;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbBundleDescriptor;

import org.glassfish.persistence.common.I18NHelper;

/*
 * This is the helper class for JDO code generation 
 *
 */
public class JDOCodeGeneratorHelper {

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        JDOCodeGeneratorHelper.class);

    /** Calculate module name from a bundle.
     * @return module name.
     */
    public static String getModuleName(EjbBundleDescriptor bundle) {
        String moduleName = null;
        Application application = bundle.getApplication();
        if (application.isVirtual()) {
            // Stand-alone module is deployed.
            moduleName = application.getRegistrationName();

        } else {
            // Module is deployed as a part of an Application.
            String jarName = bundle.getModuleDescriptor().getArchiveUri();
            int l = jarName.length();

            // Remove ".jar" from the bundle's jar name.
            moduleName = jarName.substring(0, l - 4);

        }

        return moduleName;
    }

    /**
     * Create GeneratorException for this message key.
     * @param key the message key in the bundle.
     * @param bundle the ejb bundle.
     * @return GeneratorException.
     */
    public static GeneratorException createGeneratorException(
            String key, EjbBundleDescriptor bundle) {
        return new GeneratorException(I18NHelper.getMessage(
            messages, key,
            bundle.getApplication().getRegistrationName(),
            getModuleName(bundle)));
    }

    /** 
     * Create GeneratorException for this message key.
     * @param key the message key in the bundle.
     * @param bundle the ejb bundle.
     * @param e the Exception to use for the message.
     * @return GeneratorException.
     */
    public static GeneratorException createGeneratorException(
            String key, EjbBundleDescriptor bundle,  Exception e) {

        return new GeneratorException(I18NHelper.getMessage(
            messages, key,
            bundle.getApplication().getRegistrationName(),
            getModuleName(bundle), 
            e.getMessage()));
    }

    /** 
     * Create GeneratorException for this message key and bean name.
     * @param key the message key in the bundle.
     * @param bundle the ejb bundle.
     * @return GeneratorException.
     */
    public static GeneratorException createGeneratorException(
            String key, String beanName, EjbBundleDescriptor bundle) {

        return new GeneratorException(I18NHelper.getMessage(
            messages, key, beanName,
            bundle.getApplication().getRegistrationName(),
            getModuleName(bundle)));
    }

    /**
     * Create GeneratorException for this message key and bean name.
     * @param key the message key in the bundle.
     * @param beanName the CMP bean name that caused the exception.
     * @param bundle the ejb bundle.
     * @param e the Exception to use for the message.
     * @return GeneratorException.
     */
    public static GeneratorException createGeneratorException(
            String key, String beanName, EjbBundleDescriptor bundle, 
            Exception e) {

        return createGeneratorException(key, beanName, bundle, e.getMessage());
    }

    /**
     * Create GeneratorException for this message key, bean name,
     * and a StringBuffer with validation exceptions.
     * @param key the message key in the bundle.
     * @param beanName the CMP bean name that caused the exception.
     * @param bundle the ejb bundle.
     * @param e the Exception to use for the message.
     * @param buf the StringBuffer with validation exceptions.
     * @return GeneratorException.
     */
    public static GeneratorException createGeneratorException(
            String key, String beanName, EjbBundleDescriptor bundle,  
            Exception e, StringBuffer buf) {

        String msg = (buf == null) ?
                e.getMessage() :
                buf.append(e.getMessage()).append('\n').toString();
        return createGeneratorException(key, beanName, bundle, msg);
    }

    /**
     * Create GeneratorException for this message key and bean name.
     * @param key the message key in the bundle.
     * @param beanName the CMP bean name that caused the exception.
     * @param bundle the ejb bundle.
     * @param msg the message text to append.
     * @return GeneratorException.
     */
    public static GeneratorException createGeneratorException(
            String key, String beanName, EjbBundleDescriptor bundle, 
            String msg) {

        return new GeneratorException(I18NHelper.getMessage(
            messages, key,
            new Object[] {
                beanName, 
                bundle.getApplication().getRegistrationName(),
                getModuleName(bundle),
                msg}
            ));
    }
}
