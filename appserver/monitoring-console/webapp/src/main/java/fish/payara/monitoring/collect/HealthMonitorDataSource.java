package fish.payara.monitoring.collect;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "health-monitor-data-collector")
@RunLevel(StartupRunLevel.VAL)
public class HealthMonitorDataSource implements MonitoringDataSource {

    @Override
    public void collect(MonitoringDataCollector collector) {
        // TODO Auto-generated method stub
        
    }


}
