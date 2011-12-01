package org.glassfish.virtualization.virtualbox;

import org.glassfish.virtualization.config.Virtualization;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: 9/19/11
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
@Configured
public interface VirtualBoxVirtualization extends Virtualization {

    @Attribute
    String getEmulatorPath();
    void setEmulatorPath(String emulatorPath);
}
