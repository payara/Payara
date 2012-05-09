package org.glassfish.elasticity.expression;

/**
 * Created by IntelliJ IDEA.
 * User: mk
 * Date: 9/17/11
 * Time: 10:19 PM
 * To change this template use File | Settings | File Templates.
 */
public interface BooleanConditionSupport<T> {

    public void applyCondition(BooleanCondition<T> condition);

}
