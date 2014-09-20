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

package com.sun.enterprise.connectors.util;

import com.sun.enterprise.connectors.ConnectorRuntime;
import org.jvnet.hk2.annotations.Service;

import javax.validation.*;
import javax.validation.metadata.BeanDescriptor;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.logging.LogDomains;
import com.sun.enterprise.connectors.ConnectorRegistry;

@Service
public class ConnectorJavaBeanValidator {

    private final static Logger _logger = LogDomains.getLogger(
            ConnectorJavaBeanValidator.class, LogDomains.RSR_LOGGER);

    public void validateJavaBean(Object bean, String rarName) {
        if (bean != null) {
            Validator validator = getBeanValidator(rarName);
            if (validator != null) {
                BeanDescriptor bd =
                        validator.getConstraintsForClass(bean.getClass());
                bd.getConstraintDescriptors();

                Class array[] = new Class[]{};
                Set constraintViolations = validator.validate(bean, array);


                if (constraintViolations != null && constraintViolations.size() > 0) {
                    ConstraintViolationException cve = new ConstraintViolationException(constraintViolations);
                    StringBuffer msg = new StringBuffer();    

                    Iterator it = constraintViolations.iterator();
                    while (it.hasNext()) {
                        ConstraintViolation cv = (ConstraintViolation) it.next();
                        msg.append("\n Bean Class : ").append(cv.getRootBeanClass());
                        msg.append("\n Bean : ").append(cv.getRootBean());
                        msg.append("\n Property path : " ).append(cv.getPropertyPath());
                        msg.append("\n Violation Message : " ).append(cv.getMessage());
                    }

                    Object[] args = new Object[]{bean.getClass(), rarName, msg.toString()};
                    _logger.log(Level.SEVERE, "validation.constraints.violation",args);
                    throw cve;
                }
            } else {
                if(_logger.isLoggable(Level.FINEST)){
                   _logger.log(Level.FINEST, "No Bean Validator is available for RAR [ " + rarName + " ]");
                }
            }
        }
    }

    private Validator getBeanValidator(String rarName) {
        Validator beanValidator = ConnectorRegistry.getInstance().getBeanValidator(rarName);
        ValidatorFactory validatorFactory = null;
        // this is needed in case of appclient/standalone client
        // and system-resource-adapters in server.
        if (beanValidator == null) {
            ClassLoader contextCL = null;
            try{
                contextCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(ConnectorRuntime.getRuntime().getConnectorClassLoader());
                validatorFactory = Validation.byDefaultProvider().configure().buildValidatorFactory();
                beanValidator = validatorFactory.getValidator();
                ConnectorRegistry.getInstance().addBeanValidator(rarName, beanValidator);
            }finally{
                Thread.currentThread().setContextClassLoader(contextCL);
            }
        }
        return beanValidator;
    }
}
