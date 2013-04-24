/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.api;

import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.i18n.StringManagerBase;
import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.PasswordAliasStore;

/**
 * The purpose of this class is to expand paths that contain embedded 
 * system properties of the form ${property-name}. The result must be 
 * an absolute path, or messages are logged. Here are some examples:
 *
 *      ${com.sun.aas.installRoot}/config/domain.xml
 *      /foo/${config}/domain.xml
 *      /foo/${config}/${domain-name}
 * 
 * This class is used to map paths containing system properties in 
 * domain.xml and used so that absolute paths (which are installation
 * directory specific) are not present, making domain.xml portable
 * in an SE/EE environment across many machines (with different 
 * installation directories).
 */
public class RelativePathResolver {
    
    private static Logger _logger = null;

    private static RelativePathResolver _instance = null;
    
    private static final String ALIAS_TOKEN = "ALIAS";
    private static final String ALIAS_DELIMITER = "=";

    private static PasswordAliasStore domainPasswordAliasStore = null; 
    
    private synchronized static PasswordAliasStore getDomainPasswordAliasStore() {
        if (domainPasswordAliasStore == null) {
            domainPasswordAliasStore = Globals.getDefaultHabitat().getService(DomainScopedPasswordAliasStore.class);
        }
        return domainPasswordAliasStore;
    }
    
    private synchronized static RelativePathResolver getInstance()
    {
        if (_instance == null) {
            _instance = new RelativePathResolver();
        }
        return _instance;
    }
    
    public static String unresolvePath(String path, String[] propNames) 
    {
        return getInstance().unresolve(path, propNames);    
    }
    
    public static String resolvePath(String path) 
    {
        return getInstance().resolve(path);
    }
    
    public RelativePathResolver() 
    {
    }

    /**
     * unresolvePath will replace the first occurrence of the value of the given
     * system properties with ${propName} in the given path  
     **/
    public String unresolve(String path, String[] propNames) {
        if (path != null) {           
            int startIdx;
            String propVal;
            
            //All paths returned will contain / as the separator. The 
            //assumption is that the File class can convert this to an OS
            //dependent path separator (e.g. \\ on windows).
            path = path.replace(File.separatorChar, '/');            
            for (int i = 0; i < propNames.length; i++) {
                propVal = getPropertyValue(propNames[i], true);             
                if (propVal != null) {                    
                    //All paths returned will contain / as the separator. This will allow
                    //all comparison to be done using / as the separator                       
                    propVal = propVal.replace(File.separatorChar, '/');                
                    startIdx = path.indexOf(propVal);                    
                    if (startIdx >= 0) {
                        path = path.substring(0, startIdx) +
                            "${" + propNames[i] + "}" + 
                            path.substring(startIdx + propVal.length());
                    }
                } else {
                    InternalLoggerInfo.getLogger().log(Level.SEVERE, 
                        InternalLoggerInfo.unknownProperty,
                        new Object[] {propNames[i], path});
                }
            }            
        }
        return path;
    }   

    /**
     * You would like to think that we could just log and continue (without throwing 
     a RuntimeException; however, unfortunately anything logged by the logger in the
     * launcher (PELaucnhFilter) does not appear in server.log, so for now, this 
     * will be considered a fatal error.
     */
    protected void fatalError(String message, String path) {
        InternalLoggerInfo.getLogger().log(Level.SEVERE, message, new Object[] {path});
        StringManagerBase sm = StringManagerBase.getStringManager(InternalLoggerInfo.getLogger().getResourceBundleName(),
                getClass().getClassLoader());        
        throw new RuntimeException(sm.getString(message, path));
    }
       
    private void appendChar (char c, StringBuffer propName, StringBuffer result)
    {
        if (propName == null) {
            result.append(c);
        } else {
            propName.append(c);
        }
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


    /**
     * Resolves the given property by returning its value as either
     *  1) a system property of the form ${system-property-name}
     *  2) a password alias property of the form ${ALIAS=aliasname}. Here the alias name 
     *  is mapped to a password.
     * @param propName The property name to resolve
     * @return The resolved value of the property or null.
     */    
    protected String getPropertyValue(String propName, boolean bIncludingEnvironmentVariables)
    {
        if(!bIncludingEnvironmentVariables)
          return null;
        
        // Try finding the property as a system property
        String result = System.getProperty(propName);        
        if (result == null) {            
            //If not found as a system property, the see if it is a password alias.
            int idx1 = propName.indexOf(ALIAS_TOKEN);
            if (idx1 >= 0) {
                int idx2 = propName.indexOf(ALIAS_DELIMITER, ALIAS_TOKEN.length());                
                if (idx2 > 0) {
                    String aliasName = propName.substring(idx2 + 1).trim();    
                    //System.err.println("aliasName " + aliasName);
                    try {
                        result = new String(getDomainPasswordAliasStore().get(aliasName));
                    } catch (Exception ex) {                        
                        InternalLoggerInfo.getLogger().log(Level.WARNING, InternalLoggerInfo.exceptionResolvingAlias, 
                            new Object[] {ex, aliasName, propName});
                    }
                }
            }
        }
        return result;
    }
   
    public String resolve(String path) {
        return resolve(path, true);
    }
    /**
     * Replace any system properties of the form ${property} in the given path. Note
     * any mismatched delimiters (e.g. ${property/${property2} is considered a fatal
     * error and for now causes a fatal RuntimeException to be thrown.     
     */
    public String resolve(String path, boolean bIncludingEnvironmentVariables) {
        if (path == null) {
            return path;
        }        
        
        //Now parse through the given string one character at a time looking for the 
        //starting delimiter "${". Occurrences of "$" or "{" are valid characters;
        //however once an occurrence of "${" is found, then "}" becomes a closing 
        //delimiter. 
        int size = path.length();
        StringBuffer result = new StringBuffer(size);
        StringBuffer propName = null;
        String propVal;
        //keep track of whether we have found at least one occurrence of "${". The
        //significance is that "}" is parsed as a terminating character.
        boolean foundOne = false;
        char c;
        for (int i = 0; i < size; i++) {
            c = path.charAt(i);
            switch(c) {
                case '$': {
                    if (i < size - 1 && path.charAt(i + 1) == '{') {                         
                        //found "${"
                        foundOne = true;
                        i++;
                        if (propName == null) { // start parsing a new property Name
                            propName = new StringBuffer();
                            break;
                        } else { // previous property not terminated missing }
                            fatalError(InternalLoggerInfo.referenceMissingTrailingDelim,
                                path);
                            return path; //can't happen since fatalError throws RuntimeException
                        }                        
                    } else {
                        appendChar(c, propName, result);
                    }
                    break;
                } case '}': {
                    if (foundOne) { // we have found at least one occurrence of ${                        
                        if (propName != null) {                            
                            propVal = getPropertyValue(propName.toString(), bIncludingEnvironmentVariables);
                            if (propVal != null) {                                                                                                                                                         
                                //Note: when elaborating a system property, we always convert \\ to / to ensure that 
                                //paths created on windows are compatible with unix filesystems.
                                result.append(propVal.replace(File.separatorChar, '/'));
                            } else {
                                //NOTE: We cannot ensure that system properties will always
                                //be defined and so this is an expected case. Consider
                                //a property named ${http-listener-port}. This property
                                //may be defined at the server or config level and set only
                                //when that server instance starts up. The property may not
                                //be set in the DAS.
                                result.append("${" + propName + "}");
                            }
                            propName = null;
                        } else { //no matching starting delimiter found ${
                            fatalError(InternalLoggerInfo.referenceMissingStartingDelim,
                                path);
                            return path; //can't happen since fatalError throws RuntimeException
                        }
                    } else {
                        appendChar(c, propName, result);
                    }
                    break;
                } default : {
                    appendChar(c, propName, result);
                    break;
                }                    
            }
        }
        
        if (propName != null) { // missing final } 
            fatalError(InternalLoggerInfo.referenceMissingTrailingDelim,
                path);
            return path; //can't happen
        }
        
        return result.toString();
    }
    
    /**
     * checks if string does not consist of unresolvable values
     */
    public boolean isResolvable(String path, boolean bIncludingEnvironmentVariables) {
        String resolved = resolve(path, bIncludingEnvironmentVariables);
        return (resolved.indexOf("${")<0);
    }
    
    public static void main(String[] args) {
        if (args[0].equalsIgnoreCase("unresolve")) {
            for (int i = 2; i < args.length; i++) {
                String result = unresolvePath(args[i], new String[] {args[1]});
                System.out.println(args[i] + " " + result + " " + resolvePath(result));             
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                System.out.println(args[i] + " " + resolvePath(args[i]));
            }
        }
    }
    /** Returns the actual password from the domain-wide safe password store,
     * if the given password is aliased. An aliased String is of the form
     * ${ALIAS=aliasname} where the actual password is stored in given alias name.
     * Following are the returned values:
     * <ul>
     * <li> Returns a null if given String is null. </li> 
     * <li> Retuns the given String if it is not in the alias form. </li>
     * <li> Returns the real password from store if the given String is
     *      of the alias form and the alias has been created by the
     *      administrator. If the alias is not defined in the store,
     *      an IllegalArgumentException is thrown with appropriate
     *      message. </li>
     * </ul>
     * @param at is the aliased token of the form "${ALIAS=string}"
     * @return a String representing the actual password
     * @throws IllegalArgumentException if the alias is not defined
     * @throws KeyStoreException CertificateException IOException NoSuchAlgorithmException
     *         UnrecoverableKeyException if there is an error is opening or
     *         processing the password store
     */
    public static String getRealPasswordFromAlias(final String at) throws 
            KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException,
            UnrecoverableKeyException {
        try {
            if (at == null || RelativePathResolver.getAlias(at) == null) {
                return ( at );
            }
        } catch (final Exception e) { //underlying code is unsafe!
            return (at);
        }
        final String          an = RelativePathResolver.getAlias(at);
        final boolean     exists = getDomainPasswordAliasStore().containsKey(an);
        if (!exists) {
            final StringManager lsm = StringManager.getManager(RelativePathResolver.class);
            final String msg = lsm.getString("no_such_alias", an, at);
            throw new IllegalArgumentException(msg);
        }
        final String real = new String(getDomainPasswordAliasStore().get(an));
        return ( real );
    }    
}
