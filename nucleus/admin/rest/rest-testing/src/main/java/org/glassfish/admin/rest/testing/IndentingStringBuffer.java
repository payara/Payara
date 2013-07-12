package org.glassfish.admin.rest.testing;

public class IndentingStringBuffer {

    private StringBuilder sb = new StringBuilder();
    private Indenter indenter = new Indenter();

    public void println(String val) {
        sb.append(indenter.getIndent())
                .append(val).append("\n");
    }

    public void indent() {
        indenter.indent();
    }

    public void undent() {
        indenter.undent();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
