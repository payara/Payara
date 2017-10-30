package fish.payara.nucleus.requesttracing.sampling;

import java.util.Random;

public class SampleFilter {
    
    private final Random random;
    private final double sampleRate;
    private final boolean adaptive;
    
    public SampleFilter(double sampleRate, boolean adaptive) {
        this.random = new Random();
        this.sampleRate = sampleRate;
        this.adaptive = adaptive;
    }
    
    public SampleFilter() {
        this(1.0, false);
    }
    
    public boolean sample() {
        return random.nextDouble() < sampleRate;
    }

}
