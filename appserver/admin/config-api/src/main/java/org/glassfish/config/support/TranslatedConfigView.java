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

package org.glassfish.config.support;

import org.jvnet.hk2.config.ConfigView;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
//import org.glassfish.security.common.RelativePathResolver;
import org.glassfish.security.common.MasterPassword;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.io.IOException;

import com.sun.enterprise.security.store.PasswordAdapter;


/**
 * View that translate configured attributes containing properties like ${foo.bar}
 * into system properties values.
 *
 * @author Jerome Dochez
 */
public class TranslatedConfigView implements ConfigView {

    final static Pattern p = Pattern.compile("([^\\$]*)\\$\\{([^\\}]*)\\}([^\\$]*)");

    private static final String ALIAS_TOKEN = "ALIAS";
    
    public static Object getTranslatedValue(Object value) {
        if (value!=null && value instanceof String) {
            String stringValue = value.toString();
            if (stringValue.indexOf('$')==-1) {
                return value;
            }
            MasterPassword masterpasswd =  habitat.getByContract(MasterPassword.class);
            if (masterpasswd!= null) {
                if (getAlias(stringValue) != null) {
                    try{
                        return getRealPasswordFromAlias(masterpasswd,stringValue);
                    } catch (Exception e) {
                        Logger.getAnonymousLogger().severe("Error in dealiasing the password " +e.getLocalizedMessage());
                        return stringValue;
                    }
                }
            }
           

            Matcher m = p.matcher(stringValue);
            while (m.find()) {
                String newValue = System.getProperty(m.group(2).trim());
                if (newValue!=null) {
                    stringValue = m.replaceFirst(m.quoteReplacement(m.group(1)+
                            System.getProperty(m.group(2).trim())+m.group(3)));
                    m.reset(stringValue);
                }
            }
            return stringValue;
        }
        return value;
    }

    final ConfigView masterView;

    
    TranslatedConfigView(ConfigView master ) {
        this.masterView = master;

    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return getTranslatedValue(masterView.invoke(proxy, method, args));
    }


    public ConfigView getMasterView() {
        return masterView;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setMasterView(ConfigView view) {
        // immutable implementation
    }

    public <T extends ConfigBeanProxy> Class<T> getProxyType() {
        return masterView.getProxyType();
    }

    public <T extends ConfigBeanProxy> T getProxy(Class<T> proxyType) {
        return proxyType.cast(Proxy.newProxyInstance(proxyType.getClassLoader(), new Class[]{proxyType},
                 this));
    }
    static Habitat habitat;
    public static void setHabitat(Habitat h) {
         habitat = h;
    }

   /**
     * check if a given property name matches AS alias pattern ${ALIAS=aliasname}.
     * if so, return the aliasname, otherwise return null.
     * @param propName The property name to resolve. ex. ${ALIAS=aliasname}.
     * @return The aliasname or null.
     */
    static public String getAlias(String propName)
    {
       String aliasName=null;
       String starter = "${" + ALIAS_TOKEN + "="; //no space is allowed in starter
       String ender   = "}";

       propName = propName.trim();
       if (propName.startsWith(starter) && propName.endsWith(ender) ) {
           propName = propName.substring(starter.length() );
           int lastIdx = propName.length() - 1;
           if (lastIdx > 1) {
              propName = propName.substring(0,lastIdx);
              if (propName!=null)
                 aliasName = propName.trim();
           }
       }
       return aliasName;
    }

    public static String getRealPasswordFromAlias(MasterPassword masterPasswordHelper,final String at) throws
               KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException,
               UnrecoverableKeyException {

           final String          an = getAlias(at);
           final PasswordAdapter pa = masterPasswordHelper.getMasterPasswordAdapter(); // use default password store
           final boolean     exists = pa.aliasExists(an);
           if (!exists) {

               final String msg = String.format("Alias  %s does not exist",an);
               throw new IllegalArgumentException(msg);
           }
           final String real = pa.getPasswordForAlias(an);
           return ( real );
       }

    
}
