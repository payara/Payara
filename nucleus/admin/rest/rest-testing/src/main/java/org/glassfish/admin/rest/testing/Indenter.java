package org.glassfish.admin.rest.testing;

class Indenter {

    public static final int INDENT = 2;
    private String indentPerLevel;
    private int level = 0;
    private String indent;

    Indenter() {
        computeIndentPerLevel();
        computeIndent();
    }

    String getIndent() {
        return indent;
    }
    
    void indent() {
        level++;
        computeIndent();
    }
    
    void undent() {
        level--;
        computeIndent();
    }

    private void computeIndentPerLevel() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < INDENT; i++) {
            sb.append(" ");
        }
        indentPerLevel = sb.toString();
    }

    private void computeIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(indentPerLevel);
        }
        indent = sb.toString();
    }
}
