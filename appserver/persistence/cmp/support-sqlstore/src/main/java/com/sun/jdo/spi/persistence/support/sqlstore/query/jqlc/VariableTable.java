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
 * VariableTable.java
 *
 * Created on April 12, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;

import java.util.*;

import org.glassfish.persistence.common.I18NHelper;

import com.sun.jdo.api.persistence.support.JDOQueryException;
import com.sun.jdo.api.persistence.support.JDOUnsupportedOptionException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;

/**
 * The variable table 
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public class VariableTable
{
    /**
     * I18N support
     */
    protected final static ResourceBundle messages = 
        I18NHelper.loadBundle(VariableTable.class);

    /**
     * A VarInfo consists of two info fields:
     * - constraint: the variable is constraint with the specified expr
     * - used: the variable is used
     */
    static class VarInfo
    {
        /**
         * The constraint expression.
         */
        JQLAST constraint;

        /**
         * Set of JQLAST nodes denoting an access of this variable.
         */
        Set used;

        /**
         * Dependency for this variable. 
         * The constraint for this variable may use another variable. 
         */
        String dependsOn;

        /**
         * Flag whether this varInfo is checked already (see checkConstraints)
         */
        int status;

        static final int UNCHECKED = 0;
        static final int IN_PROGRESS = 1;
        static final int CHECKED = 2;

        VarInfo()
        {
            this.constraint = null;
            this.used = new HashSet();
            this.dependsOn = null;
            this.status = UNCHECKED;
        }

        VarInfo(VarInfo other)
        {
            this.constraint = other.constraint;
            this.used = new HashSet(other.used);
            this.dependsOn = other.dependsOn;
            this.status = other.status;
        }
    }

    /** */
    private ErrorMsg errorMsg;

    /** List of names of declared variables. */
    private List declaredVars; 

    /** Map of variable infos. */
    private Map varInfos;

    /**
     * Create an empty variable table
     */
    public VariableTable(ErrorMsg errorMsg)
    {
        this.errorMsg = errorMsg;
        declaredVars = new ArrayList();
        varInfos = new HashMap();
    }

    /**
     * Create a variable table initialized with the entries of the other variable table.
     * The constructor creates copies of the values stored in the map (instances of class VarInfo).
     */
    public VariableTable(VariableTable other)
    {
        errorMsg = other.errorMsg;
        declaredVars = other.declaredVars;
        varInfos = new HashMap();
        for (Iterator i = other.varInfos.entrySet().iterator(); i.hasNext();)
        {
            Map.Entry entry = (Map.Entry)i.next();
            varInfos.put(entry.getKey(), new VarInfo((VarInfo)entry.getValue()));
        }
    }

    /**
     * Creates a new entry in the variable table with the specified name as key and 
     * an empty value.
     */
    public void add(String name)
    {
        declaredVars.add(name);
        // init var entry as not constraint and unused
        varInfos.put(name, new VarInfo());
    }

    /**
     * Mark the specified variable as used. 
     * The method sets the info field of the VarInfo object to true.
     */
    public void markUsed(JQLAST variable, String dependendVar)
    {
        String name = variable.getText();
        VarInfo entry = (VarInfo)varInfos.get(name);
        if (entry == null)
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "jqlc.variabletable.markused.varnotfound", //NOI18N
                name));
        entry.used.add(variable);
        if (dependendVar != null)
        {
            VarInfo dependendVarInfo = (VarInfo)varInfos.get(dependendVar);
            if (dependendVarInfo.dependsOn != null)
                throw new JDOFatalInternalException(I18NHelper.getMessage(
                    messages,
                    "jqlc.variabletable.markused.multidep", //NOI18N
                    dependendVar, dependendVarInfo.dependsOn, name));
            dependendVarInfo.dependsOn = name;
        }
    }

    /**
     * Mark the specified variable as constaint with the specified expr.
     * The method sets the constraint field of the VarInfo object to true.
     */
    public void markConstraint(JQLAST variable, JQLAST expr)
    {
        String name = variable.getText();
        VarInfo entry = (VarInfo)varInfos.get(name);
        if (entry == null)
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages,
                "jqlc.variabletable.markconstraint.varnotfound", //NOI18N
                name));
        String old = (entry.constraint==null ? null : entry.constraint.getText());
        if ((old != null) && !old.equals(expr.getText()))
        {
            errorMsg.unsupported(variable.getLine(), variable.getColumn(),
                I18NHelper.getMessage(messages, "jqlc.variabletable.markconstraint.multiple", //NOI18N
                                      name));
        }
        entry.constraint = expr;
    }

    /**
     * Merges the specified variable table (other) into this variable table.
     */
    public void merge(VariableTable other)
    {
        for (Iterator i = declaredVars.iterator(); i.hasNext();)
        { 
            String name = (String)i.next();
            VarInfo info = (VarInfo)varInfos.get(name);
            VarInfo otherInfo = (VarInfo)other.varInfos.get(name);
            
            // copy other info if this info is empty
            if ((info.constraint == null) && (info.used.size() == 0))
            {
                info.constraint = otherInfo.constraint;
                info.used = otherInfo.used;
                info.dependsOn = otherInfo.dependsOn;
                info.status = otherInfo.status;
                continue;
            }

            // do nothing if otherInfo is empty
            if ((otherInfo.constraint == null) && (otherInfo.used.size() == 0))
            {
                continue;
            }
            
            // constraint check
            // If both variables tables include constraints they have to be the same
            if ((info.constraint != null) && (otherInfo.constraint != null))
            {
                if (!otherInfo.constraint.getText().equals(info.constraint.getText()))
                {
                    throw new JDOUnsupportedOptionException(
                        I18NHelper.getMessage(messages, "jqlc.variabletable.merge.different", name)); //NOI18N
                }
            }
            // If at least one variable table does not define constraint, 
            // nullify the constaint in this variable table
            else
            {
                info.constraint = null;
                info.dependsOn = null;
                info.status = VarInfo.UNCHECKED;
            }
            
            // copy otherInfo.used to this used list
            info.used.addAll(otherInfo.used);
        }
    }

    /**
     *
     */
    public void checkConstraints()
    {
        // iterate declaredVars to check the variables in the order they are declared
        for (Iterator i = declaredVars.iterator(); i.hasNext();)
        {
            String name = (String)i.next();
            VarInfo info = (VarInfo)varInfos.get(name);
            checkConstraint(name, info);
        }
    }

    protected void checkConstraint(String variable, VarInfo info)
    {
        switch (info.status)
        {
        case VarInfo.UNCHECKED:
            // if unchecked, start checking
            info.status = VarInfo.IN_PROGRESS;
            break;
        case VarInfo.IN_PROGRESS:
            // if this VarInfo is currently processed we have a cyclic dependency
            throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jqlc.variabletable.checkconstraint.cycle", // NOI18N
                                      variable));
        case VarInfo.CHECKED:
            // if alreday checked just return
            return;
        }
        
        if (info.dependsOn != null)
        {
            VarInfo dependendVarInfo = (VarInfo)varInfos.get(info.dependsOn);
            checkConstraint(info.dependsOn, dependendVarInfo);
        }
        
        if ((info.constraint != null) && (info.used.size() == 0))
        {
            throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jqlc.variabletable.checkconstraint.unused", //NOI18N
                                      variable));
        }
        
        attachConstraintToUsedAST(info);
        info.status = VarInfo.CHECKED;
    }
    
    /**
     *
     */
    protected void attachConstraintToUsedAST(VarInfo info)
    {
        for (Iterator i = info.used.iterator(); i.hasNext();)
        {
            JQLAST varNode = (JQLAST)i.next();
            if (varNode.getFirstChild() == null)
                varNode.setFirstChild(JQLAST.Factory.getInstance().dupTree(info.constraint));
        }
    }
    
}
