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

package com.sun.enterprise.admin.servermgmt;

import java.io.File;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.i18n.StringManager;

/**
 * This class performs the file related validations such as 
 * <ul>
 * existence of the file
 * read, write & execute permissions,
 * whether the file is a directory or a file
 * </ul>
 * <b>NOT THREAD SAFE</b>
 */
public class FileValidator extends Validator
{
    /**
     * The valid constraint set.
     */
    public static final String validConstraints = "drwx";

    /**
     * i18n strings manager object
     */
    private static final StringManager strMgr = 
        StringManager.getManager(FileValidator.class);

    /**
     * The current constraint set.
     */
    private String constraints = "r";

    /**
     * Constructs a new FileValidator object.
     * @param name The name of the entity that will be validated. This name is
     * used in the error message.
     * @param constraints The constaint set that will be checked for any given
     * file during validation.
     */
    public FileValidator(String name, String constraints)
    {
        super(name, java.lang.String.class);

        if (isValidConstraints(constraints))
        {
            this.constraints = constraints;
        }
    }

    /**
     * @return Returns the current constraint set.
     */
    public String getConstraints()
    {
        return constraints;
    }

    /**
     * Sets the current constraint set to the given set if it is a valid 
     * constriant set.
     */
    public String setConstraints(String constraints)
    {
        if (isValidConstraints(constraints))
        {
            this.constraints = constraints;
        }
        return this.constraints;
    }

    /**
     * Validates the given File.
     * @param str Must be the absolute path of the File that will be validated.
     * @throws InvalidConfigException
     */
    public void validate(Object str) throws InvalidConfigException
    {
        super.validate(str);
        new StringValidator(getName()).validate(str);
        File f = new File((String)str);
        validateConstraints(f);
    }

    /**
     * Validates the given File against the current constraint set.
     */
    void validateConstraints(File file) throws InvalidConfigException
    {
        final File f = FileUtils.safeGetCanonicalFile(file);
        final String constriants = getConstraints();
        char[] ca = constriants.toCharArray();
        for (int i = 0; i < ca.length; i++)
        {
            switch (ca[i])
            {
                case 'r' :
                    if (!f.canRead())
                    {
                        throw new InvalidConfigException(
                            strMgr.getString("fileValidator.no_read", 
                                             f.getAbsolutePath()));
                    }
                    break;
                case 'w' :
                    if (!f.canWrite())
                    {
                        throw new InvalidConfigException(
                            strMgr.getString("fileValidator.no_write", 
                                             f.getAbsolutePath()));
                    }
                    break;
                case 'd' :
                    if (!f.isDirectory())
                    {
                        throw new InvalidConfigException(
                            strMgr.getString("fileValidator.not_a_dir", 
                                             f.getAbsolutePath()));
                    }
                    break;
                case 'x' :
                    //do what
                    break;
                default :
                    break;
            }
        }
    }

    /**
     * Checks if the given constraint set is a subset of valid constraint set.
     * @param constraints
     * @return Returns true if the given constraint set is subset or equal to
     * the valid constraint set - "drwx".
     */
    boolean isValidConstraints(String constraints)
    {
        if (constraints == null) { return false; }
        final int length = constraints.length();
        if ((length == 0) || (length > 4)) { return false; }
        boolean isValid = true;
        for (int i = 0; i < length; i++)
        {
            char ch = constraints.charAt(i);
            switch (ch)
            {
                case 'r' :
                case 'w' :
                case 'x' :
                case 'd' :
                    continue;
                default :
                    isValid = false;
                    break;
            }
        }
        return isValid;
    }
}
