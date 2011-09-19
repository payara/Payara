package org.glassfish.elasticity.expression;

public interface BooleanCondition<T> {

    public boolean doesSatisfy(T n);

}
