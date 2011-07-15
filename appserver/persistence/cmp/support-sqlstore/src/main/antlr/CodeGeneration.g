/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * CodeGeneration.g
 *
 * Created on March 13, 2000
 */

header
{
    package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;
    
    import java.util.*;
    import java.lang.reflect.Field;
    import java.lang.IllegalAccessException;

    import com.sun.jdo.api.persistence.support.JDOFatalUserException;
    import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable;
    import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
    import com.sun.jdo.spi.persistence.support.sqlstore.RetrieveDesc;
    import com.sun.jdo.spi.persistence.support.sqlstore.StateManager;

    import org.glassfish.persistence.common.I18NHelper;
    import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;
    import com.sun.jdo.spi.persistence.utility.logging.Logger;

    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.TypeTable;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.Type;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.ClassType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.FieldInfo;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumericType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumericWrapperClassType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.NumberType;
    import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.StringType;
}

/**
 * This class defines the code generation pass of the JQL compiler.
 * Input of this pass is the typed and optimized AST as produced by optimizer.
 * The result is a RetrieveDesc.
 *
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 * @version 0.1
 */
class CodeGeneration extends TreeParser;
                             
options
{
    importVocab = JQL;
    ASTLabelType = "JQLAST"; //NOI18N
}

{
    /** Name of the USE_IN property. */
    public static final String USE_IN_PROPERTY = 
        "com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc.USE_IN";

    /** */
    private static final boolean USE_IN = Boolean.getBoolean(USE_IN_PROPERTY);

    /**
     * I18N support
     */
    protected final static ResourceBundle messages = 
      I18NHelper.loadBundle(CodeGeneration.class);

    /**
     * The persistence manager the query object is connected to.
     */
    protected PersistenceManager pm;
    
    /**
     * type table
     */
    protected TypeTable typetab;
    
    /**
     * query parameter table
     */
    protected ParameterTable paramtab;
    
    /**
     *
     */
    protected ErrorMsg errorMsg;
    
    /**
     * prefetchEnabled flag for RetrieveDesc.
     */
    protected boolean prefetchEnabled;
    
    /**
     * The RetrieveDesc for the candidate class. 
     * Code gen for the CLASS_DEF AST will initilaized this variable.
     * Code gen for the filter expression will add the constraints.
     */
    protected RetrieveDesc candidateRD;

    /**
     * rd2TagMap maps RetrieveDesc to tags. A tag is either the variable name or
     * the navigation path that used to create a new RetrieveDesc. This info is 
     * needed to identify whether two different RetrieveDescs denote the same 
     * variable or relationship.
     */
    protected Map rd2TagMap;

    /** 
     * Set of RetrieveDescs. CodeGeneration uses this set to prevent multiple 
     * addConstraint calls for the RetrieveDescs denoting a variable.
     */
    protected Set boundRetrieveDescs;

    /** The logger */
    private static Logger logger = LogHelperQueryCompilerJDO.getLogger();

    /**
     * Defines the SQL wildcard character to be used in wildcard pattern 
     * (string methods startsWith and endsWith).
     */
    protected static final String WILDCARD_PATTERN = "%"; //NOI18N
    
    /**
     *
     */
    public void init(PersistenceManager pm, TypeTable typetab, 
                     ParameterTable paramtab, ErrorMsg errorMsg,
                     boolean prefetchEnabled)
    {
        this.pm = pm;
        this.typetab = typetab;
        this.paramtab = paramtab;
        this.errorMsg = errorMsg;
        this.prefetchEnabled = prefetchEnabled;
        this.rd2TagMap = new HashMap();
        this.boundRetrieveDescs = new HashSet();
    }
    
    /**
     *
     */
    public void reportError(RecognitionException ex) {
        errorMsg.fatal("CodeGeneration error", ex); //NOI18N
    }

    /**
     *
     */
    public void reportError(String s) {
        errorMsg.fatal("CodeGeneration error: " + s); //NOI18N
    }
    
    /**
     * Returns the RetrieveDesc that represents the current query.
     */
    public RetrieveDesc getRetrieveDesc()
    {
        if (candidateRD instanceof DebugRetrieveDesc)
            return ((DebugRetrieveDesc)candidateRD).wrapped;
        return candidateRD;
    }

    /**
     * Helper method for checkRetrieveDesc handling operators & and &&.
     */
    protected void checkAndOpRetrieveDesc(JQLAST op, JQLAST left, 
        JQLAST right, Map usedRD) throws RecognitionException
    {
        if ((right.getType() == CONTAINS) || (right.getType() == NOT_CONTAINS))
        {
            // If right is a CONTAINS clause, start analysing the right expr.
            // This ensures that the lft expression can reuse the RD defined 
            // for the variable from the contains clause
            checkRetrieveDesc(right, usedRD);
            checkRetrieveDesc(left, usedRD);
        }
        else
        {
            checkRetrieveDesc(left, usedRD);
            checkRetrieveDesc(right, usedRD); 
        }
        op.setRetrieveDesc(getCommonRetrieveDesc(left, right)); 
    }
    
    /**
     * Check the attached RetrieveDesc of the specified binary operation and its operands.
     */
    protected RetrieveDesc getCommonRetrieveDesc(JQLAST left, JQLAST right)
    {
        RetrieveDesc rd = null;
        RetrieveDesc leftRD = left.getRetrieveDesc();
        RetrieveDesc rightRD = right.getRetrieveDesc();

        if ((leftRD == null) && (rightRD != null))
        {
            // case 1: no RetrieveDesc for left operand, but right operand returns RetrieveDesc
            // attach the right RetrieveDesc to all nodes of the left subtree
            propagateRetrieveDesc(left, rightRD);
            rd = rightRD;
        }

        else if ((leftRD != null) && (rightRD == null))
        {
            // case 2: no RetrieveDesc for right operand, but left operand returns RetrieveDesc
            // attach the left RetrieveDesc to all nodes of the right subtree
            propagateRetrieveDesc(right, leftRD);
            rd = leftRD;
        }
        else if ((leftRD != null) && (rightRD != null))
        {
            // case 3: both left and right operand have a RetrieveDesc attached
            if (leftRD == rightRD)
            {
                // case 3a: left and right RetrieveDesc are identical
                rd = leftRD;
            }
            else
            {
                // case 3b: left and right RetrieveDesc are NOT identical
                // check navigation:
                rd = getCommonRetrieveDescHelper(leftRD, findNavigationSource(left), 
                                                 rightRD, findNavigationSource(right));

                // use leftRD as default
                if (rd == null) 
                {
                    rd = leftRD;
                }
            }
        }
        return rd;
    }

    /** Helper method for getCommonRetrieveDesc used to check navigation. */
    protected RetrieveDesc getCommonRetrieveDescHelper(
        RetrieveDesc leftRD, JQLAST leftNavSrc, 
        RetrieveDesc rightRD, JQLAST rightNavSrc)
    {
        RetrieveDesc rd = null;
        String leftPath = (String)rd2TagMap.get(leftRD);
        String rightPath = (String)rd2TagMap.get(rightRD);
        RetrieveDesc leftNavSrcRD = 
            (leftNavSrc == null) ? null : leftNavSrc.getRetrieveDesc();
        String leftNavSrcPath = 
            (leftNavSrcRD == null ) ? null: (String)rd2TagMap.get(leftNavSrcRD);
        RetrieveDesc rightNavSrcRD = 
            (rightNavSrc == null) ? null : rightNavSrc.getRetrieveDesc();
        String rightNavSrcPath = 
            (rightNavSrcRD == null) ? null : (String)rd2TagMap.get(rightNavSrcRD);

        if ((leftNavSrcPath != null) && leftNavSrcPath.equals(rightPath))
        {
            // case I: left operand is a navigation and 
            //         the navigation source is equal to the right operand
            rd = rightRD;
        }
        else if ((rightNavSrcPath != null) && rightNavSrcPath.equals(leftPath))
        {
            // case II: right operand is a navigation and 
            //          the navigation source is equal to the left operand
            rd = leftRD;
        }
        else if ((leftNavSrcPath != null) && (rightNavSrcPath != null) &&  
            leftNavSrcPath.equals(rightNavSrcPath))
        {
            // case III: both operands are navigations and have the same source
            rd = leftNavSrcRD;
        }
        else {
            // case IV: check whether the navigation source is a bound variable.
            //          If yes, check the collection source
            JQLAST leftConstraint = findNavigationSourceOfBoundVariable(leftNavSrc);
            JQLAST rightConstraint = findNavigationSourceOfBoundVariable(rightNavSrc);
            if ((leftConstraint != null) && (rightConstraint != null))
            {
                rd = getCommonRetrieveDescHelper(leftRD, leftConstraint, 
                                                 rightRD, rightConstraint);
            }
            else if ((leftConstraint == null) && (rightConstraint != null))
            {
                rd = getCommonRetrieveDescHelper(leftRD, leftNavSrc, 
                                                 rightRD, rightConstraint);
            }
            else if ((leftConstraint != null) && (rightConstraint == null))
            {
                rd = getCommonRetrieveDescHelper(leftRD, leftConstraint, 
                                                 rightRD, rightNavSrc);
            }
        }
        return rd;
    }

    /**
     * Helper method to support getting the common RetrieveDesc for operands 
     * taking three arguments such as like with escape, substring, indexOf.
     */
    protected RetrieveDesc getCommonRetrieveDesc(JQLAST arg1, JQLAST arg2, JQLAST arg3)
    {
        RetrieveDesc rd = null;
        if (arg3 == null) {
            // Just call the regular method for binray ops, 
            // if the third argument is not specified.
            rd = getCommonRetrieveDesc(arg1, arg2);
        }
        else {
            // First check args two and three.
            getCommonRetrieveDesc(arg2, arg3);
            // Now check the first and the second arg.
            rd = getCommonRetrieveDesc(arg1, arg2);
            // Propagate the common RetrieveDesc to the third arg. 
            // This is important, if arg two and three are literals.
            // Then the first call checking arg2 and arg3 did not attach any 
            // RetrieveDesc. The second call checking arg1 and arg2 might have 
            // propagated a rd from arg1 to arg2. So this propagateRetrieveDesc
            // call propagates this rd to arg3, too.
            propagateRetrieveDesc(arg3, rd);
        }
        return rd;
    }

    /** 
     * Helper method to support getting the common RetrieveDesc for object 
     * comparison operators.
     */
    protected RetrieveDesc getObjectComparisonRetrieveDesc(JQLAST left, JQLAST right)
    {
        RetrieveDesc rd = null;
        if ((left.getType() == NAVIGATION) && 
            (right.getType() == VALUE) && (right.getValue() == null))
        {
            // case obj.relship == null
            // take the RetrieveDesc from the navigation source
            rd = ((JQLAST)left.getFirstChild()).getRetrieveDesc();
        }
        else if ((left.getType() == VALUE) && (left.getValue() == null) && 
            (right.getType() == NAVIGATION))
        {
            // case null == obj.relship
            // take the RetrieveDesc from the navigation source
            rd = ((JQLAST)right.getFirstChild()).getRetrieveDesc();
        }
        else
        {
            // use regular getCommonRetrieveDesc
            rd = getCommonRetrieveDesc(left, right);
        }
        return rd;
    }

    /**
     * Returns the source if a navigation or field access.
     */
    protected JQLAST findNavigationSource(JQLAST tree)
    {
        JQLAST child = (JQLAST)tree.getFirstChild();
        switch (tree.getType())
        {
        case NOT_IN:
        case FIELD_ACCESS:
        case NAVIGATION:
            return findNavigationSource(child);
        case THIS:
        case VARIABLE:
            return tree;
        case CONTAINS:
        case NOT_CONTAINS:
            return null;
        default:
            for (JQLAST node = child; node != null; node = (JQLAST)node.getNextSibling())
            {
                JQLAST tmp = findNavigationSource(node);
                if (tmp != null)
                    return tmp;
            }
        }
        return null;
    }

    /** 
     * If the specifid node is a bound variable return the navigation source of 
     * it's collection. 
     */
    protected JQLAST findNavigationSourceOfBoundVariable(JQLAST tree)
    {
        if ((tree.getType() == VARIABLE) && (tree.getFirstChild() != null))
            return findNavigationSource((JQLAST)tree.getFirstChild());
        return null;
    }
    
    /**
     * Attach the specified RetrieveDesc to all JQLAST node of the ast subtree,
     * that do not have a RetrieveDesc attached.
     */
    protected void propagateRetrieveDesc(JQLAST ast, RetrieveDesc rd)
    {
        if (ast.getRetrieveDesc() == null)
        {
            ast.setRetrieveDesc(rd);
        }
        for (JQLAST node = (JQLAST)ast.getFirstChild(); 
             node != null; 
             node = (JQLAST)node.getNextSibling())
        {
            propagateRetrieveDesc(node, rd);
        }
    }

    /**
     * Returns an Object representing 0 according to the specified type.
     */
    protected Object getZeroValue(Type type)
    {
        return (type instanceof NumberType) ? 
               ((NumberType)type).getValue(new Integer(0)) :
               null;
    }

    /**
     * Returns an Object representing -1 according to the specified type.
     */
    protected Object getMinusOneValue(Type type)
    {
        return (type instanceof NumberType) ? 
               ((NumberType)type).getValue(new Integer(-1)) :
               null;
    }
   
    /**
     * Returns -value. 
     * The method assumes that the passed argument is a numeric wrapper class object. 
     * If so it negates the wrapped numeric value and wraps the negated value into a 
     * numeric wrapper class object.
     */
    protected Object negate(Object value, Type type)
    {
        return (type instanceof NumberType) ? 
               ((NumberType)type).negate((Number)value) :
               null;
    }

    /**
     * Returns the boolean operation of the equivalent relational expression 
     * with swapped arguments.
     * expr1 > expr2 <=> expr2 < expr1
     */
    protected int getSwappedOp(int operation)
    {
        int ret = 0;
        switch (operation)
        {
        case RetrieveDesc.OP_EQ:
            ret = RetrieveDesc.OP_EQ;
            break;
        case RetrieveDesc.OP_NE:
            ret = RetrieveDesc.OP_NE;
            break;
        case RetrieveDesc.OP_LT:
            ret = RetrieveDesc.OP_GT;
            break;
        case RetrieveDesc.OP_LE:
            ret = RetrieveDesc.OP_GE;
            break;
        case RetrieveDesc.OP_GT:
            ret = RetrieveDesc.OP_LT;
            break;
        case RetrieveDesc.OP_GE:
            ret = RetrieveDesc.OP_LE;
            break;
        }
        return ret;
    }

    /**
     * Code generation for a comparison of the form field relop value, 
     * where field denotes a non relationship field
     * This method checks for null values and generates OP_NULL / OP_NOTNULL constraints 
     * in the case of field relop null
     */
    protected void generateSimpleFieldValueComparison(RetrieveDesc rd, String name, 
                                                      int operation, Object value)
    {
        if (value != null)
        {
            rd.addConstraint(name, operation, value);
        }   
        else if (operation == RetrieveDesc.OP_EQ)
        {
            rd.addConstraint(name, RetrieveDesc.OP_NULL, null);
        }
        else if (operation == RetrieveDesc.OP_NE)
        {
            rd.addConstraint(name, RetrieveDesc.OP_NOTNULL, null);
        }
        else
        {
            errorMsg.fatal(I18NHelper.getMessage(messages, "jqlc.codegeneration.generatesimplefieldvaluecomparison.invalidvalue")); //NOI18N
        }
    }

    /**
     * Code generation for a comparison of the form 
     *   dbvalue relop constant
     * where dbvalue denotes an object in the database such as 
     * - this
     * - the result of a relationship navigation
     * - variable access
     * and constant is a constant value at query compile time (e.g. a literal)
     */
    protected void generateDbValueConstantComparison(RetrieveDesc rd, ClassType objectType, 
                                                     int operation, Object value, Type valueType)
    {
        int booleanOp = getKeyFieldsComparisonBooleanOp(operation);

        List keyFieldNames = objectType.getKeyFieldNames();
        for (Iterator i = keyFieldNames.iterator(); i.hasNext();)
        {
            String keyFieldName = (String)i.next();
            Object keyFieldValue = null;
            if (value != null)
            {
                keyFieldValue = getFieldValue((ClassType)valueType, value, keyFieldName);
            }
            generateSimpleFieldValueComparison(rd, keyFieldName, operation, keyFieldValue);
            if (i.hasNext())
                rd.addConstraint(null, booleanOp, null);
        }
    }

    /**
     * Code generation for a comparison of the form 
     *   dbvalue relop dbvalue
     * where dbvalue denotes an object in the database such as 
     * - this
     * - the result of a relationship navigation
     * - variable access
     */
    protected void generateDbValueDbValueComparison(RetrieveDesc leftRD, ClassType leftType, int operation, 
                                                    RetrieveDesc rightRD, ClassType rightType)
    {
        int booleanOp = getKeyFieldsComparisonBooleanOp(operation);
        
        // Note, this code assumes that both operands are of class types that have 
        // the same key fields. Thus take the list of key field names of the left side.
        List leftKeyFieldNames = leftType.getKeyFieldNames();
        for (Iterator i = leftKeyFieldNames.iterator(); i.hasNext();)
        {
            String keyFieldName = (String)i.next();
            leftRD.addConstraint(keyFieldName, operation, rightRD, keyFieldName);
            if (i.hasNext())
                leftRD.addConstraint(null, booleanOp, null);
        }
    }

    /**
     * Code generation for a comparison of the form 
     *   parameter relop constantValue
     */
    protected void generateParameterValueComparison(RetrieveDesc rd,
                                                    String paramName, 
                                                    int operation, Object value)
    {
        if (value != null)
        {
            rd.addConstraint(null, RetrieveDesc.OP_VALUE, value);
            rd.addConstraint(null, RetrieveDesc.OP_PARAMETER,
                paramtab.getParameterInfoForParamName(paramName));
            rd.addConstraint(null, operation, null);
        }   
        else if (operation == RetrieveDesc.OP_EQ)
        {
            rd.addConstraint(null, RetrieveDesc.OP_PARAMETER,
                paramtab.getParameterInfoForParamName(paramName));
            rd.addConstraint(null, RetrieveDesc.OP_NULL, null);
        }
        else if (operation == RetrieveDesc.OP_NE)
        {
            rd.addConstraint(null, RetrieveDesc.OP_PARAMETER,
                paramtab.getParameterInfoForParamName(paramName));
            rd.addConstraint(null, RetrieveDesc.OP_NOTNULL, null);
        }
        else
        {
            errorMsg.fatal(I18NHelper.getMessage(messages, "jqlc.codegeneration.generateparametervaluecomparison.invalidvalue")); //NOI18N
        }
    }

    /**
     * Returns the boolean operation used to connect the key field comparison expressions:
     * l == r is mapped to l.pk1 == r.pk1 & ... & l.pkn == r.pkn => return &
     * l != r is mapped to l.pk1 != r.pk1 | ... | l.pkn != r.pkn => return |
     */
    protected int getKeyFieldsComparisonBooleanOp(int operation)
    {
        switch (operation)
        {
        case RetrieveDesc.OP_EQ:
            return RetrieveDesc.OP_AND;
        case RetrieveDesc.OP_NE:
            return RetrieveDesc.OP_OR;
        }
        errorMsg.fatal(I18NHelper.getMessage(messages,
            "jqlc.codegeneration.getkeyfieldscomparisonbooleanop.invalidobj", //NOI18N
            String.valueOf(operation)));
        return 0;
    }

    /**
     * Returns the value of the  field access object.field.
     * Uses jdoGetField for object of a persistence capable class and reflection otherwise.
     */
    protected static Object getFieldValue (ClassType classType, Object object, String fieldName)
    {
        Object value = null;
        FieldInfo fieldInfo = classType.getFieldInfo(fieldName);
        if (classType.isPersistenceCapable())
        {
            PersistenceCapable pc = (PersistenceCapable)object;
            int index = fieldInfo.getFieldNumber();
            StateManager stateManager = pc.jdoGetStateManager();
            
            if (stateManager != null)
            {
                // call stateManager.prepareGetField to allow the stateManager 
                // to mediate the field access
                stateManager.prepareGetField(index);
            }
            value = pc.jdoGetField(index);
        }
        else
        {
            // non persistence capable class => use reflection
            try
            {
                value = fieldInfo.getField().get(object);
            }
            catch (IllegalAccessException e) 
            {
                throw new JDOFatalUserException(
                    I18NHelper.getMessage(messages, "jqlc.codegeneration.fieldaccess.illegal",  //NOI18N
                        fieldName, (object==null ? "null" : object.toString())), e); //NOI18N
            }
        }
        return value;
    }
    
    /**
     * This method checks whether the result RetrieveDesc needs a DISTINCT clause or not. 
     * @param query the query AST
     */
    protected void handleDistinct(JQLAST query, boolean distinct)
    {
        // candidateRD is null in the case of false filter
        if (candidateRD == null)
            return;
        
        if (distinct)
            candidateRD.addResult(RetrieveDesc.OP_DISTINCT, FieldTypeEnumeration.NOT_ENUMERATED);
    }

    /**
     * This method returns true if the specified node is an AST that represensts a value.
     * It returns false for CONTAINS/NOT_CONTAINS nodes and boolean operations that include 
     * only CONTAINS nodes 
     */
    protected boolean pushesValueOnStack(JQLAST node)
    {
        switch(node.getType())
        {
        case CONTAINS:
        case NOT_CONTAINS:
            return false;
        case BAND:
        case BOR:
        case AND:
        case OR:
            JQLAST left = (JQLAST)node.getFirstChild();
            JQLAST right = (JQLAST)left.getNextSibling();
            return pushesValueOnStack(left) || pushesValueOnStack(right);
        default:
            return true;
        }
    }

    /**
     * Create a new RetrieveDesc for the specified classType and
     * store this RetrieveDesc in the cache with the specified path expression attached.
     * The method wraps the RetrieveDesc in a DebugRetrieveDesc, if debug mode is on.
     */
    protected RetrieveDesc createRetrieveDesc(String pathExpr, ClassType classType)
    {
        RetrieveDesc rd = pm.getRetrieveDesc(classType.getJavaClass());
        if (logger.isLoggable(Logger.FINEST)) 
        {
            rd = new DebugRetrieveDesc(rd);
            logger.finest("LOG_JQLCDumpRD", "create " + JQLAST.getRetrieveDescRepr(rd)); //NOI18N
        }        
        rd2TagMap.put(rd, pathExpr);
        rd.setNavigationalId(pathExpr);
        return rd;
    }

    /**
     * Wrapper that traces the RetrieveDesc calls
     */
    protected static class DebugRetrieveDesc implements RetrieveDesc
    {
        RetrieveDesc wrapped = null;

        DebugRetrieveDesc(RetrieveDesc wrapped)
        {
            this.wrapped = wrapped;
        }
        
        public RetrieveDesc unwrap(RetrieveDesc rd)
        {
            if (rd instanceof DebugRetrieveDesc)
                return ((DebugRetrieveDesc)rd).wrapped;
            return rd;
        }

        // methods from RetrieveDesc 
        public void addResult(String name, RetrieveDesc desc, boolean projection)
        {
            if (logger.isLoggable(Logger.FINEST))
                logger.finest("LOG_JQLCDumpRD", //NOI18N
                    JQLAST.getRetrieveDescRepr(this) + ".addResult(" +  //NOI18N
                    name + ", " + JQLAST.getRetrieveDescRepr(desc) + ", " + //NOI18N
                    projection + ")"); //NOI18N
            desc = unwrap(desc);
            wrapped.addResult(name, desc, projection);   
        }
        
        public void addResult(int opCode, int resultType)
        {
            if (logger.isLoggable(Logger.FINEST))
                logger.finest("LOG_JQLCDumpRD", //NOI18N
                    JQLAST.getRetrieveDescRepr(this) + ".addResult(" +  //NOI18N
                    opCode + ", " + resultType + ")"); //NOI18N
            wrapped.addResult(opCode, resultType);   
        }
        
        public void addConstraint(String name, int operation, Object value)
        {
            String thirdArgRepr = null;
            if (value instanceof RetrieveDesc)
            {
                RetrieveDesc foreignConstraint = (RetrieveDesc)value;
                thirdArgRepr = JQLAST.getRetrieveDescRepr(foreignConstraint);
                value = unwrap(foreignConstraint);
            }
            else
            {
                thirdArgRepr = (value == null) ? "null" : value.toString();
            }
            if (logger.isLoggable(Logger.FINEST))
                logger.finest("LOG_JQLCDumpRD", //NOI18N
                JQLAST.getRetrieveDescRepr(this) + ".addConstraint(" +  //NOI18N
                name + ", " + operation + ", " + thirdArgRepr + ")"); //NOI18N
            wrapped.addConstraint(name, operation, value);
        }

        public void addConstraint(String name, RetrieveDesc foreignConstraint)
        {
            if (logger.isLoggable(Logger.FINEST))
                logger.finest("LOG_JQLCDumpRD", //NOI18N
                    JQLAST.getRetrieveDescRepr(this) + ".addConstraint(" +  //NOI18N
                    name + ", " + JQLAST.getRetrieveDescRepr(foreignConstraint) + ")"); //NOI18N
            foreignConstraint = unwrap(foreignConstraint);
            wrapped.addConstraint(name, foreignConstraint);
        }

        public void addConstraint(String name, int operator, RetrieveDesc foreignConstraint, String foreignFieldName)
        {
            if (logger.isLoggable(Logger.FINEST))
                logger.finest("LOG_JQLCDumpRD", //NOI18N
                    JQLAST.getRetrieveDescRepr(this) + ".addConstraint(" +  //NOI18N
                    name + ", " + operator + ", " + JQLAST.getRetrieveDescRepr(foreignConstraint) + //NOI18N
                    ", " +  foreignFieldName + ")"); //NOI18N
            foreignConstraint = unwrap(foreignConstraint);
            wrapped.addConstraint(name, operator, foreignConstraint, foreignFieldName); 
        }
        
        public void setNavigationalId(Object navigationalId)
        { 
            if (logger.isLoggable(Logger.FINEST))
                logger.finest("LOG_JQLCDumpRD", //NOI18N
                    JQLAST.getRetrieveDescRepr(this) + 
                    ".setNavigationalId(" + navigationalId + ")"); //NOI18N
            wrapped.setNavigationalId(navigationalId); 
        }

        public void setPrefetchEnabled(boolean prefetchEnabled)
        {
            if (logger.isLoggable(Logger.FINEST))
                logger.finest("LOG_JQLCDumpRD", //NOI18N
                    JQLAST.getRetrieveDescRepr(this) +
                    ".setPrefetchEnabled(" + prefetchEnabled + ")"); //NOI18N
            wrapped.setPrefetchEnabled(prefetchEnabled);
        }

        // Methods from ActionDesc

        public Class getPersistenceCapableClass()
        { return wrapped.getPersistenceCapableClass(); }
    }
}

// rules

query
    :   q:.
        {
            prepareRetrieveDescs(q);
            if (logger.isLoggable(Logger.FINEST)) 
                logger.finest("LOG_JQLCDumpTree", q.getTreeRepr("RD annotated AST")); //NOI18N
            doCodeGen(q);
        }
    ;

doCodeGen
{ 
    boolean distinct = false;
}
    :   #(  q:QUERY
            candidateClass 
            parameters
            variables
            ordering
            distinct = result
            filter
        )
        {
            handleDistinct(q, distinct);
        }
    ;

// ----------------------------------
// rules: candidate class
// ----------------------------------

candidateClass
{   
    errorMsg.setContext("setCandidates"); //NOI18N
}
    :   c:CLASS_DEF
        // Note, DISTINCT is added by handleDistinct called in the rule query
    ;
// ----------------------------------
// rules: parameter declaration
// ----------------------------------

parameters
{   
    errorMsg.setContext("declareParameters"); //NOI18N
}
    :   ( declareParameter )*
    ;

declareParameter
    :   #( PARAMETER_DEF type IDENT )
    ;

// ----------------------------------
// rules: variable declaration
// ----------------------------------

variables 
{ 
    errorMsg.setContext("declareVariables"); //NOI18N
}
    :   ( declareVariable )*
    ;

declareVariable
    :   #( VARIABLE_DEF type i:IDENT )
    ;

// ----------------------------------
// rules: ordering specification
// ----------------------------------

ordering
{   
    errorMsg.setContext("setOrdering"); //NOI18N
}
    :   ( orderSpec )*
    ;

orderSpec
{
    int op = 0;
}
    :   #(  ORDERING_DEF 
            (   ASCENDING  { op = RetrieveDesc.OP_ORDERBY; }
            |   DESCENDING { op = RetrieveDesc.OP_ORDERBY_DESC; }
            )
            orderingExpr[op]
        )
    ;

orderingExpr [int op]
    :   ( #(  FIELD_ACCESS expression IDENT) )=> #(  f:FIELD_ACCESS expression i:IDENT )
        {
            f.getRetrieveDesc().addConstraint(i.getText(), op, null);
        }
    |   e:.
        {
            errorMsg.unsupported(e.getLine(), e.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.codegeneration.generic.unsupportedop", // NOI18N
                    e.getText()));
        }
    ;

// ----------------------------------
// rules: result expression
// ----------------------------------

result returns [boolean distinct]
{   
    errorMsg.setContext("setResult"); //NOI18N
    distinct = false;
}
    :   #(  RESULT_DEF 
            distinct = resultExpr[true]
        )
    |   {
            // no result is equivalent to setResult("distinct this") =>
            // distinct is true
            distinct = true;
        }
    ;

resultExpr [boolean outer] returns [boolean distinct]
{
    String name = null;
    // this should be take care at first level of recursion
    distinct = false;
    boolean tmp;
}
    :   #( d:DISTINCT tmp = resultExpr[outer] )
        {
            distinct = true;
        }
    |   #( avg:AVG distinct = resultExpr[true] )
        {
            candidateRD.addResult(RetrieveDesc.OP_AVG,
                avg.getJQLType().getEnumType());
        }
    |   #( max:MAX distinct = resultExpr[true] )
        {
            candidateRD.addResult(RetrieveDesc.OP_MAX,
                max.getJQLType().getEnumType());
        }
    |   #( min:MIN distinct = resultExpr[true] )
        {
            candidateRD.addResult(RetrieveDesc.OP_MIN,
                min.getJQLType().getEnumType());
        }
    |   #( sum:SUM distinct = resultExpr[true] )
        {
            candidateRD.addResult(RetrieveDesc.OP_SUM,
                sum.getJQLType().getEnumType());
        }
    |   #( count:COUNT distinct = r:resultExpr[true] )
        {
            Type resultType = r.getJQLType();
            if (typetab.isPersistenceCapableType(resultType)) {
                List pkfields = ((ClassType)resultType).getKeyFieldNames();
                if (pkfields != null) {
                    candidateRD.addResult(RetrieveDesc.OP_COUNT_PC,
                        count.getJQLType().getEnumType());
                } else {
                    errorMsg.unsupported(r.getLine(), r.getColumn(),
                        I18NHelper.getMessage(messages,
                        "jqlc.codegeneration.resultexpr.missingpkfields", // NOI18N
                        resultType.getName()));
                }
            } else {
                candidateRD.addResult(RetrieveDesc.OP_COUNT,
                    count.getJQLType().getEnumType());
            }
        }
    |   #(  op1:FIELD_ACCESS tmp = expr1:resultExpr[false] i1:IDENT )
        {
            op1.getRetrieveDesc().addResult(i1.getText(), null, true);
        }
    |   #(  op2:NAVIGATION tmp = expr2:resultExpr[false] i2:IDENT )
        {
            RetrieveDesc from = expr2.getRetrieveDesc();
            RetrieveDesc to = op2.getRetrieveDesc();
            from.addResult(i2.getText(), to, outer);
        }
    |   #(  op3:VARIABLE ( name = col3:collectionExprResult )? )
        {
            if (col3 != null) {
                RetrieveDesc from = col3.getRetrieveDesc();
                RetrieveDesc to = op3.getRetrieveDesc();
                from.addResult(name, to, outer);
            }
        }
    |   THIS
    ;

collectionExprResult returns [String fieldName]
{   
    fieldName = null; 
    boolean tmp;
}
    :   #( FIELD_ACCESS tmp = resultExpr[false] name1:IDENT )
        {  fieldName = name1.getText(); }
    |   #( NAVIGATION tmp = resultExpr[false] name2:IDENT )
        {  fieldName = name2.getText(); }
    |   #( TYPECAST . fieldName = collectionExprResult )
    |   #( NOT_IN fieldName = collectionExprResult )
    ;

// ----------------------------------
// rules: filer expression
//
// NOTE: the code generator traverses operands of binary operations in reverse order.
// The reason is that the RetrieveDesc processes the constriant stack in a LIFO way. 
// This means, the code generator has to process the right operand first, 
// then the left operand and finally the operation.
// ----------------------------------

filter
{   
    errorMsg.setContext("setFilter"); //NOI18N
}
    :   #( FILTER_DEF expr:. )
        {
            switch (expr.getType()) {
            case VALUE:
                // constant filter
                Object value = expr.getValue();
                if (value instanceof Boolean) 
                {
                    // Note, in the case of a true filter do not add
                    // any constraints to the candidateRD

                    if (!((Boolean)value).booleanValue())
                    {
                        // false filter => unset candidateRD
                        candidateRD = null;
                    }
                }
                else
                {
                    errorMsg.fatal(I18NHelper.getMessage(messages,
                        "jqlc.codegeneration.filter.nonbooleanvalue", //NOI18N
                        String.valueOf(value)));
                }
                break;
            case FIELD_ACCESS:
                // The entire filter consists of a boolean field only.
                // Map this to 'booleanField <> FALSE'. Note, the runtime will
                // create a JDBC parameter for the literal FALSE and call 
                // setBoolean to bind the value.
                RetrieveDesc rd = expr.getRetrieveDesc();
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, Boolean.FALSE);
                expression(expr);
                rd.addConstraint(null, RetrieveDesc.OP_NE, null);
                break;
            default:
                expression(expr);
                break;
            }
        }
    ;

expression 
    :   ( primary )=> primary
    |   bitwiseExpr
    |   conditionalExpr
    |   relationalExpr
    |   binaryArithmeticExpr
    |   unaryArithmeticExpr
    |   complementExpr
    ;

// This rule transforms an access expression of a boolean field into an
// equal operation: expr == true.
booleanOperationArgument
    : e:expression
      {
           if (#e.getType() == FIELD_ACCESS) {
               RetrieveDesc rd = #e.getRetrieveDesc();
               rd.addConstraint(null, RetrieveDesc.OP_VALUE, Boolean.TRUE);
               rd.addConstraint(null, RetrieveDesc.OP_EQ, null);
           }
      }
    ;

bitwiseExpr 
    :   #( op1:BAND left1:. right1:booleanOperationArgument )
        {  
            booleanOperationArgument(left1);
            // do not generate boolean operation if one of the operands is variable constraint
            if (pushesValueOnStack(left1) && pushesValueOnStack(right1))
                op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_AND, null); 
        }
    |   #( op2:BOR  left2:. right2:booleanOperationArgument )
        {
            booleanOperationArgument(left2);
            // do not generate boolean operation if one of the operands is variable constraint
            if (pushesValueOnStack(left2) && pushesValueOnStack(right2))
                op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_OR, null); 
        }
    |   #( op3:BXOR left3:. right3:booleanOperationArgument )
        {  
            booleanOperationArgument(left3);
            errorMsg.unsupported(op3.getLine(), op3.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.codegeneration.generic.unsupportedop", // NOI18N
                    op3.getText()));
        }
    ;

conditionalExpr 
    :   #( op1:AND left1:. right1:booleanOperationArgument )
        {  
            booleanOperationArgument(left1);
            // do not generate boolean operation if one of the operands is variable constraint
            if (pushesValueOnStack(left1) && pushesValueOnStack(right1))
                op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_AND, null); 
        }
    |   #( op2:OR  left2:. right2:booleanOperationArgument )
        {    
            booleanOperationArgument(left2);
            // do not generate boolean operation if one of the operands is variable constraint
            if (pushesValueOnStack(left2) && pushesValueOnStack(right2))
                op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_OR, null); 
        }
    ;

relationalExpr 
    :   ( fieldComparison )=> fieldComparison
    |   ( objectComparison )=> objectComparison
    |   ( collectionComparison )=> collectionComparison
    |   ( parameterComparison )=> parameterComparison
    |   #( op1:EQUAL left1:. expression )
        {  
            expression(left1);
            op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_EQ, null); 
        }
    |   #( op2:NOT_EQUAL left2:. expression )
        {  
            expression(left2);
            op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_NE, null); 
        }
    |   #( op3:LT left3:. expression )
        {  
            expression(left3);
            op3.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_LT, null); 
        }
    |   #( op4:GT left4:. expression )
        {  
            expression(left4);
            op4.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_GT, null); 
        }
    |   #( op5:LE left5:. expression )
        {  
            expression(left5);
            op5.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_LE, null); 
        }
    |   #( op6:GE left6:. expression )
        {  
            expression(left6);
            op6.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_GE, null); 
        }
    ;

fieldComparison
    :   #(EQUAL fieldComparisonOperands[RetrieveDesc.OP_EQ] )
    |   #(NOT_EQUAL fieldComparisonOperands[RetrieveDesc.OP_NE] )
    |   #(LT fieldComparisonOperands[RetrieveDesc.OP_LT] )
    |   #(LE fieldComparisonOperands[RetrieveDesc.OP_LE] )
    |   #(GT fieldComparisonOperands[RetrieveDesc.OP_GT] )
    |   #(GE fieldComparisonOperands[RetrieveDesc.OP_GE] )
    ;

fieldComparisonOperands [int operation]
{   
    String leftName = null;
    String rightName = null;
    Object value = null;
}
    :   value = constantValue rightName = f1:fieldAccess
        {
            // case constant relop field
            generateSimpleFieldValueComparison(f1.getRetrieveDesc(), rightName, 
                                               getSwappedOp(operation), value);
        }
    |   p1:PARAMETER rightName = f2:fieldAccess
        {
            // case parameter relop field
            // Support for fixed-width char pk columns
            f2.getRetrieveDesc().addConstraint(rightName, RetrieveDesc.OP_FIELD, null); 
            f2.getRetrieveDesc().addConstraint(null,
                RetrieveDesc.OP_PARAMETER, 
                paramtab.getParameterInfoForParamName(p1.getText(), rightName));
            f2.getRetrieveDesc().addConstraint(null, operation, null);
        }
    |   leftName = f3:fieldAccess 
        (   value = constantValue 
            {
                // case field relop constant
                generateSimpleFieldValueComparison(f3.getRetrieveDesc(), leftName, 
                                                   operation, value);
            }
        |   rightName = f4:fieldAccess 
            {
                // case field relop field
                f3.getRetrieveDesc().addConstraint(leftName, operation, 
                                                   f4.getRetrieveDesc(), rightName);
            }
        |   p2:PARAMETER
            {
                // case field relop parameter
                // Support for fixed-width char pk columns
                f3.getRetrieveDesc().addConstraint(null,
                    RetrieveDesc.OP_PARAMETER, 
                    paramtab.getParameterInfoForParamName(p2.getText(), leftName));
                f3.getRetrieveDesc().addConstraint(leftName, RetrieveDesc.OP_FIELD, null);
                f3.getRetrieveDesc().addConstraint(null, operation, null);
            }
        )
    ;

objectComparison
{
    Object value = null;
}
    :   #( OBJECT_EQUAL objectComparisonOperands[RetrieveDesc.OP_EQ] ) 
    |   #( OBJECT_NOT_EQUAL objectComparisonOperands[RetrieveDesc.OP_NE] )
    ;

objectComparisonOperands [int operation]
{   
    Object value = null;
}
    :   value = v1:constantValue d1:dbValue
        // case constant relop dbvalue
        {
            if ((value == null) && (d1.getType() == NAVIGATION))
            {
                JQLAST expr = (JQLAST)d1.getFirstChild();
                JQLAST ident = (JQLAST)expr.getNextSibling();
                // now handle navigation source
                expression(expr);
                // now generate IS NULL constraint
                generateSimpleFieldValueComparison(expr.getRetrieveDesc(), ident.getText(), 
                                                   getSwappedOp(operation), value);
            }
            else
            {
                if (d1.getType() == NAVIGATION) navigation(d1);
                generateDbValueConstantComparison(d1.getRetrieveDesc(), (ClassType)d1.getJQLType(), 
                                                  getSwappedOp(operation), value, v1.getJQLType());
            }
        }
    |   d2:dbValue
        (   value = v2:constantValue 
            // case dbvalue relop constant
            {
                if ((value == null) && (d2.getType() == NAVIGATION))
                {
                    JQLAST expr = (JQLAST)d2.getFirstChild();
                    JQLAST ident = (JQLAST)expr.getNextSibling();
                    // now handle navigation source
                    expression(expr);
                    // now generate IS NULL constraint
                    generateSimpleFieldValueComparison(expr.getRetrieveDesc(), ident.getText(), 
                                                       operation, value);
                }
                else
                {
                    if (d2.getType() == NAVIGATION) navigation(d2);
                    generateDbValueConstantComparison(d2.getRetrieveDesc(), 
                                                      (ClassType)d2.getJQLType(), 
                                                      operation, value, v2.getJQLType());
                }
            }
        |   d3:dbValue
            // case dbvalue relop dbvalue
            {
                if (d2.getType() == NAVIGATION) navigation(d2);
                if (d3.getType() == NAVIGATION) navigation(d3);
                generateDbValueDbValueComparison(d2.getRetrieveDesc(), 
                                                 (ClassType)d2.getJQLType(), 
                                                 operation, 
                                                 d3.getRetrieveDesc(), 
                                                 (ClassType)d3.getJQLType());
            }
        )
    ;

parameterComparison
    :   #(EQUAL parameterComparisonOperands[RetrieveDesc.OP_EQ] )
    |   #(NOT_EQUAL parameterComparisonOperands[RetrieveDesc.OP_NE] )
    |   #(OBJECT_EQUAL parameterComparisonOperands[RetrieveDesc.OP_EQ] )
    |   #(OBJECT_NOT_EQUAL parameterComparisonOperands[RetrieveDesc.OP_NE] )
    ;

parameterComparisonOperands [int operation]
{
    Object value = null;
}
    :   p1:PARAMETER value = v1:constantValue
        {
            generateParameterValueComparison(v1.getRetrieveDesc(), p1.getText(),
                operation, value);
        }
    |   value = v2:constantValue p2:PARAMETER
        {
            generateParameterValueComparison(v2.getRetrieveDesc(), p2.getText(),
                operation, value);
        }
    ;

dbValue
{
    String name = null;
}
    :   THIS
    |   variableAccess
    |   #(  NAVIGATION . IDENT )   
        // do not use non-terminal navigation here, because navigation 
        // creates a RetrieveDesc for the relationship navigation and 
        // we must not create this in the case of relship == null
    ;

collectionComparison
    :   #( eq:COLLECTION_EQUAL . . )
        {
            errorMsg.unsupported(eq.getLine(), eq.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.codegeneration.collectioncomparison.nonnull")); // NOI18N
        }
    |   #( ne:COLLECTION_NOT_EQUAL . . )
        {
            errorMsg.unsupported(ne.getLine(), ne.getColumn(),
                I18NHelper.getMessage(messages, 
                    "jqlc.codegeneration.collectioncomparison.nonnull")); // NOI18N
        }
    ;

binaryArithmeticExpr 
    :   #( op1:PLUS left1:. right1:. )
        {
            // Optimize indexOf + <intValue>: 
            // The SQL database returns an index starting with 1, so we need 
            // to decrement the returned index. We can do the derement at compile
            // timeCombine, if the other operand is a constant int value.
            if ((left1.getType() == INDEXOF) && 
                (right1.getType() == VALUE) && 
                (right1.getValue() instanceof Integer))
            {
                // case: indexOf() + intValue
                indexOf(left1, ((Integer)right1.getValue()).intValue());
            }
            else if ((right1.getType() == INDEXOF) && 
                (left1.getType() == VALUE) && 
                (left1.getValue() instanceof Integer))
            {
                // case: intValue + indexOf()
                indexOf(right1, ((Integer)left1.getValue()).intValue());
            }
            else
            {
                expression(right1);
                expression(left1);
                op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_ADD, null); 
            }
        }
    |   #( op2:CONCAT left2:. expression )
        {
            expression(left2);
            op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_CONCAT, null); 
        }
    |   #( op3:MINUS left3:. right3:. )
        {
            // Optimize indexOf + <intValue>: 
            // The SQL database returns an index starting with 1, so we need 
            // to decrement the returned index. We can do the derement at compile
            // timeCombine, if the other operand is a constant int value.
            if ((left3.getType() == INDEXOF) && 
                (right3.getType() == VALUE) && 
                (right3.getValue() instanceof Integer))
            {
                // case: indexOf - intValue 
                // treated as indexOf + -intValue
                indexOf(left3, -((Integer)right3.getValue()).intValue());
            }
            else
            {
                expression(right3);
                expression(left3);
                op3.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_SUB, null); 
            }
        }
    |   #( op4:STAR left4:. expression )
        {
            expression(left4);
            op4.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_MUL, null); 
        }
    |   #( op5:DIV left5:. expression )
        {
            expression(left5);
            op5.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_DIV, null); 
        }
    |   #( op6:MOD left6:. expression )
        {
            expression(left6);
            op6.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_MOD, null);
        }
    ;

unaryArithmeticExpr 
{   
    Object value = null; 
}
    :   #(   UNARY_PLUS expression )
        // no action needed, just ignore the unary plus
    |   #(   op2:UNARY_MINUS
            (
                ( constantValue )=> value = constantValue
                {
                    value = negate(value, op2.getJQLType());
                    op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, value);
                }
            |
                expression 
                { 
                    // map -value to 0 - value
                    op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, 
                                                       getZeroValue(op2.getJQLType()));
                    op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_SUB, null); 
                }
            )
        )
    ;

complementExpr 
    :   #( op1:BNOT expression )
        { 
            // map ~value to -1 - value (which is equivalent to (-value)-1)
            op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, 
                                                getMinusOneValue(op1.getJQLType()));
            op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_SUB, null); 
        }
    |   #(  op2:LNOT expr:. )
        {   if (expr.getType() == FIELD_ACCESS) {
                // The NOT operand is a boolean field.
                // Map this to 'booleanField = FALSE'. Note, the runtime will
                // create a JDBC parameter for the literal FALSE and call 
                // setBoolean to bind the value.
                RetrieveDesc rd = op2.getRetrieveDesc();
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, Boolean.FALSE); 
                expression(expr);
                rd.addConstraint(null, RetrieveDesc.OP_EQ, null);
            }
            else {
                expression(expr);
                op2.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_NOT, null); 
            }
        }
    ;

primary 
{   
    Object value; 
    String name;
}
    :   #( TYPECAST type expression )
        { /* code gen for cast? */ }
    |   value = v:constantValue
        {
            if (value == null)
            {
                errorMsg.fatal(I18NHelper.getMessage(messages, "jqlc.codegeneration.primary.null")); //NOI18N
            }
            else if (value instanceof Boolean)
            {
                boolean booleanValue = ((Boolean)value).booleanValue();
                RetrieveDesc rd = v.getRetrieveDesc();
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, new Integer(0)); 
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, new Integer(0)); 
                rd.addConstraint(null, (booleanValue?RetrieveDesc.OP_EQ:RetrieveDesc.OP_NE), null); 
            }
            else
            {
                v.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, value); 
            }
        }
    |   p:PARAMETER 
        {
            p.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_PARAMETER, 
                paramtab.getParameterInfoForParamName(p.getText()));
        }
    |   THIS
    |   name = f:fieldAccess
        {
            f.getRetrieveDesc().addConstraint(name, RetrieveDesc.OP_FIELD, null);
        }
    |   navigation
    |   variableAccess
    |   #(  CONTAINS . VARIABLE )
        // code moved to variable access
    |   #(  NOT_CONTAINS . VARIABLE )
        // code moved to variable access
    |   startsWith
    |   endsWith
    |   isEmpty
    |   like
    |   substring
    |   indexOf[0]
    |   length
    |   abs
    |   sqrt
    ;

constantValue returns [Object value]
{   
    value = null; 
}
    :   v:VALUE
        {
            value = v.getValue();
        }
    ;


fieldAccess returns [String fieldName]
{   
    fieldName = null; 
}
    :   #( FIELD_ACCESS expression name:IDENT )
        {  fieldName = name.getText(); }
    ;

navigation
    :   #(  n:NAVIGATION expr:expression i:IDENT )
        {
            RetrieveDesc from = expr.getRetrieveDesc();
            RetrieveDesc to = n.getRetrieveDesc();
            from.addConstraint(i.getText(), to);
        }
    ;

variableAccess
{
    String name = null;
}
    :   #(  var:VARIABLE ( name = col:collectionExpr )? )
        {
            
            RetrieveDesc varRD = var.getRetrieveDesc();
            if (!boundRetrieveDescs.contains(varRD))
            {
                if (col != null) 
                {
                    if (col.getType() == NOT_IN)
                        col.getRetrieveDesc().addConstraint(name, RetrieveDesc.OP_NOTIN, varRD);
                    else if (USE_IN)
                        // generate OP_IN if USE_IN property is set
                        col.getRetrieveDesc().addConstraint(name, RetrieveDesc.OP_IN, varRD);
                    else
                        // otherwise generate regular join
                        col.getRetrieveDesc().addConstraint(name, varRD);
                }
                else
                {
                    candidateRD.addConstraint(null, varRD);
                }
                boundRetrieveDescs.add(varRD);
            }
        }
    ;

collectionExpr returns [String fieldName]
{   
    fieldName = null; 
}
    :   #( FIELD_ACCESS expression name1:IDENT )
        {  fieldName = name1.getText(); }
    |   #( NAVIGATION expression name2:IDENT )
        {  fieldName = name2.getText(); }
    |   #( TYPECAST . fieldName = collectionExpr )
    |   #( NOT_IN fieldName = collectionExpr )
    ;

startsWith
{
    Object value = null;
    JQLAST pattern = null;
}
    :   #(  op1:STARTS_WITH string:. 
            {
                // I need to store a pointer to the second operand of startsWith here.
                // See second alternative below.
                pattern = (JQLAST)string.getNextSibling();
            }
            ( 
                ( constantValue )=> value = constantValue
                {
                    if (string.getType() == FIELD_ACCESS)
                    {
                        // case 1 fieldAccess constantValue
                        String fieldName = fieldAccess(string);
                        op1.getRetrieveDesc().addConstraint(fieldName, RetrieveDesc.OP_LIKE, 
                            ((String)value) + WILDCARD_PATTERN);
                    }
                    else
                    {
                        // case 2 expression constantValue
                        op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, 
                            ((String)value) + WILDCARD_PATTERN);
                        expression(string);
                        op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_LIKE, null);
                    }
                }
            |   {
                    // I have to access the tree matched by rule expression before 
                    // the rule is entered. Variable pattern points to that tree and 
                    // needs to be initilaized before!
                    pattern.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, 
                                                            WILDCARD_PATTERN);
                }
                expression
                {
                    // case 3 expression expression
                    pattern.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_CONCAT, null);
                    expression(string);
                    op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_LIKE, null);
                }
            )
        )
    ;

endsWith
{
    Object value = null;
}
    :   #(  op1:ENDS_WITH string:. 
            ( 
                ( constantValue )=> value = constantValue
                {
                    if (string.getType() == FIELD_ACCESS)
                    {
                        // case 1 fieldAccess constantValue
                        String fieldName = fieldAccess(string);
                        op1.getRetrieveDesc().addConstraint(fieldName, RetrieveDesc.OP_LIKE, 
                            WILDCARD_PATTERN + ((String)value));
                    }
                    else
                    {
                        // case 2 expression constantValue
                        op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, 
                            WILDCARD_PATTERN + ((String)value));
                        expression(string);
                        op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_LIKE, null);
                    }
                }
            |   pattern:expression
                {
                    // case 3 expression expression
                    pattern.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE,
                                                            WILDCARD_PATTERN);
                    pattern.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_CONCAT, null);
                    expression(string);
                    op1.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_LIKE, null);
                }
            )
        )
    ;

isEmpty
{ 
    String name = null;
}
    :   #(op:IS_EMPTY name = collectionExpr)
        {
            op.getRetrieveDesc().addConstraint(name, RetrieveDesc.OP_NULL, null);
        }
    ;

like
{   
    int opCode = RetrieveDesc.OP_LIKE;
}
    :   #( op:LIKE string:. pattern:. opCode = escape ) 
        {
            expression(pattern);
            expression(string);
            op.getRetrieveDesc().addConstraint(null, opCode, null);
        }
    ;

escape returns [int opCode]
{   
    // The default is no ESCAPE definition => OP_LIKE
    opCode = RetrieveDesc.OP_LIKE;
}
    :   expression
        {
            opCode = RetrieveDesc.OP_LIKE_ESCAPE;
        }
    |   // empty rule
    ;

substring
    :   // JDOQL:        string.substring(begin, end) ->
        // RetrieveDesc: SUBSTRING(string, begin + 1, end - begin)
        #( op:SUBSTRING string:. begin:. end:. ) 
        {
            RetrieveDesc rd = op.getRetrieveDesc();
            if ((begin.getType() == VALUE) && (end.getType() == VALUE))
            {
                // Optimization: begin and end are constant values =>
                // calculate start and length of SQL SUBSTRING function 
                // at compile time.
                // Note, Semantic ensures begin and end are int or Integer values.
                int beginValue = (begin.getValue() != null) ? 
                    ((Integer)begin.getValue()).intValue() : 0;
                int endValue = (end.getValue() != null) ? 
                    ((Integer)end.getValue()).intValue() : 0;
                if (beginValue < 0) 
                {
                    errorMsg.error(begin.getLine(), begin.getColumn(),
                        I18NHelper.getMessage(messages, 
                            "jqlc.codegeneration.substring.beginnegative", // NOI18N
                            String.valueOf(beginValue)));
                }
                else if (endValue < beginValue) 
                {
                    errorMsg.error(op.getLine(), op.getColumn(),
                        I18NHelper.getMessage(messages, 
                            "jqlc.codegeneration.substring.beginlargerend", // NOI18N
                            String.valueOf(beginValue), String.valueOf(endValue)));
                }
                // SQL length = end - begin
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, 
                    new Integer(endValue-beginValue));
                // SQL start index = begin + 1 
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, 
                    new Integer(beginValue+1));
            }
            else
            {
                // At least one of begin or end is a non constant value =>
                // generate the arguments start and length of the SQL SUBSTRING 
                // function as binary plus/minus expressions.
                // The next 3 line denote the SQL length = end - begin
                expression(begin);
                expression(end);
                rd.addConstraint(null, RetrieveDesc.OP_SUB, null);
                // The next 3 lines denote the SQL start index = begin + 1 
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, new Integer(1));
                expression(begin);
                rd.addConstraint(null, RetrieveDesc.OP_ADD, null);
            }
            // now push the string on the constraint stack
            expression(string);
            rd.addConstraint(null, RetrieveDesc.OP_SUBSTRING, null);
        }
    ;

// incr denotes the value that need to be added to result of POSITION
indexOf [int incr]
{
    int opCode = RetrieveDesc.OP_POSITION;
}
    :   // JDOQL:        string.indexOf(pattern) ->
        // RetrieveDesc: POSITION(string, pattern) - 1
        // JDOQL:        string.indexOf(pattern, begin) ->
        // RetrieveDesc: POSITION_START(string, pattern, begin + 1) - 1
        #( op:INDEXOF string:. pattern:. opCode = fromIndex ) 
        {
            RetrieveDesc rd = op.getRetrieveDesc();
            // the 3 lines denote the SQL function POSITION OR POSITION_START
            expression(pattern);
            expression(string);
            rd.addConstraint(null, opCode, null);
            // SQL handles indexes starting from 1 =>
            // decrement the returned value to make it Java like!
            incr--;
            if (incr != 0)
            {
                rd.addConstraint(null, RetrieveDesc.OP_VALUE, new Integer(incr));
                rd.addConstraint(null, RetrieveDesc.OP_ADD, null);
            }
        }
    ;

fromIndex returns [int opCode]
{
    // The default is no start definition => OP_POSITION
    opCode = RetrieveDesc.OP_POSITION;
}
    :   e:.
        {
            opCode = RetrieveDesc.OP_POSITION_START;
            // Java indexOf method use indexes starting with 0, 
            // where SQL starts with 1, so we need to add 1
            if (e.getType() == VALUE)
            {
                // Optimization: calulate index at compile time, 
                // if start is a constant value.
                // Note, Semantic ensures begin and end are int or Integer values.
                int value = (e.getValue() != null) ? 
                    ((Integer)e.getValue()).intValue() : 0;
                e.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, 
                    new Integer(value + 1));
            }
            else
            {
                e.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_VALUE, 
                    new Integer(1));
                expression(e);
                e.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_ADD, null);
            }
        }
    |   // empty rule
    ;

length
    :   #( op:LENGTH expression )
        {
            op.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_LENGTH, null);
        }
    ;

abs
    :   #( op:ABS expression )
        {
            op.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_ABS, null);
        }
    ;

sqrt
    :   #( op:SQRT expression )
        {
            op.getRetrieveDesc().addConstraint(null, RetrieveDesc.OP_SQRT, null);
        }
    ;

type
    :   TYPENAME
    |   primitiveType
    ;

primitiveType
    :   BOOLEAN
    |   BYTE
    |   CHAR
    |   SHORT
    |   INT
    |   FLOAT
    |   LONG
    |   DOUBLE
    ;

// ----------------------------------
// rules: RetrieveDesc handling
// ----------------------------------

prepareRetrieveDescs
{
    Map usedRD = new HashMap();
}
    :   #(  q:QUERY
            checkRetrieveDesc[usedRD] // candidate class
            ( #( PARAMETER_DEF . . ) )*
            ( #( VARIABLE_DEF . . ) )*
            ( #( ORDERING_DEF 
                    ( ASCENDING | DESCENDING ) 
                    ordering:checkRetrieveDesc[usedRD]
                    { propagateRetrieveDesc(ordering, candidateRD); }
                )
            )*
            ( #( RESULT_DEF result:checkRetrieveDesc[usedRD]
                    { propagateRetrieveDesc(result, candidateRD); }
                )
            )?
            #( FILTER_DEF 
                filter:checkRetrieveDesc[usedRD]
                { propagateRetrieveDesc(filter, candidateRD); }
            )
        )
    ;

checkRetrieveDesc [Map usedRD]
    :   c:CLASS_DEF
        {
            // check persistence capable
            ClassType candidateClass = (ClassType)c.getJQLType();
            candidateRD = createRetrieveDesc("this", candidateClass); //NOI18N
            candidateRD.setPrefetchEnabled(prefetchEnabled);
        }
    |   #( cast:TYPECAST type expr1:checkRetrieveDesc[usedRD] )
        {
            cast.setRetrieveDesc(expr1.getRetrieveDesc());
        }

    //  constantValue not necessary here, this is covered by the last rule 

    |   t:THIS
        { 
            t.setRetrieveDesc(candidateRD); 
        }
    |   #(var:VARIABLE ( checkRetrieveDesc[usedRD] )? )
        {
            RetrieveDesc to = (RetrieveDesc)usedRD.get(var.getText());
            if (to == null)
            {
                to = createRetrieveDesc(var.getText(), (ClassType)var.getJQLType());
                usedRD.put(var.getText(), to);
            }
            var.setRetrieveDesc(to);
        }
    |   #(notIn:NOT_IN notInArg:checkRetrieveDesc[usedRD])
        {
            #notIn.setRetrieveDesc(#notInArg.getRetrieveDesc());
        }
    |   #(fa:FIELD_ACCESS expr4:checkRetrieveDesc[usedRD] i:IDENT)
        {
            fa.setRetrieveDesc(expr4.getRetrieveDesc());
            i.setRetrieveDesc(expr4.getRetrieveDesc());
        }
    |   #(n:NAVIGATION checkRetrieveDesc[usedRD] IDENT)
        {
            RetrieveDesc to = (RetrieveDesc)usedRD.get(n.getText());
            if (to == null)
            {
                to = createRetrieveDesc(n.getText(), (ClassType)n.getJQLType());
                usedRD.put(n.getText(), to);
            }
            n.setRetrieveDesc(to);
        }
    |   #(CONTAINS checkRetrieveDesc[usedRD] checkRetrieveDesc[usedRD])
    |   #(NOT_CONTAINS checkRetrieveDesc[usedRD] checkRetrieveDesc[usedRD])
    |   #(sw:STARTS_WITH expr7:checkRetrieveDesc[usedRD] checkRetrieveDesc[usedRD])
        {
            sw.setRetrieveDesc(expr7.getRetrieveDesc());
        }
    |   #(ew:ENDS_WITH expr8:checkRetrieveDesc[usedRD] checkRetrieveDesc[usedRD])
        {
            ew.setRetrieveDesc(expr8.getRetrieveDesc());
        }
    |   #(ie:IS_EMPTY expr9:checkRetrieveDesc[usedRD])
        {
            ie.setRetrieveDesc(expr9.getRetrieveDesc());
        }
    |   #(like:LIKE string10:checkRetrieveDesc[usedRD] 
            pattern10:checkRetrieveDesc[usedRD] ( escape10:checkRetrieveDesc[usedRD] )? )
        {
            like.setRetrieveDesc(getCommonRetrieveDesc(string10, pattern10, escape10));
        }
    |   #(substr:SUBSTRING string11:checkRetrieveDesc[usedRD] 
            lower11:checkRetrieveDesc[usedRD] upper11:checkRetrieveDesc[usedRD] )
        {
            substr.setRetrieveDesc(getCommonRetrieveDesc(string11, lower11, upper11));
        }
    |   #(indexOf:INDEXOF string12:checkRetrieveDesc[usedRD] 
            pattern12:checkRetrieveDesc[usedRD] ( start12:checkRetrieveDesc[usedRD] )? )
        {
            indexOf.setRetrieveDesc(getCommonRetrieveDesc(string12, pattern12, start12));
        }
    |   #(len:LENGTH expr13:checkRetrieveDesc[usedRD])
        {
            len.setRetrieveDesc(expr13.getRetrieveDesc());
        }
    |   #(abs:ABS expr14:checkRetrieveDesc[usedRD])
        {
            abs.setRetrieveDesc(expr14.getRetrieveDesc());
        }
    |   #(sqrt:SQRT expr15:checkRetrieveDesc[usedRD])
        {
            sqrt.setRetrieveDesc(expr15.getRetrieveDesc());
        }

        // binary operations

    |   #( op1:BAND      left1:. right1:. )
        {   checkAndOpRetrieveDesc(op1, left1, right1, usedRD); }
    |   #( op2:BOR       left2:checkRetrieveDesc[new HashMap(usedRD)] 
                         right2:checkRetrieveDesc[new HashMap(usedRD)] )
        { op2.setRetrieveDesc(getCommonRetrieveDesc(left2, right2)); }
    |   #( op3:BXOR      left3:checkRetrieveDesc[new HashMap()] 
                         right3:checkRetrieveDesc[new HashMap()] )
        { op3.setRetrieveDesc(getCommonRetrieveDesc(left3, right3)); }
    |   #( op4:AND      left4:. right4:. )
        {   checkAndOpRetrieveDesc(op4, left4, right4, usedRD); }
    |   #( op5:OR        left5:checkRetrieveDesc[new HashMap(usedRD)] 
                         right5:checkRetrieveDesc[new HashMap(usedRD)] )
        { op5.setRetrieveDesc(getCommonRetrieveDesc(left5, right5)); }
    |   #( op6:EQUAL     left6:checkRetrieveDesc[usedRD] right6:checkRetrieveDesc[usedRD] )
        { op6.setRetrieveDesc(getCommonRetrieveDesc(left6, right6)); }
    |   #( op7:NOT_EQUAL left7:checkRetrieveDesc[usedRD] right7:checkRetrieveDesc[usedRD] )
        { op7.setRetrieveDesc(getCommonRetrieveDesc(left7, right7)); }
    |   #( op8:LT        left8:checkRetrieveDesc[usedRD] right8:checkRetrieveDesc[usedRD] )
        { op8.setRetrieveDesc(getCommonRetrieveDesc(left8, right8)); }
    |   #( op9:GT        left9:checkRetrieveDesc[usedRD] right9:checkRetrieveDesc[usedRD] )
        { op9.setRetrieveDesc(getCommonRetrieveDesc(left9, right9)); }
    |   #( op10:LE        left10:checkRetrieveDesc[usedRD] right10:checkRetrieveDesc[usedRD] )
        { op10.setRetrieveDesc(getCommonRetrieveDesc(left10, right10)); }
    |   #( op11:GE        left11:checkRetrieveDesc[usedRD] right11:checkRetrieveDesc[usedRD] )
        { op11.setRetrieveDesc(getCommonRetrieveDesc(left11, right11)); }
    |   #( op12:OBJECT_EQUAL     left12:checkRetrieveDesc[usedRD] right12:checkRetrieveDesc[usedRD] )
        { op12.setRetrieveDesc(getObjectComparisonRetrieveDesc(left12, right12)); }
    |   #( op13:OBJECT_NOT_EQUAL left13:checkRetrieveDesc[usedRD] right13:checkRetrieveDesc[usedRD] )
        { op13.setRetrieveDesc(getObjectComparisonRetrieveDesc(left13, right13)); }
    |   #( op14:COLLECTION_EQUAL left14:checkRetrieveDesc[usedRD] right14:checkRetrieveDesc[usedRD] )
        { op14.setRetrieveDesc(getCommonRetrieveDesc(left14, right14)); }
    |   #( op15:COLLECTION_NOT_EQUAL left15:checkRetrieveDesc[usedRD] right15:checkRetrieveDesc[usedRD] )
        { op15.setRetrieveDesc(getCommonRetrieveDesc(left15, right15)); }
    |   #( op16:PLUS      left16:checkRetrieveDesc[usedRD] right16:checkRetrieveDesc[usedRD] )
        { op16.setRetrieveDesc(getCommonRetrieveDesc(left16, right16)); }
    |   #( op17:CONCAT    left17:checkRetrieveDesc[usedRD] right17:checkRetrieveDesc[usedRD] )
        { op17.setRetrieveDesc(getCommonRetrieveDesc(left17, right17)); }
    |   #( op18:MINUS     left18:checkRetrieveDesc[usedRD] right18:checkRetrieveDesc[usedRD] )
        { op18.setRetrieveDesc(getCommonRetrieveDesc(left18, right18)); }
    |   #( op19:STAR      left19:checkRetrieveDesc[usedRD] right19:checkRetrieveDesc[usedRD] )
        { op19.setRetrieveDesc(getCommonRetrieveDesc(left19, right19)); }
    |   #( op20:DIV       left20:checkRetrieveDesc[usedRD] right20:checkRetrieveDesc[usedRD] )
        { op20.setRetrieveDesc(getCommonRetrieveDesc(left20, right20)); }
    |   #( op21:MOD       left21:checkRetrieveDesc[usedRD] right21:checkRetrieveDesc[usedRD] )
        { op21.setRetrieveDesc(getCommonRetrieveDesc(left21, right21)); }

        // unary operations

    |   #( uop1:UNARY_PLUS  arg1:checkRetrieveDesc[usedRD] )
        {  uop1.setRetrieveDesc(arg1.getRetrieveDesc()); }
    |   #( uop2:UNARY_MINUS arg2:checkRetrieveDesc[usedRD] )
        {  uop2.setRetrieveDesc(arg2.getRetrieveDesc()); }
    |   #( uop3:BNOT        arg3:checkRetrieveDesc[usedRD] )
        {  uop3.setRetrieveDesc(arg3.getRetrieveDesc()); }
    |   #( uop4:LNOT        arg4:checkRetrieveDesc[usedRD] )
        {  uop4.setRetrieveDesc(arg4.getRetrieveDesc()); }
    |   #( d:DISTINCT arg5:checkRetrieveDesc[usedRD] )
        {  d.setRetrieveDesc(arg5.getRetrieveDesc()); }
    |   #( avg:AVG arg6:checkRetrieveDesc[usedRD] )
        {  avg.setRetrieveDesc(arg6.getRetrieveDesc()); }
    |   #( max:MAX arg7:checkRetrieveDesc[usedRD] )
        {  max.setRetrieveDesc(arg7.getRetrieveDesc()); }
    |   #( min:MIN arg8:checkRetrieveDesc[usedRD] )
        {  min.setRetrieveDesc(arg8.getRetrieveDesc()); }
    |   #( sum:SUM arg9:checkRetrieveDesc[usedRD] )
        {  sum.setRetrieveDesc(arg9.getRetrieveDesc()); }
    |   #( count:COUNT arg10:checkRetrieveDesc[usedRD] )
        {  count.setRetrieveDesc(arg10.getRetrieveDesc()); }
    |   .
    ; 
