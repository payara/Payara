package fish.payara.microprofile.openapi.spec;

import java.util.EnumSet;
import java.util.Iterator;

public final class Field implements Iterable<NodeType> {

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

    public boolean isAnyType() {
        return oneOfTypes.isEmpty();
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