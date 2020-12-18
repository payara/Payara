/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.samples.classloaderdata;

/**
 *
 * @author cuba
 */
public class InstanceCountService {
    
    private int previousCount = InstanceCountTracker.getPreviousInstanceCount();
    private int currentCount = InstanceCountTracker.getInstanceCount();
    
    public int getCurrentCount() {
        return currentCount;
    }
    
    public int getPreviousCount() {
        return previousCount;
    }
    
}
