package org.glassfish.admin.rest.testing;

class IndentingStringBuffer {

    private StringBuilder sb = new StringBuilder();
    private Indenter indenter = new Indenter();

    void println(String val) {
        sb.append(indenter.getIndent())
                .append(val).append("\n");
    }

    void indent() {
        indenter.indent();
    }

    void undent() {
        indenter.undent();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
