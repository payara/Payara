package fish.payara.microprofile.openapi.api.visitor;

/**
 * Represents an object that can traverse an API
 * by passing each element to the given {@link ApiVisitor}.
 */
public interface ApiWalker {

    /**
     * Traverse the API, passing each element to the <code>visitor</code>.
     * @param visitor the visitor to pass each element to.
     */
    void accept(ApiVisitor visitor);
}