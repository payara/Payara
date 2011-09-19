package org.glassfish.elasticity.expression;

/**
 * Created by IntelliJ IDEA.
 * User: mk

 * Date: 9/18/11
 * Time: 6:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionEvaluationException
    extends  RuntimeException {


    public ExpressionEvaluationException() {
    }

    public ExpressionEvaluationException(String s) {
        super(s);
    }

    public ExpressionEvaluationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ExpressionEvaluationException(Throwable throwable) {
        super(throwable);
    }
}
