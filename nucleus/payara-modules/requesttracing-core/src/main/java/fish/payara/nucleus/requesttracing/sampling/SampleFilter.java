package fish.payara.nucleus.requesttracing.sampling;

import java.util.Random;

/**
 * Accepts a rate from 0 to 1
 */
public class SampleFilter {

    private final Random random;
    protected double sampleRate;

    public SampleFilter(double sampleRate) {
        this.random = new Random();
        this.sampleRate = sampleRate;
    }

    public SampleFilter() {
        this(1.0);
    }

    public boolean sample() {
        return random.nextDouble() < sampleRate;
    }

}
