/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010,2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.admin.servermgmt.InvalidConfigException;

import java.util.HashMap;

/**
 * This class validates the domain config Map object. It does this by invoking 
 * the validator of each required entry. Subclasses must specify the required 
 * set of DomainConfigEntryInfo objects.
 */
public abstract class DomainConfigValidator extends Validator
{
    /**
     * i18n strings manager object
     */
    private static final StringManager strMgr = 
        StringManager.getManager(DomainConfigValidator.class);

    /**
     * Holder class for domain config entry meta info. The meta info of a 
     * domainConfig entry is the key, dataType, displayText and an optional 
     * validator object. The key must be defined in the DomainConfig class.
     */
    protected static class DomainConfigEntryInfo
    {
        final String key;
        final String dataType;
        final Validator validator;

        /** Creates a new DomainConfigEntryInfo object */
        public DomainConfigEntryInfo(String key, 
                                     String dataType, 
                                     Validator validator)
        {
            this.key            = key;
            this.dataType       = dataType;
            this.validator      = validator;
        }

        /**
         * Returns true if a non-null Validator object was specified during 
         * construction.
         */
        public boolean hasValidator()
        {
            return (validator != null);
        }
    }

    /**
     * An array of DomainConfigEntryInfo objects that must be initialized by 
     * subclasses.
     */
    private DomainConfigEntryInfo[] entries;

    /**
     * Constructs a new DomainConfigValidator object.
     * @param entries An array of required DomainConfigEntryInfo objects. 
     * Must be supplied by subclasses.
     */
    protected DomainConfigValidator(DomainConfigEntryInfo[] entries)
    {
        super(strMgr.getString("domainConfig"), DomainConfig.class);
        this.entries = entries;
    }
    
    protected DomainConfigValidator(String name, Class type, DomainConfigEntryInfo[] entries)
    {
        super(name, type);
        this.entries = entries;
    }

    /**
     * Validates the domainConfig. For each required domain config entry in the
     * entries, gets the value from the domainConfig object and invokes the 
     * validator of that entry. Skips the validation of an entry if no validator
     * is specified for that entry.
     * @param domainConfig The domainConfig object that needs to be validated. 
     * A domainConfig object is valid if it
     * <ul>
     * is of type DomainConfig
     * contains the required set of DomainConfig keys
     * the value for each required key is valid.
     * </ul>
     * @throws InvalidConfigException If invalid domainConfig is supplied.
     */
    public void validate(Object domainConfig) 
        throws InvalidConfigException
    {
        super.validate(domainConfig);
        for (int i = 0; i < entries.length; i++)
        {
            if (isValidate(entries[i].key, domainConfig))
            {
                final Object value = ((HashMap)domainConfig).get(entries[i].key);
                if (entries[i].hasValidator())
                {
                    entries[i].validator.validate(value);
                }
            }
        }
    }

    /**
     * @param key
     * @return Returns true if the key is valid and required.
     */
    public boolean isKeyAllowed(Object key)
    {
        return (get(key) != null);
    }

    /**
     * @param key
     * @param value
     * @return Returns true if the key is valid and required and the value for
     * that key is valid.
     */
    public boolean isValueValid(Object key, Object value)
    {
        boolean isValid = false;
        final DomainConfigEntryInfo info = get(key);
        if (info != null)
        {
            if (info.hasValidator())
            {
                try
                {
                    info.validator.validate(value);
                }
                catch (InvalidConfigException idce)
                {
                    isValid = false;
                }
            }
            else
            {
                isValid = true;
            }
        }
        return isValid;
    }

    /**
     * @return Returns the accepted datatype for the key. The returned value is
     * the fully qualified class name of the datatype. If the key is invalid or 
     * doesnot belong to the valid domain config key set, "" is returned.
     */
    public String getDataType(Object key)
    {
        final DomainConfigEntryInfo info = get(key);
        if (info != null)
        {
            return info.dataType;
        }
        return "";
    }

    /**
     * This method allows subclasses to say if an entry should be validated at
     * all. This is an attempt to add some flexibility to the otherwise static
     * validation. (Eg:- If we donot want to validate the ports during domain
     * creation)
     */
    protected abstract boolean isValidate(String name, Object domainConfig);

    /**
     * @return Returns the DomainConfigEntryInfo corresponding to the key. 
     * Returns null if no DomainConfigEntryInfo exists in the entries 
     * for the given key.
     */
    private DomainConfigEntryInfo get(Object key)
    {
        for (int i = 0; i < entries.length; i++)
        {
            if (entries[i].key.equals(key)) { return entries[i]; }
        }
        return null;
    }
}
