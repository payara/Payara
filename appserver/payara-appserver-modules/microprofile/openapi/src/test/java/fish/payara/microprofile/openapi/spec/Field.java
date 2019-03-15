package fish.payara.microprofile.openapi.spec;

import java.util.EnumSet;
import java.util.Iterator;

public final class Field implements Iterable<NodeType> {

    private static final NodeType[] ALL_NODE_TYPES = NodeType.values();

    final String name;
    final EnumSet<NodeType> oneOfTypes = EnumSet.noneOf(NodeType.class);
    boolean isRequired = false;
    boolean isArray = false;
    boolean isMap = false;

    Field(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public boolean isMap() {
        return isMap;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isOneOfManyTypes() {
        return oneOfTypes.size() > 0;
    }

    public boolean isAnyType() {
        return oneOfTypes.isEmpty();
    }

    public NodeType type() {
        return oneOfTypes.iterator().next();
    }

    @Override
    public Iterator<NodeType> iterator() {
        return oneOfTypes.iterator();
    }

    @Override
    public String toString() {
        return name + ": " + oneOfTypes.toString();
    }
}