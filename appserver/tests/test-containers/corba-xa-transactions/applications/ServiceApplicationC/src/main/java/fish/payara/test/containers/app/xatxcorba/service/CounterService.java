package fish.payara.test.containers.app.xatxcorba.service;

import java.util.concurrent.atomic.AtomicInteger;
import javax.ejb.Singleton;

/**
 * @author fabio
 */
@Singleton
public class CounterService {

    private final AtomicInteger counter = new AtomicInteger();

    public Integer getNextId(){
        return counter.incrementAndGet();
    }
}
