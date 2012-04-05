package org.glassfish.elasticity.engine.util;

import org.glassfish.elasticity.config.serverbeans.MetricGathererConfig;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/4/12
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class JVMMetricGathererConfig
    implements MetricGathererConfig {

    String name;
    public void setName(String value) throws PropertyVetoException {
         name = value;
    }

    @Override
    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setCollectionRate(int value) throws PropertyVetoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getCollectionRate() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setRetainData(int value) throws PropertyVetoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getRetainData() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAutoStart(boolean value) throws PropertyVetoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getAutoStart() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ConfigBeanProxy getParent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends ConfigBeanProxy> T getParent(Class<T> tClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends ConfigBeanProxy> T createChild(Class<T> tClass) throws TransactionFailure {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ConfigBeanProxy deepCopy(ConfigBeanProxy configBeanProxy) throws TransactionFailure {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String toString() {
        return getName();

    }

}
