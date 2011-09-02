package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.*;
import static org.glassfish.config.support.Constants.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.Injectable;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;

import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/30/11
 */
@Configured
@SuppressWarnings("unused")
@NotDuplicateTargetName(message="{alert.action.duplicate.name}", payload=AlertAction.class)
public interface AlertAction extends ConfigBeanProxy, Injectable, Named, ReferenceContainer, RefContainer, Payload {

        /**
     * Sets the alert name
     * @param value alert name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name")
    @Override
    public void setName(String value) throws PropertyVetoException;

    @NotTargetKeyword(message="{alert.reserved.name}", payload=AlertAction.class)
    @Pattern(regexp=NAME_SERVER_REGEX, message="{alert.invalid.name}", payload=AlertAction.class)
    @Override
    public String getName();

   /**
     * Sets the action state
     * @param value action state
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="state", defaultValue = "ok")
    public void setState(String value) throws PropertyVetoException;

    @Attribute
    public String getState();

    /**
      * Sets the action state
      * @param value action state
      * @throws PropertyVetoException if a listener vetoes the change
      */
     @Param(name="action-ref", primary = true)
     public void setAction(String value) throws PropertyVetoException;

     @Attribute
     public String getAction();

}
