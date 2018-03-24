/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.microprofile.config.converters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 * @author steve
 */
public class CommonSenseConverter implements Converter<Object> {
    
    private Class clazz;
    
    public CommonSenseConverter(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object convert(String string) {
        Object result = null;
        try {
            // need to do common sense reflected conversion
            Constructor method = clazz.getConstructor(String.class);
            result = method.newInstance(string);
            
        } catch (NoSuchMethodException | SecurityException |InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            // ok do noting as it may not exist
        }
        return result;
    }
    
}
