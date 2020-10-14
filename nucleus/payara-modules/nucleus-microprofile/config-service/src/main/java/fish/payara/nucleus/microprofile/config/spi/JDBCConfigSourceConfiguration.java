/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.microprofile.config.spi;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

@Configured(name = "jdbc-config-source-configuration")
public interface JDBCConfigSourceConfiguration extends ConfigBeanProxy, ConfigExtension  {
     @Attribute(required = true)
    String getJndiName();
    void setJndiName(String jndiName);

    @Attribute(required = true)
    String getTableName();
    void setTableName(String tableName);
    
    @Attribute(required = true)
    String getKeyColumnName();
    void setKeyColumnName(String keyColumName);
    
    @Attribute(required = true)
    String getValueColumnName();
    void setValueColumnName(String valueColumName);
}
