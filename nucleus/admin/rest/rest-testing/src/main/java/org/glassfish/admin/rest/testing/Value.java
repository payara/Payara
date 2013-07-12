package org.glassfish.admin.rest.testing;

public abstract class Value {
  abstract Object getJsonValue() throws Exception;
  abstract void print(IndentingStringBuffer sb);
}
