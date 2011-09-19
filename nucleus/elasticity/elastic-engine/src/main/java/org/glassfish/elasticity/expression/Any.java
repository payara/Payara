package org.glassfish.elasticity.expression;

import org.glassfish.elasticity.api.MetricFunction;
import org.glassfish.hk2.scopes.PerLookup;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;

import java.util.Collection;

@Service(name="any")
@Scoped(PerLookup.class)
public class Any
    implements MetricFunction<Number, Boolean>, BooleanConditionSupport<Number> {

    private Collection<Number> collection;

    private BooleanCondition<Number> condition;

    private boolean result;

    public void accept(Collection<Number> collection) {
        this.collection = collection;
    }

    public void applyCondition(BooleanCondition<Number> c) {
        this.condition = c;
        for (Number value : collection) {
            if (condition.doesSatisfy(value)) {
                result = true;
            }
        }
    }

    @Override
    public Boolean value() {
        return result;
    }
}
