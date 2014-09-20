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
 * Constraint.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint;

import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.QueryPlan;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class Constraint extends Object {
    /**
     * The stack that contains all constraints beside join constraints.
     * Join constraints are handled by a separate stack.
     */
    public List stack;

    /**
     * The stack that contains outer join constraints. All the elements of this stack
     * are instance of {@link ConstraintJoin}. Outer join constraints can be appended
     * to the query w/o changing the semantic.
     */
    private List outerJoinStack;

    /**
     * The stack that contains order by constraints. We would like to
     * separate Order By constraints from the main stack, but they must
     * be merged with all other constraints to preserve the order of
     * constraints from different stacks. E.g.
     * "order by firstname ascending, department.name ascending"
     *
     * @see #isEmptyOrOrderBy
     */
//    private List orderByStack;

    /**
     * Adds a field to the constraint stack.
     *
     * AddField creates a ConstraintField node for the indicated
     * named field, optionally including an operation descriptor
     * and adds it to the constraint stack.
     *
     * @param name
     * 	The name parameter specifies the name of the field to be
     * 	added to the constrant stack.
     *
     * @param desc
     * 	The desc parameter specifies an operation descriptor describing
     * 	what is to be done with the field named by the name parameter.
     */
    public void addField(String name, ActionDesc desc) {
        stack.add(new ConstraintFieldName(name, desc));
    }

    /**
     * Adds a field to the constraint stack.
     *
     * AddField creates a ConstraintFieldDesc node for the indicated
     * field descriptor and adds it to the constraint stack.
     *
     * @param desc
     * 	The Desc parameter is the field descriptor to be
     * 	added to the constrant stack.
     */
    public void addField(LocalFieldDesc desc) {
        stack.add(new ConstraintFieldDesc(desc));
    }

    /**
     * Adds a field to the constraint stack.
     *
     * AddField creates a ConstraintFieldDesc node for the indicated
     * field descriptor and adds it to the constraint stack.
     *
     * @param desc
     *     The Desc parameter is the field descriptor to be
     *     added to the constrant stack.
     *
     * @param plan
     *     The query plan to which this desc belongs
     */
    public void addField(LocalFieldDesc desc, QueryPlan plan) {
        stack.add(new ConstraintFieldDesc(desc, plan));
    }

    public void addField(ConstraintFieldDesc constraintDesc) {
        stack.add(constraintDesc);
    }

    public void addForeignField(String name, ActionDesc desc) {
        stack.add(new ConstraintForeignFieldName(name, desc));
    }

    /**
     * Adds an operation to the constraint stack.
     *
     * AddOperation creates a ConstraintOperation node whose operation is
     * operation and adds it to the constraint stack.
     *
     * @param operation
     * 	The operation parameter specifies the operation to be added to
     * 	the constrant stack.
     */
    public void addOperation(int operation) {
        stack.add(new ConstraintOperation(operation));
    }

    /**
     * Adds a data value to the constraint stack. Creates a ConstraintValue
     * node whose value is value and adds it to the constraint stack.
     * @param value The value to be added to the constrant stack.
     * @param localField The localField to which this value is bound.
     * Please note that localField can be null for values involved in
     * complex expressions in a query.
     */
    public void addValue(Object value, LocalFieldDesc localField) {
        stack.add(new ConstraintValue(value, localField));
    }

    /**
     * Adds a subquery constraint on the stack.
     * @param field The field on which subquery constraint is added.
     * @param rd    Retrieve descriptor corresponding to the subquery.
     */
    public void addConstraintFieldSubQuery(String field, ActionDesc rd)  {
        stack.add(new ConstraintFieldNameSubQuery(field, rd));
    }

    /**
     * Adds the index of a parameter to the stack.
     * @param index the parameter index.
     * @param enumType the type for this parameter.
     * @param localField the localField to which this parameter is bound.
     */
    public void addParamIndex(int index, int enumType,
                                    LocalFieldDesc localField) {
        stack.add(new ConstraintParamIndex(index,enumType, localField));
    }

    /**
     * Adds specified join constraint. Equi joins are added to the
     * constraint stack, outer joins are added to a separate stack, the
     * <code>outerJoinStack</code>.
     *
     * @param join The join constraint to be added.
     */
    public void addJoinConstraint(ConstraintJoin join) {
        if (join.operation == ActionDesc.OP_EQUIJOIN) {
            stack.add(join);
        } else {
            // The current logic is written with the assumption that a join
            // that is not an equi join is always a left join
            assert join.operation == ActionDesc.OP_LEFTJOIN;
            outerJoinStack.add(join);
        }
    }

    /**
     * Merges the stack with the specified foreign constraint stack.
     * @param foreignConstraint The constraint to be merged.
     * @param joinOp Join operation as defined in {@link ActionDesc}.
     * @return True, if we need to add an additional "AND" constraint.
     */
    public boolean mergeConstraint(Constraint foreignConstraint, int joinOp) {

        stack.addAll(foreignConstraint.stack);
        outerJoinStack.addAll(foreignConstraint.outerJoinStack);

        return addAnd(foreignConstraint, joinOp);
    }

    /**
     * Decides, if we need to add an additional "AND" constraint to
     * the stack <em>after</em> merging the foreign stack.
     *
     * @param foreignConstraint Constraint to be joined.
     * @param joinOp Join operator.
     * @return True, if we need to add an additional "AND" constraint.
     */
    private boolean addAnd(Constraint foreignConstraint, int joinOp) {
        // Add "AND" constraints for equi-joins only.
        // * Don't add an "AND" constraint for outer joins. Outer joins
        // don't contribute to the "where"-clause in ANSI-case. In the
        // non-ANSI case, outer joins can be appended to the query w/o
        // changing the semantic.
        // * Don't add an "AND" constaint for non-relationship joins,
        // as no additional join constraint is added in this case.
        return (joinOp == ActionDesc.OP_EQUIJOIN)

        // Never add an "AND" constraint, if the foreign stack is
        // empty or contains "ORDER_BY" constraints only, as "ORDER_BY"
        // constraints don't contribute to the "where"-clause.
        && !foreignConstraint.isEmptyOrOrderBy();
    }

    /**
     * Checks, if the constraint stack is empty or contains "ORDER_BY"
     * constraints only. "ORDER_BY" constraints are recognized as an
     * ORDER_BY operation followed by a field name constraint, and
     * an optional Value constraint giving the position of the Order
     * By constraint.
     *  <em>NOTE:</em> The value constraints giving the position for
     * the order by constraints are currently not generated by the
     * query compiler. Order by constraints stay in the correct order
     * because of two reasons
     * <ul>
     * <li>the way, constraints are processed by the query compiler</li>
     * <li>the way constraint stacks are joined in
     * {@link com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.SelectQueryPlan#processForeignConstraints}.
     * </li>
     * </ul>
     */
    private boolean isEmptyOrOrderBy() {
        boolean rc = true;
        // Abort the loop at first possible opportunity.
        for (int i = stack.size() - 1; i >= 0 && rc; ) {
            ConstraintNode node = (ConstraintNode) stack.get(i);

            if ((node instanceof ConstraintOperation)
                    && ((((ConstraintOperation) node).operation == ActionDesc.OP_ORDERBY) ||
                    (((ConstraintOperation) node).operation == ActionDesc.OP_ORDERBY_DESC))) {
                if ((i > 0) && (
                        stack.get(i - 1) instanceof ConstraintFieldName ||
                        stack.get(i - 1) instanceof ConstraintFieldDesc)) {
                    // Order By constraint.
                    i--;
                    if ((i > 0) && (stack.get(i - 1) instanceof ConstraintValue)) {
                        // Optional Value constraint.
                        i--;
                    }
                } else {
                    rc = false;
                }
            } else {
                rc = false;
            }

            // Check the next constraint if any.
            i--;
        }
        return rc;
    }

    /**
      * Gets the where clause constraints for this Constraint
      * @return The where clause constraints for this Constraint
      */
     public List getConstraints() {
         return stack;
     }

    /**
      * Gets the outer join constraints for this Constraint
      * @return The outer join constraints for this Constraint
      */
     public List getOuterJoinConstraints() {
         return outerJoinStack;
     }

    public Constraint() {
        stack = new ArrayList();
        outerJoinStack = new ArrayList();
    }

}
