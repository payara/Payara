package org.glassfish.cdi.transaction;

import javax.transaction.Transactional;

/**
 * User: paulparkinson
 * Date: 5/3/13
 * Time: 10:46 AM
 */
public class BeanMethodLevelAll {

@javax.transaction.Transactional(value = Transactional.TxType.MANDATORY)
    public String fooMANDATORY() {
      return "In " + this + ".foo()";
    }

@javax.transaction.Transactional(value = Transactional.TxType.NEVER)
    public String fooNEVER() {
      return "In " + this + ".foo()";
    }

@javax.transaction.Transactional(value = Transactional.TxType.NOT_SUPPORTED)
    public String fooNOT_SUPPORTED() {
      return "In " + this + ".foo()";
    }

@javax.transaction.Transactional(value = Transactional.TxType.REQUIRED)
    public String fooREQUIRED() {
      return "In " + this + ".foo()";
    }

@javax.transaction.Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public String fooREQUIRES_NEW() {
      return "In " + this + ".foo()";
    }

@javax.transaction.Transactional(value = Transactional.TxType.SUPPORTS)
    public String fooSUPPORTS() {
      return "In " + this + ".foo()";
    }
}
