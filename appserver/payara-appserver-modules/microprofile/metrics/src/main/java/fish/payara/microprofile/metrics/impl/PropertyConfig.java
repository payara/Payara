package fish.payara.microprofile.metrics.impl;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PropertyConfig {
    
    protected String metricName;
    
    public String getMetricName() {
        return metricName;
    }

    public static <T extends PropertyConfig> T matches(Collection<T> configurations, String metricName) {
        for(PropertyConfig propertyConfig:configurations) {
            if(propertyConfig.getMetricName().contentEquals("*")){
                //check this validation
                return (T) propertyConfig;
            }
            Pattern p = Pattern.compile(metricName.trim());
            Matcher m = p.matcher(propertyConfig.getMetricName().trim());
            if(m.matches()){
                return (T) propertyConfig;
            }
        }
        return null;
    }
}
