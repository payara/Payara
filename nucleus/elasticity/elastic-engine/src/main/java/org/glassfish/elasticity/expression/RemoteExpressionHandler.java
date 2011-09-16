package org.glassfish.elasticity.expression;

import org.glassfish.elasticity.api.MetricFunction;
import org.glassfish.elasticity.engine.message.ElasticMessage;
import org.glassfish.elasticity.group.ElasticMessageHandler;
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.metric.TabularMetricAttribute;
import org.glassfish.elasticity.metric.TabularMetricEntry;
import org.jvnet.hk2.component.Habitat;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public class RemoteExpressionHandler {

    private Habitat habitat;

    private String token;

    public RemoteExpressionHandler(Habitat habitat, String token) {
        this.habitat = habitat;
        this.token = token;
    }

    public List<List<Object>>  handleMessage(String senderName, ElasticMessage message) {

        List<List<Object>> resultList = new ArrayList<List<Object>>();
        try {
            List<List<ExpressionNode>> nodes = (List<List<ExpressionNode>>) message.getData();
            int size = nodes.size();

            int index = 0;
            for (List<ExpressionNode> list : nodes) {
                List<Object> result = new ArrayList<Object>();
                for (ExpressionNode node : list) {
                    Object answer = evaluate(node);
                     result.add(answer);
//                    System.out.println("RemoteExpressionHandler evaluated " + node + "; answer = "  + answer);
                }
                resultList.add(result);
                index++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return resultList;
    }

    private Object evaluate(ExpressionNode node) {
        switch (node.getToken().getTokenType()) {
            case DOUBLE:
                node.setEvaluatedResult(Double.valueOf(node.getToken().value()));
                break;
            case INTEGER:
                node.setEvaluatedResult(Integer.valueOf(node.getToken().value()));
                break;
            case FUNCTION_CALL:
                ExpressionParser.FunctionCall fcall = (ExpressionParser.FunctionCall) node;

                MetricFunction function = habitat.getComponent(MetricFunction.class, fcall.getFunctionNameToken().value());
                ExpressionNode param = fcall.getParams().get(0);
                evaluate(param);
                if (param.getToken().getTokenType() == TokenType.ATTR_ACCESS) {
                    ExpressionParser.AttributeAccessNode attrNode = (ExpressionParser.AttributeAccessNode) param;
                    if (attrNode.getEvaulatedType() == MetricAttribute.class) {
                        function.visit(((MetricAttribute) attrNode.getEvaluatedResult()).getValue());
                    } else if (attrNode.getEvaulatedType() == LinkedList.class) {
                       for (Object obj : (LinkedList) attrNode.getEvaluatedResult()) {
                           function.visit(obj);
                       }
                    }
                    node.setEvaluatedResult(function.value());
                    node.setEvaulatedType(function.value().getClass());
                } else {
                    System.out.println("FuncCall with non attr access??");
                }
                break;
            case MULT:
                Number p1 = (Number) evaluate(node.getLeft());
                Number p2 = (Number) evaluate(node.getRight());
                node.setEvaluatedResult(p1.doubleValue() * p2.doubleValue());
                break;
            case PLUS:
                Number a1 = (Number) evaluate(node.getLeft());
                Number a2 = (Number) evaluate(node.getRight());
                node.setEvaluatedResult(a1.doubleValue() + a2.doubleValue());
                break;
            case ATTR_ACCESS:
                return evaluateAttributeAccess(node);
            default:
                System.out.println("** RETURNING UNEVALUATED node: " + node.getToken().getTokenType());
                break;
        }

        return node.getEvaluatedResult();
    }

    private Object evaluateAttributeAccess(ExpressionNode node) {
        ExpressionParser.AttributeAccessNode attrNode = (ExpressionParser.AttributeAccessNode) node;

        Object metric = habitat.getComponent(MetricNode.class, attrNode.getToken().value());
        attrNode.setEvaulatedType(MetricNode.class);

        List<String> attrNames = (List<String>) attrNode.getData();
        for (int index = 0; index < attrNames.size(); index++) {
            String attrName = attrNames.get(index);
            if (metric instanceof MetricNode) {
                MetricNode parent = (MetricNode) metric;
                for (MetricAttribute attribute : parent.getAttributes()) {
                    if (attribute.getName().equals(attrName)) {
                        metric = attribute;
                    }
                }
            } else if (metric instanceof TabularMetricAttribute) {
                TabularMetricAttribute parent = (TabularMetricAttribute) metric;
                Iterator<TabularMetricEntry> tabIter = parent.iterator(15 * 60, TimeUnit.SECONDS);
                LinkedList result = new LinkedList();
                while (tabIter.hasNext()) {
                    result.add(tabIter.next().getValue(attrName));
                }
                metric = result;
            } else if (metric instanceof MetricAttribute) {
                //TODO: Semantic Error??
                metric = null;
            }

            attrNode.setEvaulatedType(metric.getClass());
            node.setEvaluatedResult(metric);
        }

        return metric;
    }

}
