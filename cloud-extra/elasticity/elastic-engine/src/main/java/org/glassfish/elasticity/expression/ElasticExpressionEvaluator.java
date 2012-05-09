/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.expression;

import org.glassfish.elasticity.api.MetricFunction;
import org.glassfish.elasticity.engine.container.AlertContextImpl;
import org.glassfish.elasticity.engine.container.ElasticServiceContainer;
import org.glassfish.elasticity.engine.message.ElasticMessage;
import org.glassfish.elasticity.engine.message.MessageProcessor;
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.metric.TabularMetricAttribute;
import org.glassfish.elasticity.metric.TabularMetricEntry;
import org.glassfish.elasticity.util.NotEnoughMetricDataException;
import org.jvnet.hk2.component.Habitat;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public class ElasticExpressionEvaluator {

    private Habitat services;

    private AlertContextImpl ctx;

    private ElasticServiceContainer container;

    private boolean executeRemote;

    public ElasticExpressionEvaluator(Habitat services, AlertContextImpl ctx) {
        this.services = services;
        this.ctx = ctx;
        this.container = ctx.getElasticServiceContainer();
        this.executeRemote = true;
    }

    public ElasticExpressionEvaluator(Habitat services, ElasticServiceContainer container) {
        this.services = services;
        this.container = container;
        this.executeRemote = false;
    }

    public Object evaluate(ExpressionNode node) {
        switch (node.getToken().getTokenType()) {
            case GT:
            case LT:
            case GTE:
            case LTE:
                Object left = evaluate(node.getLeft());
                Object right = evaluate(node.getRight());

                boolean isLeftOperandBooleanConditionSupport
                        = BooleanConditionSupport.class.isAssignableFrom(left.getClass());
                boolean isRightOperandBooleanConditionSupport
                        = BooleanConditionSupport.class.isAssignableFrom(right.getClass());
                if (isLeftOperandBooleanConditionSupport || isRightOperandBooleanConditionSupport) {
                    if (isLeftOperandBooleanConditionSupport) {
                        NumberComparator numberComparator = new NumberComparator(node.getToken().getTokenType(),
                            (Number) right, false /*Not a left operand*/);
                        ((BooleanConditionSupport) left).applyCondition(numberComparator);
                        node.setEvaluatedResult(((MetricFunction) left).value());
                    } else {
                        NumberComparator numberComparator = new NumberComparator(node.getToken().getTokenType(),
                            (Number) left, true /*a left operand*/);
                        ((BooleanConditionSupport) right).applyCondition(numberComparator);
                        node.setEvaluatedResult(((MetricFunction) right).value());
                    }
                } else {
                    NumberComparator numberComparator = new NumberComparator(node.getToken().getTokenType(),
                            (Number) left, true);
                    node.setEvaluatedResult(numberComparator.doesSatisfy((Number) right));
                }
                node.setEvaulatedType(Boolean.class);
                break;
            case DOUBLE:
                node.setEvaluatedResult(Double.valueOf(node.getToken().value()));
                break;
            case INTEGER:
                node.setEvaluatedResult(Integer.valueOf(node.getToken().value()));
                break;
            case FUNCTION_CALL:
                ExpressionParser.FunctionCall functionCallNode = (ExpressionParser.FunctionCall) node;
                MetricFunction function = services.forContract(MetricFunction.class)
                .named(functionCallNode.getFunctionNameToken().value()).get();

                //Evaluate the function param
                Object parameterValue = null;
                if (functionCallNode.isRemote()) {
                    if (!executeRemote) {
                        throw new IllegalStateException("Remote call within a Remote Call..." + functionCallNode);
                    }
                    ElasticMessage em = container.getMessageProcessor().createElasticMessage(null);
                    em.setResponseRequired(true);
                    em.setData(functionCallNode.getParams());
                    MessageProcessor.ExpressionResponse response = container.getMessageProcessor().sendMessage(em);
                    if (response.hasExceptions()) {
                       Throwable ex = null;
                       for (Object obj : response.values()) {
                           if (obj instanceof Exception) {
                               ex = (Exception) obj;
                               if (obj instanceof ExpressionEvaluationException) {
                                   ex = ((ExpressionEvaluationException) obj).getCause();
                                   if (ex instanceof NotEnoughMetricDataException) {
                                       throw ((NotEnoughMetricDataException) ex);
                                   }
                               }
                           }
                       }

                       throw new ExpressionEvaluationException(ex);
                    } else {
                        parameterValue = response.values();
                    }
                } else {
                    parameterValue = evaluate(functionCallNode.getParams().get(0));
                }

                if (Collection.class.isAssignableFrom(parameterValue.getClass())) {
                    function.accept((Collection<Number>) parameterValue);
                } else {
                    List wrappedList = new LinkedList();
                    wrappedList.add(parameterValue);
                    function.accept(wrappedList);
                }

                //fcall.setEvaluatedResult(functionResult);
                if (BooleanConditionSupport.class.isAssignableFrom(function.getClass())) {
                    node.setEvaluatedResult(function);
                    node.setEvaulatedType(function.getClass());
                } else {
                    node.setEvaluatedResult(function.value());
                    node.setEvaulatedType(function.value().getClass());
                }

                break;
            case MULT:
                Number p1 = (Number) evaluate(node.getLeft());
                Number p2 = (Number) evaluate(node.getRight());
                node.setEvaluatedResult(p1.doubleValue() * p2.doubleValue());
                break;
            case DIV:
                Number d1 = (Number) evaluate(node.getLeft());
                Number d2 = (Number) evaluate(node.getRight());
                node.setEvaluatedResult(d1.doubleValue() / d2.doubleValue());
                break;
            case PLUS:
                Number a1 = (Number) evaluate(node.getLeft());
                Number a2 = (Number) evaluate(node.getRight());
                node.setEvaluatedResult(a1.doubleValue() + a2.doubleValue());
                break;
            case ATTR_ACCESS:
                return evaluateAttributeAccess(node);
            default:
                throw new UnsupportedOperationException("Unknown operator: " + node.getToken().getTokenType());
        }

        return node.getEvaluatedResult();
    }

    private Object evaluateAttributeAccess(ExpressionNode node) {
        ExpressionParser.AttributeAccessNode attrNode = (ExpressionParser.AttributeAccessNode) node;

        Object metric = services.forContract(MetricNode.class).named(attrNode.getToken().value()).get();
        attrNode.setEvaulatedType(MetricNode.class);


        List<String> attrNames = (List<String>) attrNode.getData();
        for (int index = 0; index < attrNames.size(); index++) {
            String attrName = attrNames.get(index);
            if (metric instanceof MetricNode) {
                MetricNode parent = (MetricNode) metric;
                for (MetricAttribute attribute : parent.getAttributes()) {
                    if (attribute.getName().equals(attrName)) {
                        metric = attribute;
                        attrNode.setEvaulatedType(metric.getClass());
                    }
                }
            } else if (metric instanceof TabularMetricAttribute) {
                try {
                    TabularMetricAttribute parent = (TabularMetricAttribute) metric;
                    Iterator<TabularMetricEntry> tabIter = parent.iterator(15, TimeUnit.SECONDS, false/*allowPartialView*/);
                    LinkedList result = new LinkedList();
                    while (tabIter.hasNext()) {
                        result.add(tabIter.next().getValue(attrName));
                    }
                    metric = result;
                    attrNode.setEvaulatedType(LinkedList.class);
                } catch (NotEnoughMetricDataException nemdEx) {
                    throw new ExpressionEvaluationException("Not Enough Data", nemdEx);
                }
            } else if (metric instanceof MetricAttribute) {
                metric = ((MetricAttribute) metric).getValue();
                throw new IllegalStateException("Accessing a MetricAttribute from MetricAttribute...?");
            }

            attrNode.setEvaulatedType(metric.getClass());
            attrNode.setEvaluatedResult(metric);
        }

        if (metric instanceof MetricAttribute) {
            metric = ((MetricAttribute) metric).getValue();
        }

        return metric;
    }

    private static class NumberComparator
            implements BooleanCondition<Number> {

        private TokenType operator;

        private Number simpleOperand;

        private boolean isLeftOperand;

        public NumberComparator(TokenType operator, Number simpleNumber, boolean isLeftOperand) {
            this.operator = operator;
            this.simpleOperand = simpleNumber;
            this.isLeftOperand = isLeftOperand;
        }

        public  boolean doesSatisfy(Number number) {
            return isLeftOperand
                    ? doesSatisfy(simpleOperand, operator, number)
                    : doesSatisfy(number, operator, simpleOperand);
        }

        public  boolean doesSatisfy(Number left, TokenType operator, Number right) {
//            System.out.println("NumberComparator " + left.doubleValue() + " " + operator.name() + " " + right.doubleValue());
            switch (operator) {
                case LT:
                    return ((Number) left).doubleValue() < ((Number) right).doubleValue();
                case GT:
                    return ((Number) left).doubleValue() > ((Number) right).doubleValue();
                case LTE:
                    return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
                case GTE:
                    return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
                case EQ:
                    return ((Number) left).doubleValue() == ((Number) right).doubleValue();
                default:
                    throw new IllegalArgumentException("Only > < >= <= = are supported");
            }
        }
    }

}
