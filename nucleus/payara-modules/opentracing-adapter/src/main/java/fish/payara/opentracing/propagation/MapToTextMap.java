/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.opentracing.propagation;

import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author andrew
 */
public class MapToTextMap implements TextMap {

    private final Map<String, String> map;

    public MapToTextMap(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return map.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        map.put(key, value);
    }
    
}
