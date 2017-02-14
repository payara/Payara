/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.micro.cdi;

/**
 * Interface for the ClusteredCDIEventBus
 * @author Steve Millidge <Payara Services Limited>
 */
public interface ClusteredCDIEventBus {

    /**
     * Initialise must be called in the application to start the flow of event
     */
    public void initialize();
    
}
