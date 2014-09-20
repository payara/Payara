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
 * CMPBeanHelper.java
 *
 * Created on May 28, 2003
 */
package com.sun.jdo.spi.persistence.support.ejb.cmp;

import java.util.Collection;
import java.util.ResourceBundle;

import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBException;
import javax.ejb.ObjectNotFoundException;

import com.sun.jdo.api.persistence.support.JDOHelper;
import com.sun.jdo.api.persistence.support.JDOException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.PersistenceCapable;
import com.sun.jdo.api.persistence.support.PersistenceManager;

import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

/** Provides static helper methods for CMP bean implementation to simplify
 * the generated code.
 */
public class CMPBeanHelper {

    /** I18N message handlers */
    private final static ResourceBundle cmpMessages = I18NHelper.loadBundle(
        "com.sun.jdo.spi.persistence.support.ejb.ejbc.Bundle", // NOI18N
        CMPBeanHelper.class.getClassLoader());

    /** 
     * The lifecycle logger used to log messages from ejbCreate(), ejbRemove()
     * and other lifecycle methods.
     */
    private static Logger cmpLifecycleLogger = LogHelperEntityLifecycle.getLogger();

    /** 
     * The finder logger used to log messages from ejbFindXXX and/or ejbSelectXXX
     * methods.
     */
    private static Logger cmpFinderLogger    = LogHelperEntityFinder.getLogger();

    /** 
     * The internal logger used to log messages from setters and getter and other
     * generated methods.
     */
    private static Logger cmpInternalLogger  = LogHelperEntityInternal.getLogger();

    /**
     * Called from a CMP bean to log JDOException message 
     * with the LifecycleLogger. 
     *
     * @param key the key for the corresponding Bundle.
     * @param beanName the name of the calling bean.
     * @param ex the JDOException.
     */
    public static void logJDOExceptionWithLifecycleLogger(
            String key, String beanName, JDOException ex) {

        cmpLifecycleLogger.log(Logger.WARNING, 
                I18NHelper.getMessage(cmpMessages, key, 
                        beanName, findCallingMethodName()), ex);
    }

    /**
     * Called from a CMP bean to log JDOException message 
     * with the LifecycleLogger. 
     *
     * @param key the key for the corresponding Bundle.
     * @param beanName the name of the calling bean.
     * @param paramList the list of the concatenated parameters.
     * @param ex the JDOException.
     */
    public static void logJDOExceptionWithLifecycleLogger(
            String key, String beanName, String paramList, 
            JDOException ex) {

        cmpLifecycleLogger.log(Logger.WARNING, 
                I18NHelper.getMessage(cmpMessages, key, 
                        beanName, findCallingMethodName(), paramList), 
                ex);
    }

    /**
     * Called from a CMP bean to log JDOException message thrown
     * from a any getter or setter method, with the InternalLogger. 
     *
     * @param beanName the name of the calling bean.
     * @param ex the JDOException.
     */
    public static void logJDOExceptionWithInternalLogger(
            String beanName, JDOException ex) {

        cmpInternalLogger.log(Logger.WARNING, 
                I18NHelper.getMessage(cmpMessages, 
                "GEN.generic_method_exception", // NOI18N
                beanName, findCallingMethodName()), ex);
    }

    /**
     * Called from a CMP bean to log JDOException message thrown
     * from a any finder or selector method, with the FinderLogger.
     *
     * @param beanName the name of the calling bean.
     * @param params the Object[] of the parameter values for the 
     * finder or selector method.
     * @param ex the JDOException.
     */
    public static void logJDOExceptionWithFinderLogger(
            String beanName, Object[] params, JDOException ex) {

        String msg = null;
        if (params != null) {
            msg = I18NHelper.getMessage(cmpMessages,
                    "GEN.ejbSSReturnBody_exception", beanName, // NOI18N
                    findCallingMethodName(), 
                    java.util.Arrays.asList(params).toString());
        } else {
            msg = I18NHelper.getMessage(cmpMessages,
                    "GEN.ejbSSReturnBody_exception_woparams", beanName, // NOI18N
                    findCallingMethodName());
        }
        cmpFinderLogger.log(Logger.WARNING, msg, ex);
    }

    /**
     * Called from a CMP bean to log JDOException message thrown
     * from a any finder or selector method, with the FinderLogger.
     *
     * @param level the logging level as int.
     * @param beanName the name of the calling bean.
     * @param ex the Exception.
     */
    public static void logFinderException(int level, String beanName, 
            Exception ex) {

        if (cmpFinderLogger.isLoggable(level)) {
            cmpFinderLogger.log(level,
                    I18NHelper.getMessage(cmpMessages, 
                            "GEN.generic_method_exception", // NOI18N
                            beanName, findCallingMethodName()), ex);
        }
    }

    /**
     * Called from a CMP bean to log JDOException message thrown
     * from a PK setter method, with the InternalLogger. 
     * Returns generated message to the caller to be used for a
     * IllegalStateException.
     *
     * @param beanName the name of the calling bean.
     * @param ex the JDOException.
     * @return logged message as String.
     */
    public static String logJDOExceptionFromPKSetter(
            String beanName, JDOException ex) {

        String msg = I18NHelper.getMessage(cmpMessages, "EXC_PKUpdate", // NOI18N
            beanName, findCallingMethodName()); 
        if (cmpInternalLogger.isLoggable(Logger.FINE)) {
            cmpInternalLogger.log(Logger.FINE, msg, ex);
        }

        return msg;
    }

    /**
     * Called from a CMP bean to verify that the PersistenceCapable
     * instance is already persistent. Throws IllegalStateException
     * otherwise.
     * 
     * @param pc the PersistenceCapable instance to be checked.
     * @param beanName the name of the caller bean.
     * @throws IllegalStateException if the instance is not persistent.
     */
    public static void assertPersistent(PersistenceCapable pc, String beanName) {
        if (!JDOHelper.isPersistent(pc)) {
            String msg = I18NHelper.getMessage(cmpMessages, 
                "GEN.cmrgettersetter_exception", beanName, findCallingMethodName()); // NOI18N

            cmpInternalLogger.log(Logger.SEVERE, msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Called from a CMP bean to verify that the argument for
     * a Collection set method is not null. Throws IllegalArgumentException 
     * if the argument is null.
     *
     * @param c the Collection to check.
     * @param beanName the name of the caller bean.
     * @throws IllegalArgumentException if the argument is null.
     */
    public static void assertCollectionNotNull(Collection c, String beanName) {
        if (c == null) {
            String msg = I18NHelper.getMessage(cmpMessages,
                "GEN.cmrsettercol_nullexception", beanName, findCallingMethodName()); // NOI18N

            cmpInternalLogger.log(Logger.SEVERE, msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Called from a CMP bean to verify that the PersistenceManager
     * is not null. Throws IllegalStateException if the argument is null.
     *
     * @param pm the PersistenceManager to check.
     * @param bean the calling bean instance.
     * @throws IllegalStateException if the PersistenceManager is null.
     */
    public static void assertPersistenceManagerNotNull(PersistenceManager pm, 
        Object bean) {
        if (pm == null) {
            String msg = I18NHelper.getMessage(cmpMessages,
                "JDO.beannotloaded_exception", bean); // NOI18N

            cmpInternalLogger.log(Logger.SEVERE, msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Called from a CMP bean to verify that the PersistenceManager
     * is null. Throws IllegalStateException if the argument is not null.
     *
     * @param pm the PersistenceManager to check.
     * @param bean the calling bean instance.
     * @throws IllegalStateException if the PersistenceManager is not null.
     */
    public static void assertPersistenceManagerIsNull(PersistenceManager pm, 
        Object bean, StringBuffer buf) {
        if (pm != null) {
            String msg = I18NHelper.getMessage(cmpMessages,
                "JDO.beaninuse_exception", bean); // NOI18N

            // Excption to use only short message
            IllegalStateException e = new IllegalStateException(msg);

            if (buf != null && buf.length() > 0) {
                msg = (new StringBuffer(msg)).append(" ...Last Instance Usage: ").  // NOI18N
                        append(buf).toString(); 
            }
            cmpInternalLogger.log(Logger.SEVERE, msg);
            throw e;
        }
    }

    /**
     * Called from a 1.1 CMP bean to verify that the bean method is not called
     * in a container transaction. Throws IllegalStateException otherwise.
     *
     * @param bean the calling bean instance.
     * @throws IllegalStateException if the bean method is called in a container transaction.
     */
    public static void assertNotContainerTransaction(Object bean) {
        if (EJBHelper.getTransaction() != null) {
            String msg = I18NHelper.getMessage(cmpMessages,
                "JDO.containertransaction_exception", bean); // NOI18N

            cmpInternalLogger.log(Logger.SEVERE, msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Called from a CMP bean to process JDODuplicateObjectIdException.
     * Logs the message and throws DuplicateKeyException.
     *
     * @param beanName the name of the calling bean.
     * @param paramList the list of the concatenated parameters.
     * @param ex the JDOException.
     * @throws DuplicateKeyException.
     */
    public static void handleJDODuplicateObjectIdAsDuplicateKeyException(
        String beanName, String paramList, JDOException ex) 
        throws DuplicateKeyException {

        String msg = I18NHelper.getMessage(cmpMessages,
            "GEN.ejbcreate_exception_dup", beanName, // NOI18N
            findCallingMethodName(), paramList);

        cmpLifecycleLogger.log(Logger.FINER, msg, ex);
        throw new DuplicateKeyException(msg);
    }

    /**
     * Called from a CMP bean to process JDODuplicateObjectIdException.
     * Logs the message and throws EJBException.
     *
     * @param beanName the name of the calling bean.
     * @param paramList the list of the concatenated parameters.
     * @param ex the JDOException.
     * @throws EJBException.
     */
    public static void handleJDODuplicateObjectIdAsEJBException(
        String beanName, String paramList, JDOException ex) {

        String msg = I18NHelper.getMessage(cmpMessages,
            "GEN.ejbcreate_exception_dup", beanName, // NOI18N
            findCallingMethodName(), paramList);

        cmpLifecycleLogger.log(Logger.FINER, msg, ex);
        throw new EJBException(msg);
    }

   /**
     * Called from a CMP bean to process JDOObjectNotFoundException.
     * Logs the message and throws ObjectNotFoundException
     *
     * @param primaryKey the PrimaryKey instance.
     * @param beanName the name of the calling bean.
     * @param ex the JDOException.
     * @throws ObjectNotFoundException.
     */
    public static void handleJDOObjectNotFoundException(
        Object primaryKey, String beanName, JDOException ex) 
        throws ObjectNotFoundException {

        String msg = I18NHelper.getMessage(cmpMessages,
            "GEN.findbypk_exception_notfound", beanName, // NOI18N
            primaryKey.toString()); 

        cmpLifecycleLogger.log(Logger.FINER, msg, ex);
        throw new ObjectNotFoundException(msg);
    }

   /**
     * Throws EJBException on attempted updates to the
     * calling bean.
     * @param beanName the name of the calling bean.
     * @throws EJBException.
     */
    public static void handleUpdateNotAllowedException(
        String beanName) {
        String msg = I18NHelper.getMessage(cmpMessages,
            "GEN.update_not_allowed", beanName, // NOI18N
            findCallingMethodName());

        cmpLifecycleLogger.log(Logger.SEVERE, msg);
        throw new EJBException(msg);
    }
 
   /**   
     * Throws EJBException on failed clone of persistence state
     * in read-only beans.
     *   
     * @param primaryKey the PrimaryKey instance.
     * @param beanName the name of the calling bean.
     * @param ex the Exception.
     * @throws EJBException.
     */  
    public static void handleCloneException(
        Object primaryKey, String beanName, Exception ex) {
 
        String msg = I18NHelper.getMessage(cmpMessages,
            "GEN.clone_exception", beanName, // NOI18N
            primaryKey.toString());
 
        cmpLifecycleLogger.log(Logger.SEVERE, msg, ex);
        throw new EJBException(msg);
    }

    /**
     * Calculates the method name of the calling method.
     *
     * @return method name as String.
     */
    private static String findCallingMethodName() {
        StackTraceElement[] ste = (new Throwable()).getStackTrace();
        return ste[2].getMethodName();
    }
}
