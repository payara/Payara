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
 * Statement.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.generator;

import org.netbeans.modules.dbschema.ColumnElement;
import org.netbeans.modules.dbschema.TableElement;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.database.DBVendorType;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint.*;
import org.glassfish.persistence.common.I18NHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This class is used to represent a SQL statement.
 */
public abstract class Statement extends Object implements Cloneable {

    private static final Integer ONE = new Integer(1);

    protected static final int OP_PREFIX_MASK =     0x001; // 1

    protected static final int OP_INFIX_MASK =      0x002; // 2

    protected static final int OP_POSTFIX_MASK =    0x004; // 4

    protected static final int OP_PAREN_MASK =      0x008; // 8

    protected static final int OP_ORDERBY_MASK =    0x010; // 16

    protected static final int OP_WHERE_MASK =      0x020; // 32

    protected static final int OP_IRREGULAR_MASK =  0x040; // 64

    protected static final int OP_OTHER_MASK =      0x080; // 128

    protected static final int OP_PARAM_MASK =      0x100; // 256

    protected static final int OP_BINOP_MASK = 2 * OP_PARAM_MASK | OP_WHERE_MASK | OP_INFIX_MASK; // 546

    protected static final int OP_FUNC_MASK = OP_PARAM_MASK | OP_PREFIX_MASK | OP_PAREN_MASK | OP_WHERE_MASK; // 297

    protected static final int OP_PCOUNT_MASK = 3 * OP_PARAM_MASK; // 768

    protected StringBuffer statementText;

    private String quoteCharStart;

    private String quoteCharEnd;

    /** array of ColumnRef */
    protected ArrayList columns;

    Constraint constraint;

    protected InputDesc inputDesc;

    int action;

    /** array of QueryTable */
    public ArrayList tableList;

    protected DBVendorType vendorType;

    protected ArrayList secondaryTableStatements;

    /**
     * I18N message handler
     */
    protected final static ResourceBundle messages = I18NHelper.loadBundle(
           "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            Statement.class.getClassLoader());


    public Statement(DBVendorType vendorType) {

        inputDesc = new InputDesc();
        columns = new ArrayList();
        constraint = new Constraint();
        tableList = new ArrayList();
        this.vendorType = vendorType;

        if (vendorType.getQuoteSpecialOnly() == false) {
            // DO NOT SUPPORT QUOTING OTHERWISE
            this.quoteCharStart = vendorType.getQuoteCharStart();
            this.quoteCharEnd = vendorType.getQuoteCharEnd();
        }
    }

    public void addQueryTable(QueryTable table) {
        //
        // First check to make sure this is not a duplicate.
        //
        if (tableList.indexOf(table) == -1) {
            tableList.add(table);
        }
    }

    protected ColumnRef getColumnRef(ColumnElement columnElement) {
        //
        // Check whether this column has already been added.
        // If so, simply return.
        //
        int size = columns.size();

        for (int i = 0; i < size; i++) {
            ColumnRef cref = (ColumnRef) columns.get(i);

            if (cref.getColumnElement() == columnElement) return cref;
        }

        return null;
    }

    protected void addColumnRef(ColumnRef columnRef) {
        columnRef.setIndex(columns.size()+1);
        columns.add(columnRef);
    }

    /**
     * Adds a comparison on local field <CODE>lf</CODE> and value <CODE>value</CODE>.
     */
    public void addConstraint(LocalFieldDesc lf, Object value) {
        int operation;
        if (value == null) {
            operation = ActionDesc.OP_NULL;
        } else {
            constraint.addValue(value, lf);
            if (lf.isPrimitiveMappedToNullableColumn() ||
                       (vendorType.mapEmptyStringToNull() && lf.getType() == String.class)) {
                // Primitive fields mapped to nullable columns might hold
                // a default value indicting that the actual db column is null.
                // Databases might treat zero length Strings as null.
                operation = ActionDesc.OP_MAYBE_NULL;
            } else {
                // Object type fields or primitive fields
                // mapped to non nullable columns are compared exactly
                operation = ActionDesc.OP_EQ;
            }
        }
        constraint.addField(lf);
        constraint.addOperation(operation);
    }

    public DBVendorType getVendorType() {
        return vendorType;
    }

    public void appendTableText(StringBuffer text, QueryTable table) {
        appendQuotedText(text, table.getTableDesc().getName());
        text.append(" t"); // NOI18N
        text.append(table.getTableIndex());
    }

    /**
     * Append <code>text</code> surrounded by quote char to <code>buffer</code>
     * @param buffer The given buffer
     * @param text The given text
     */
    protected void appendQuotedText(StringBuffer buffer, String text) {
        buffer.append(quoteCharStart);
        buffer.append(text);
        buffer.append(quoteCharEnd);
    }

    /**
     * Returns the SQL text for the query described by this object.
     *
     * @return The text of the SQL query described by this object.
     */
    public String getText() {

        if (statementText == null) {
            generateStatementText();
        }


        return statementText.toString();
    }

    /**
     * Generates the SQL text for the query described by this object.
     */
    protected abstract void generateStatementText();

    /**
     * Processes the constraint stack and adds it to the query.
     * This means turning the constraint stack into SQL text and
     * adding it to the where clause.
     *
     * @return Where clause based on the constraint stack.
     */
    public StringBuffer processConstraints() {
        StringBuffer whereText = new StringBuffer();
        List stack = constraint.getConstraints();

        while (stack.size() > 0) {
            ConstraintNode node = (ConstraintNode) stack.get(stack.size() - 1);

            if (!(node instanceof ConstraintOperation)) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.generic.notinstanceof", // NOI18N
                        node.getClass().getName(), "ConstraintOperation")); // NOI18N
            }

            processRootConstraint((ConstraintOperation) node, stack, whereText);
        }

        return whereText;
    }

    protected void processRootConstraint(ConstraintOperation opNode,
                                         List stack,
                                         StringBuffer whereText) {
        int op = opNode.operation;
        int opInfo = operationFormat(op);

        if ((opInfo & OP_WHERE_MASK) > 0) {
            String constraint = getWhereText(stack);
            if (whereText.length() > 0 && constraint.length() > 0) {
                // This is neccessary, if the constraint stack is "un-balanced",
                // see OrderingTest#ordering006 for an example.
                whereText.append(" and ");
            }
            whereText.append(constraint);
        } else {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "sqlstore.sql.generator.statement.unexpectedconstraint", op)); // NOI18N
        }
    }

    /**
     * Get QueryPlan for this statement
     * @return QueryPlan for this statement
     */
    public abstract QueryPlan getQueryPlan();

    /**
     * Constructs the where clause for the statement from
     * the constraint stack.
     *
     * @param stack
     * 	The stack parameter holds the constraint stack to be decoded.
     *
     *  RESOLVE:  We don't support constraints on multiple statements yet.
     *  We would need to sort constraints out by statement and do something
     *  about constraints that span statements (e.g. t1.c1 = t2.c2).
     */
    protected String getWhereText(List stack) {

        StringBuffer result = new StringBuffer();
        ConstraintNode node;

        if (stack.size() == 0) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.stackempty")); // NOI18N
        }

        node = (ConstraintNode) stack.get(stack.size() - 1);
        stack.remove(stack.size() - 1);

        if (node instanceof ConstraintParamIndex) {
            processConstraintParamIndex((ConstraintParamIndex) node, result);
        } else if (node instanceof ConstraintValue) {
            processConstraintValue((ConstraintValue) node, result);
        } else if (node instanceof ConstraintField) {
            processConstraintField((ConstraintField) node, result);
        } else if (node instanceof ConstraintConstant) {
            result.append(((ConstraintConstant) node).value.toString());
        } else if (node instanceof ConstraintOperation) {
            processConstraintOperation((ConstraintOperation) node, stack, result);
        } else {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.illegalnode", // NOI18N
                    node.getClass().getName()));
        }

        return result.toString();
    }

    protected void processConstraintParamIndex(ConstraintParamIndex node, StringBuffer result) {
        // DB2 requires cast for parameter markers involved in numeric expressions.
        result.append(vendorType.getParameterMarker(node.getType()));
        Integer index = node.getIndex();
        inputDesc.values.add(new InputParamValue(index, getColumnElementForValueNode(node) ));
    }

    protected void processConstraintValue(ConstraintValue node, StringBuffer result) {
        boolean generateValueInSQLStatement = false;
        String strToAppend = "?"; // NOI18N

        // DB2 requires cast for parameter markers involved in numeric expressions.
        // For DB2, do not generate parameter markers, but inline values in generated SQL.
        // TODO: Inline numerics for all the databases not just DB2.

        if (vendorType.isInlineNumeric()) {
        // TODO: Ask Michael to pass on type for values also as enums.
        // Suggestion : Convert FieldTypeEnumeration to a class. Implement methods like
        // isNumeric(int type) for following if condition
            Object value = node.getValue();
            if (value != null && value instanceof Number) {
                generateValueInSQLStatement = true;
                strToAppend = value.toString();
            }
        }
        result.append(strToAppend);

        if(!generateValueInSQLStatement) {
            // We have added a "?" to sql. Add value to inputDesc
            generateInputValueForConstraintValueNode(node);
        }
    }

    protected QueryPlan getOriginalPlan(ConstraintField fieldNode) {
        return (fieldNode.originalPlan != null) ? fieldNode.originalPlan : getQueryPlan();
    }

    private void processConstraintField(ConstraintField fieldNode, StringBuffer result) {
        LocalFieldDesc desc = null;

        QueryPlan thePlan = getOriginalPlan(fieldNode);

        if (fieldNode instanceof ConstraintFieldDesc) {
            desc = ((ConstraintFieldDesc) fieldNode).desc;
        } else if (fieldNode instanceof ConstraintFieldName) {
            desc = thePlan.config.getLocalFieldDesc(((ConstraintFieldName) fieldNode).name);
        } else {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.generic.notinstanceof", // NOI18N
                    fieldNode.getClass().getName(),
                    "ConstraintFieldDesc/ConstraintFieldName")); // NOI18N
        }
        generateColumnText(desc, thePlan, result);
    }

    /**
     * Generates the column text for field <code>desc</code>.
     * The column has to be associated to the corresponding query table
     * from the list <code>tableList</code>.
     * For fields mapped to multiple columns choose one column to be included,
     * as all mapped columns should have the same value.
     *
     * @param desc Local field descriptor to be included in the constraint text.
     * @param thePlan Query plan corresponding to <code>desc</code>.
     * @param sb String buffer taking the resulting text.
     */
    protected void generateColumnText(LocalFieldDesc desc, QueryPlan thePlan,
                                      StringBuffer sb) {
        QueryTable table = null;
        ColumnElement column = null;
        Iterator iter = desc.getColumnElements();

        while (iter.hasNext() && table == null) {
            column = (ColumnElement) iter.next();

            // For updates, the member variable tableList is complete
            // at this point and includes only the table being updated.
            // For selects, new tables are still added to tableList
            // when join constraints are processed. Take the table list
            // from the query plan to find the table matching the column.
            if (action == QueryPlan.ACT_SELECT) {
                table = thePlan.findQueryTable(column.getDeclaringTable());
            } else {
                table = findQueryTable(column.getDeclaringTable());
            }
        }

        if (table == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.configuration.fieldnotable", // NOI18N
                    desc.getName()));
        }

        // Select statements might include columns from several tables.
        // Qualify the column with the table index.
        if (action == QueryPlan.ACT_SELECT) {
            sb.append("t").append(table.getTableIndex()).append("."); // NOI18N
        }

        appendQuotedText(sb, column.getName().getName());
    }

    /**
     * Matches the table element <code>tableElement</code> to the
     * corresponding query table from the list <code>tableList</code>.
     *
     * @param tableElement Table element to be found.
     * @return Query table object corresponding to table element.
     * @see QueryPlan#findQueryTable(TableElement)
     */
    protected QueryTable findQueryTable(TableElement tableElement) {
        QueryTable table = null;

        for (Iterator iter = tableList.iterator(); iter.hasNext() && table == null; ) {
            QueryTable t = (QueryTable) iter.next();
            if (t.getTableDesc().getTableElement() == tableElement) {
            // if (t.getTableDesc().getTableElement().equals(tableElement)) {
                table = t;
            }
        }

        return table;
    }

    private void processConstraintOperation(ConstraintOperation opNode,
                                            List stack,
                                            StringBuffer result) {
        int opCode = opNode.operation;
        int format = operationFormat(opCode);

        if ((format & OP_IRREGULAR_MASK) == 0) {
            processFunctionOrBinaryOperation(format, opCode, stack, result);
        } else {
            processIrregularOperation(opNode, opCode, stack, result);
        }
    }

    private void processFunctionOrBinaryOperation(int format,
                                                  int opCode,
                                                  List stack,
                                                  StringBuffer result) {

        if ((format & OP_PREFIX_MASK) > 0) {
            result.append(prefixOperator(opCode));
        }

        if ((format & OP_PCOUNT_MASK) > 0) {
            if ((format & OP_PAREN_MASK) > 0) {
                result.append("("); // NOI18N
            }

            result.append(getWhereText(stack));

            for (int i = 0; i < ((format & OP_PCOUNT_MASK) / OP_PARAM_MASK) - 1; i++) {
                if ((format & OP_INFIX_MASK) > 0) {
                    // opCode for which OP_BINOP_MASK is set
                    result.append(infixOperator(opCode, i - 1));
                } else {
                    // opCode for which OP_FUNC_MASK is set
                    result.append(", "); // NOI18N
                }

                result.append(getWhereText(stack));
            }

            if ((format & OP_PAREN_MASK) > 0) {
                result.append(")"); // NOI18N
            }
        }

        if ((format & OP_POSTFIX_MASK) > 0) {
            result.append(postfixOperator(opCode));
        }
    }

    protected void processIrregularOperation(ConstraintOperation opNode,
                                             int opCode,
                                             List stack,
                                             StringBuffer result) {
        switch (opCode) {
            case ActionDesc.OP_NULL:
            case ActionDesc.OP_NOTNULL:
                processNullOperation(opCode, stack, result);
                break;
           case ActionDesc.OP_BETWEEN:
                if (stack.size() < 3) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.constraint.stackempty")); // NOI18N
                }

                if (!(stack.get(stack.size() - 1) instanceof ConstraintField)) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.constraint.needfieldnode")); // NOI18N
                } else {
                    result.append(getWhereText(stack));
                    result.append(" between "); // NOI18N
                }
                result.append(getWhereText(stack));
                result.append(" and ");
                result.append(getWhereText(stack));
                break;
            case ActionDesc.OP_IN:
            case ActionDesc.OP_NOTIN:
                processInOperation(opCode, stack, result);
                break;
            case ActionDesc.OP_NOTEXISTS:
            case ActionDesc.OP_EXISTS:
                if (!(opNode instanceof ConstraintSubquery)) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.generic.notinstanceof", // NOI18N
                            opNode.getClass().getName(), "ConstraintSubquery")); // NOI18N
                }

                ConstraintSubquery sqNode = (ConstraintSubquery) opNode;

                result.append(prefixOperator(opCode));
                result.append("("); // NOI18N

                Statement sqstmt = (Statement) sqNode.plan.statements.get(0);

                result.append(sqstmt.getText());

                result.append(")"); // NOI18N
                break;
            case ActionDesc.OP_LIKE_ESCAPE:
                if (stack.size() < 3) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.constraint.stackempty")); // NOI18N
                }

                if (vendorType.supportsLikeEscape()) {
                    if (!(stack.get(stack.size() - 1) instanceof ConstraintField)) {
                        throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                                "core.constraint.needfieldnode")); // NOI18N
                    } else {
                        result.append(getWhereText(stack));
                        result.append(" LIKE "); // NOI18N
                    }
                    result.append(getWhereText(stack));
                    result.append(vendorType.getLeftLikeEscape());
                    result.append(" ESCAPE "); // NOI18N
                    result.append(getWhereText(stack));
                    result.append(vendorType.getRightLikeEscape());
                } else {
                    throw new JDOFatalInternalException(
                            I18NHelper.getMessage(messages,
                                    "sqlstore.sql.generator.statement.likeescapenotsupported")); //NOI18N
                }

                break;
            case ActionDesc.OP_SUBSTRING:
                if (stack.size() < 3) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.constraint.stackempty")); // NOI18N
                }

                result.append(vendorType.getSubstring());
                result.append("("); // NOI18N
                result.append(getWhereText(stack));
                result.append(vendorType.getSubstringFrom());
                result.append(getWhereText(stack));
                result.append(vendorType.getSubstringFor());
                result.append(getWhereText(stack));
                result.append(")"); // NOI18N
                break;
            case ActionDesc.OP_POSITION:
                if (stack.size() < 2) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.constraint.stackempty")); // NOI18N
                }

                result.append(vendorType.getPosition());
                result.append("("); // NOI18N
                boolean swap = vendorType.isPositionSearchSource();
                if (swap) {
                    ConstraintNode expr =
                            (ConstraintNode)stack.remove(stack.size() - 1);
                    ConstraintNode pattern =
                            (ConstraintNode)stack.remove(stack.size() - 1);
                    stack.add(expr);
                    stack.add(pattern);
                }

                result.append(getWhereText(stack));
                result.append(vendorType.getPositionSep());
                result.append(" ").append(getWhereText(stack)); // NOI18N
                result.append(")"); // NOI18N
                break;
            case ActionDesc.OP_POSITION_START:
                if (stack.size() < 3) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.constraint.stackempty")); // NOI18N
                }

                boolean swapArgs = vendorType.isPositionSearchSource();
                boolean threeArgs = vendorType.isPositionThreeArgs();

                if (threeArgs) {
                    if (swapArgs) {
                        ConstraintNode expr =
                                (ConstraintNode)stack.remove(stack.size() - 1);
                        ConstraintNode pattern =
                                (ConstraintNode)stack.remove(stack.size() - 1);
                        stack.add(expr);
                        stack.add(pattern);
                    }
                    result.append(vendorType.getPosition());
                    result.append("("); // NOI18N
                    result.append(getWhereText(stack));
                    result.append(vendorType.getPositionSep());
                    result.append(" ").append(getWhereText(stack)); // NOI18N
                    result.append(vendorType.getPositionSep());
                    result.append(" ").append(getWhereText(stack)); // NOI18N
                    result.append(")"); // NOI18N
                } else { //twoArgs
                    ConstraintValue valueNode =
                            (ConstraintValue)stack.remove(stack.size() - 3);
                    if (valueNode != null && ONE.equals(valueNode.getValue())) {
                        stack.add(new ConstraintOperation(ActionDesc.OP_POSITION));
                        result.append(getWhereText(stack));
                    } else {
                        throw new JDOFatalInternalException(
                                I18NHelper.getMessage(messages,
                                        "sqlstore.sql.generator.statement.positionthreeargsnotsupported")); // NOI18N
                    }
                }
                break;
            case ActionDesc.OP_MAYBE_NULL:
                processMaybeNullOperation(stack, result);
                break;
            case ActionDesc.OP_MOD:
                if (stack.size() < 2) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.constraint.stackempty")); // NOI18N
                }
                result.append(prefixOperator(opCode));
                result.append("("); // NOI18N
                result.append(getWhereText(stack));
                result.append(", "); // NOI18N
                result.append(getWhereText(stack));
                result.append(")"); // NOI18N
                break;
            case ActionDesc.OP_CONCAT:
                processConcatOperation(opCode, stack, result);
                break;

            default:
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.illegalop", // NOI18N
                        "" + opCode)); // NOI18N
        }
    }

    private void processConcatOperation(int opCode, List stack,
            StringBuffer result) {

        if (stack.size() < 2) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.stackempty")); // NOI18N
        }
        String concatCast = vendorType.getConcatCast();
        if (concatCast.length() != 0) {
            result.append(concatCast);
            //Opening brace for concat cast
            result.append("( "); // NOI18N
        }
        // Concat is a binary infix operator. Process it manually here.
        // Resolve: Why should CONCAT operation be inside braces.
        // Opening brace around CONCAT operation
        result.append("( "); // NOI18N
        result.append(getWhereText(stack));
        result.append(infixOperator(opCode, 0));
        result.append(getWhereText(stack));
        // Closing brace around CONCAT operation
        result.append(" ) "); // NOI18N

        if (concatCast.length() != 0) {
            // Closing brace for concat cast
            result.append(" ) "); // NOI18N
        }
    }

    private void processMaybeNullOperation(List stack, StringBuffer result) {
        ConstraintValue valueNode = null;
        ConstraintField fieldNode = null;

        if (stack.size() < 2) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.stackempty")); // NOI18N
        }

        if (!(stack.get(stack.size() - 1) instanceof ConstraintField)) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.needfieldnode")); // NOI18N
        } else {
            fieldNode = (ConstraintField) stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
        }

        if (!(stack.get(stack.size() - 1) instanceof ConstraintValue)) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.needvalnode")); // NOI18N
        } else {
            valueNode = (ConstraintValue) stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
        }

        Object value = valueNode.getValue();
        if (value instanceof String) {
            String v = (String) value;

            if (v.length() == 0) {
                stack.add(fieldNode);
                stack.add(new ConstraintOperation(ActionDesc.OP_NULL));
            } else {
                stack.add(valueNode);
                stack.add(fieldNode);
                stack.add(new ConstraintOperation(ActionDesc.OP_EQ));
            }
        } else {
            stack.add(valueNode);
            stack.add(fieldNode);
            stack.add(new ConstraintOperation(ActionDesc.OP_EQ));

            //
            // This takes care of the case where a nullable database column
            // is mapped to a primitive field.
            // This check adds a "OR numericColumn IS NULL" to the constraint.
            // See FieldDesc#setValue(StateManager, Object) for the conversions
            // done on binding a primitive field from the store.
            //
            // RESOLVE: Don't we have to do this in each OP_EQ comparison
            // involving primitive Fields?
            //
            boolean maybeNull = false;

            if ((value instanceof Number) &&
                    ((Number) value).doubleValue() == 0) {
                maybeNull = true;
            } else if ((value instanceof Boolean) &&
                    ((Boolean) value).booleanValue() == false) {
                maybeNull = true;
            } else if ((value instanceof Character) &&
                    ((Character) value).charValue() == '\0') {
                maybeNull = true;
            }

            if (maybeNull) {
                stack.add(fieldNode);
                stack.add(new ConstraintOperation(ActionDesc.OP_NULL));
                stack.add(new ConstraintOperation(ActionDesc.OP_OR));
            }
        }

        if (stack.size() > 0) {
            result.append(getWhereText(stack));
        }
    }

    private void processNullOperation(int opCode, List stack, StringBuffer result) {
        String nullComparisionFunctionName =
            vendorType.getNullComparisonFunctionName();
        if( nullComparisionFunctionName.length() != 0) {
            // Null comparision for LOB type fields is
            // through function for this DB
            Object nextNode = stack.get(stack.size() - 1);
            if( nextNode != null) {
                if ( nextNode instanceof ConstraintFieldName) {
                    ConstraintFieldName fieldNode =
                            (ConstraintFieldName) nextNode;
                    QueryPlan originalPlan = getQueryPlan();
                    if (fieldNode.originalPlan != null) {
                        originalPlan = fieldNode.originalPlan;
                    }
                    LocalFieldDesc desc = (LocalFieldDesc) originalPlan.config.getField(fieldNode.name);
                    if ( desc.isMappedToLob() ) {
                        // Add a dummy ConstraintOperation Node corresponding
                        // to null comparision func.
                        stack.add (new ConstraintOperation(
                                ActionDesc.OP_NULL_COMPARISION_FUNCTION) );
                    }
                }
            } else {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.stackempty")); // NOI18N
           }
        }
        result.append (getWhereText(stack));
        String str = (opCode == ActionDesc.OP_NULL) ? vendorType.getIsNull() : vendorType.getIsNotNull();
        result.append(str);
    }

    private void processInOperation(int opCode, List stack, StringBuffer result) {
        //We are trying to construct a where clause like following in the quotes here
        // where "(t0.field1, t0.field2, t0.field3,...) in
        //        ( select t1.fld1, t1.fld2, t2.fld3,...where ....)"

        StringBuffer c = new StringBuffer();

        if (stack.size() < 2) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.stackempty")); // NOI18N
        }

        //Append the first bracket "(" to the result
        result.append("("); // NOI18N

        if (!(stack.get(stack.size() - 1) instanceof ConstraintField)) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.needfieldnode")); // NOI18N
        } else {
            //append "t0.field1" to c
            c.append(getWhereText(stack));
        }

        //Append ", t0.field2, t0fld3,...."
        while (stack.size() > 1 && (stack.get(stack.size() - 1) instanceof ConstraintField)) {
            c.replace(0, 0, ", "); // NOI18N
            c.replace(0, 0, getWhereText(stack));
        }
        result.append(c.toString());
        result.append(") "); // NOI18N
        if (opCode == ActionDesc.OP_NOTIN) {
            result.append("not "); // NOI18N
        }
        result.append("in ("); // NOI18N


        ConstraintNode currentNode = (ConstraintNode)stack.remove(stack.size() - 1);
        if ( ! ( currentNode instanceof ConstraintSubquery)) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.generic.notinstanceof", // NOI18N
                    currentNode.getClass().getName(), "ConstraintSubquery")); // NOI18N
        } else {

            ConstraintSubquery sqnode = (ConstraintSubquery) currentNode;

            Statement sqstmt = (Statement) sqnode.plan.statements.get(0);
            //Append the subquery i.e. "select t1.fld1, t1.fld2, t2.fld3,...where ...."
            result.append(sqstmt.getText());

            //Close the final bracket
            result.append(")"); // NOI18N

            //Append the Input values to the InputDesc of current statement
            inputDesc.values.addAll(sqstmt.inputDesc.values);
        }

        /*
          //This is old code that takes care of case when we just have a list of values
          //as the parameter to the IN clause. We might uncomment and enhance this code
          //when we implement the functionality to have list of values inside IN clause.
        if (!(stack.get(stack.size() - 1) instanceof ConstraintValue)) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.constraint.needvalnode")); // NOI18N
        } else {
            result.append(((ConstraintValue) stack.get(stack.size() - 1)).value.toString());
            stack.remove(stack.size() - 1);
        }

        result.append(")"); // NOI18N
        */
    }

    /**
     * Gets the column information for the field to which <code>node</code>
     * is bound.
     * @param node The input node.
     * @return Returns null if the node isn't bound to a field. Otherwise
     * returns ColumnElement for the primary column of the field to which the
     * node is bound.
     */
    private static ColumnElement getColumnElementForValueNode(ConstraintValue node) {
        ColumnElement columnElement = null;
        LocalFieldDesc field = node.getLocalField();
        if(field != null) {
            //For fields mapped to multiple columns, we assume
            //that all the columns have the same value in the database.
            //Hence we only use the primary column in where clause.
            columnElement = field.getPrimaryColumn();
        }
        return columnElement;
    }

    /**
     * Generates InputValue for <code>node</code>.
     * @param node The input node.
     */
    protected void generateInputValueForConstraintValueNode(ConstraintValue node) {
        inputDesc.values.add(new InputValue(node.getValue(), getColumnElementForValueNode(node) ));
    }

    //only operations end with ")" or nothing can be here, cf. getWhereText
    protected String infixOperator(int operation, int position) {
        //
        //	InfixOperator
        //		The InfixOperator method returns the SQL text for the operator specified
        //		by the operation parameter.
        //
        //	operation
        //		The operation parameter specifies which operation for which to return
        //		SQL text.  Legal values are defined by the ConstraintOperation class.
        //
        StringBuffer result = new StringBuffer();

        switch (operation) {
            case ActionDesc.OP_ADD:
                result.append(" + "); // NOI18N
                break;
            case ActionDesc.OP_AND:
                result.append(" and ");
                break;
            case ActionDesc.OP_DIV:
                result.append(" / "); // NOI18N
                break;
            case ActionDesc.OP_EQ:
                result.append(" = "); // NOI18N
                break;
            case ActionDesc.OP_GE:
                result.append(" >= "); // NOI18N
                break;
            case ActionDesc.OP_GT:
                result.append(" > "); // NOI18N
                break;
            case ActionDesc.OP_LE:
                result.append(" <= "); // NOI18N
                break;
            case ActionDesc.OP_LT:
                result.append(" < "); // NOI18N
                break;
            case ActionDesc.OP_NE:
                result.append(" ");
                result.append(vendorType.getNotEqual());
                result.append(" ");
                break;
            case ActionDesc.OP_OR:
                result.append(" or "); // NOI18N
                break;
            case ActionDesc.OP_LIKE:
                result.append(" like "); // NOI18N
                break;
            case ActionDesc.OP_MUL:
                result.append(" * "); // NOI18N
                break;
            case ActionDesc.OP_SUB:
                result.append(" - "); // NOI1N8
                break;
            case ActionDesc.OP_MOD:
                result.append(" % "); // NOI1N8
                break;
            case ActionDesc.OP_BETWEEN:
                if (position == 1) {
                    result.append(" between "); // NOI18N
                } else {
                    result.append(" and ");
                }
                break;
            case ActionDesc.OP_CONCAT:
                result.append(vendorType.getStringConcat());
                break;
            default:
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.illegalop", // NOI18N
                        "" + operation)); // NOI18N
        }

        return result.toString();
    }


    protected int operationFormat(int operation) {
        int format = 0;
        switch (operation) {
            // Binary operators
            case ActionDesc.OP_EQ:
            case ActionDesc.OP_NE:
            case ActionDesc.OP_GT:
            case ActionDesc.OP_GE:
            case ActionDesc.OP_LT:
            case ActionDesc.OP_LE:
            case ActionDesc.OP_AND:
            case ActionDesc.OP_MUL:
            case ActionDesc.OP_LIKE:
                format = OP_BINOP_MASK;
                break;

            // Binary operators that require Parenthesis
            case ActionDesc.OP_ADD:
            case ActionDesc.OP_SUB:
            case ActionDesc.OP_DIV:
            case ActionDesc.OP_OR:
                format = OP_BINOP_MASK | OP_PAREN_MASK;
                break;

            // Functions
            case ActionDesc.OP_ABS:
            case ActionDesc.OP_SQRT:
            case ActionDesc.OP_LENGTH:
            case ActionDesc.OP_LTRIM:
            case ActionDesc.OP_NOT:
            case ActionDesc.OP_NULL_COMPARISION_FUNCTION:
                format = OP_FUNC_MASK;
                break;

            // Irregular operators
            case ActionDesc.OP_BETWEEN:
            case ActionDesc.OP_LIKE_ESCAPE:
            case ActionDesc.OP_IN:
            case ActionDesc.OP_NOTIN:
            case ActionDesc.OP_NULL:
            case ActionDesc.OP_NOTNULL:
            case ActionDesc.OP_MAYBE_NULL:
            case ActionDesc.OP_CONCAT:
            case ActionDesc.OP_EQUIJOIN:
                format = OP_IRREGULAR_MASK | OP_WHERE_MASK;
                break;

            // Irregular operators that can never be at the root of a where clause.
            // Hence they do not have OP_WHERE_MASK set.
            case ActionDesc.OP_SUBSTRING:
            case ActionDesc.OP_POSITION:
            case ActionDesc.OP_POSITION_START:
                format = OP_IRREGULAR_MASK;
                break;

            case ActionDesc.OP_NOTEXISTS:
            case ActionDesc.OP_EXISTS:
                format = OP_IRREGULAR_MASK | OP_WHERE_MASK | OP_PREFIX_MASK;
                break;

            case ActionDesc.OP_MOD:
                format = vendorType.isModOperationUsingFunction() ?
                    OP_IRREGULAR_MASK : OP_BINOP_MASK | OP_PAREN_MASK;
                break;

            case ActionDesc.OP_DISTINCT:
                format = OP_OTHER_MASK;
                break;

            case ActionDesc.OP_ORDERBY:
            case ActionDesc.OP_ORDERBY_DESC:
                format = OP_ORDERBY_MASK;
                break;

            case ActionDesc.OP_RTRIM:
            case ActionDesc.OP_RTRIMFIXED:
                if (vendorType.isAnsiTrim()) {
                    format = OP_WHERE_MASK | OP_PREFIX_MASK | OP_POSTFIX_MASK | OP_PARAM_MASK;
                } else {
                    format = OP_FUNC_MASK;
                }
                break;

            //TODO: Check how can this masks be optimized
            case ActionDesc.OP_LEFTJOIN:
                format = OP_IRREGULAR_MASK | OP_WHERE_MASK | OP_INFIX_MASK;
                format = format | OP_POSTFIX_MASK;
                break;
            case ActionDesc.OP_RIGHTJOIN:
                format = OP_IRREGULAR_MASK | OP_WHERE_MASK | OP_INFIX_MASK;
                format = format | OP_PREFIX_MASK;
                break;

            default:
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.illegalop", // NOI18N
                        "" + operation)); // NOI18N
        }

        return format;
    }

    protected String postfixOperator(int operation) {
        //
        //	PostfixOperator
        //		The PostfixOperator method returns the SQL text for the operator specified
        //		by the operation parameter.
        //
        //	operation
        //		The operation parameter specifies which operation for which to return
        //		SQL text.  Legal values are defined by the ConstraintOperation class.
        //
        StringBuffer result = new StringBuffer();

        switch (operation) {
            case ActionDesc.OP_RTRIM:
                result.append(vendorType.getRtrimPost());
                break;
            case ActionDesc.OP_RTRIMFIXED:
                result.append(postfixOperator(ActionDesc.OP_RTRIM));
                break;
            case ActionDesc.OP_LEFTJOIN:
                result.append(vendorType.getLeftJoinPost());
                break;
            default:
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.illegalop", // NOI18N
                        "" + operation)); // NOI18N
        }

        return result.toString();
    }

    protected String prefixOperator(int operation) {
        //
        //  PrefixOperator
        //      The PrefixOperator method returns the SQL text for the operator specified
        //      by the operation parameter.
        //
        //  operation
        //      The operation parameter specifies which operation for which to return
        //      SQL text.  Legal values are defined by the ConstraintOperation class.
        //
        StringBuffer result = new StringBuffer();

        switch (operation) {
            case ActionDesc.OP_ABS:
                result.append(vendorType.getAbs()); // NOI18N
                break;
            case ActionDesc.OP_LENGTH:
                result.append(vendorType.getCharLength()); // NOI18N
                break;
            case ActionDesc.OP_RTRIM:
                result.append(vendorType.getRtrim());
                break;
            case ActionDesc.OP_RTRIMFIXED:
                result.append(prefixOperator(ActionDesc.OP_RTRIM));
                break;
            case ActionDesc.OP_NOT:
                result.append("not "); // NOI18N
                break;
            case ActionDesc.OP_SQRT:
                result.append(vendorType.getSqrt());
                break;
            case ActionDesc.OP_NOTEXISTS:
                result.append("not exists "); // NOI18N
                break;
            case ActionDesc.OP_EXISTS:
                result.append("exists "); // NOI18N
                break;
            case ActionDesc.OP_NULL_COMPARISION_FUNCTION:
                result.append(vendorType.getNullComparisonFunctionName());
                break;
            case ActionDesc.OP_MOD:
                result.append(vendorType.getModFunctionName()); // NOI18N
                break;

            default:
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.constraint.illegalop", "" + operation)); // NOI18N
        }

        return result.toString();
    }

    public void addSecondaryTableStatement(Statement s) {
        if (s == null) return;

        if (secondaryTableStatements == null)
            secondaryTableStatements = new ArrayList();

        secondaryTableStatements.add(s);
    }

    public ArrayList getSecondaryTableStatements() {
        return secondaryTableStatements;
    }

    public ArrayList getQueryTables() {
        return tableList;
    }

    public ArrayList getColumnRefs() {
        return columns;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getAction() {
        return action;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            //
            // shouldn't happen.
            //
            return null;
        }
    }

    /**
     * Binds input valus corrsponding to this <code>Statement</code> object to
     * database statement s.
     * @param s The database statement
     * @throws SQLException
     */
    public void bindInputValues(DBStatement s) throws SQLException {
        for (int i = 0, size = inputDesc.values.size(); i < size; i++) {
            InputValue inputVal = (InputValue) inputDesc.values.get(i);
            s.bindInputColumn(i + 1, inputVal.getValue(),
                    inputVal.getColumnElement(), vendorType);
        }
    }

    /**
     * Gets input values corrsponding to this <code>Statement</code>.
     * @return An Object array containing input values.
     */
    private Object[] getInputValues() {
        final int size = inputDesc.values.size();
        Object[] inputValues = new Object[size];
        for (int i = 0; i < size; i++) {
            InputValue inputValue = (InputValue) inputDesc.values.get(i);
            inputValues[i] = inputValue.getValue();
        }
        return inputValues;
    }

    /**
     * Gets formatted sql text corrsponding to this statement object. The text
     * also contains values for input to the statement.
     * @return formatted sql text corrsponding to this statement object.
     */
    public String getFormattedSQLText() {
        return formatSqlText(getText(), getInputValues());
    }

    /**
     *  The formatSqlText method returns a string containing the text of
     *  the SQL statement about to be executed and the input values for the
     *  placeholders.
     *  @param sqlText Specifies the text of the SQL statement to be executed.
     *  @param input Holds the input values used for the SQL statement.
     *  @return The SQL text and the input values formatted into a printable
     * string.
     */
    static protected String formatSqlText(String sqlText, Object[] input) {
        StringBuffer str = new StringBuffer();

        str.append(I18NHelper.getMessage(messages,
                "sqlstore.sql.generator.statement.sqlStatement") );   //NOI18N
        str.append("<").append(sqlText).append("> "); // NOI18N

        if (input != null && input.length > 0) {
            str.append(I18NHelper.getMessage(messages,
            "sqlstore.sql.generator.statement.withinputvalues")); // NOI18N
            for (int i = 0; i < input.length; i++) {
                if (i > 0) {
                    str.append(", "); // NOI18N
                }
                Object inputValue = input[i];
                if (inputValue == null) {
                    str.append("<null>"); // NOI18N
                } else {
                    str.append(inputValue.getClass().getName());
                    str.append(":"); // NOI18N
                    str.append(inputValue.toString());
                }
            }
        } else {
            str.append(I18NHelper.getMessage(messages,
            "sqlstore.sql.generator.statement.withnoinputvalues")); // NOI18N
       }

        return str.toString();
    }

}
