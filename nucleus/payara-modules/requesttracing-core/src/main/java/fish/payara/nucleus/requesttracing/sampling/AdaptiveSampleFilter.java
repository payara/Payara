package fish.payara.nucleus.requesttracing.sampling;

import com.google.common.io.Files;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdaptiveSampleFilter extends SampleFilter {

    private final Integer targetCount;
    private final Integer timeValue;
    private final TimeUnit timeUnit;

    private int sampledCount;
    private double projectedCount;
    private long startTimeMillis;

    public AdaptiveSampleFilter(double sampleRate, Integer targetCount, Integer timeValue, TimeUnit timeUnit) {
        super(sampleRate);
        if (targetCount == null || timeValue == null || timeUnit == null) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + " requires a non null targetCount, timeValue and timeUnit.");
        }
        this.targetCount = targetCount;
        this.timeValue = timeValue;
        this.timeUnit = timeUnit;

        this.sampledCount = 0;
        this.projectedCount = 0;
    }

    @Override
    public boolean sample() {
        boolean sample = super.sample();

        // If a new request has been sampled
        if (sample) {

            // If this is the first request of the time slot, record the time it happened
            if (sampledCount == 0 && projectedCount == 0) {
                startTimeMillis = System.currentTimeMillis();
            } else {
                // Else update the projected count
                updateProjectedCount();
            }

            // If the time slot is over, reset the stats
            if (getTimePassedInMillis() > TimeUnit.MILLISECONDS.convert(timeValue, timeUnit)) {
                sampledCount = 0;
                projectedCount = 0;
            }

            sampledCount++;
        }

        try {
            Files.append(this.toString(), Paths.get("/home/matt/adaptiveFilterResults.txt").toFile(), Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(AdaptiveSampleFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sample;
    }

    private void updateProjectedCount() {
        // Record how long passed since the first traced request in milliseconds
        double passedTimeInMillis = getTimePassedInMillis();
        // Get the timeValue in milliseconds
        double timeValueInMillis = TimeUnit.MILLISECONDS.convert(timeValue, timeUnit);

        // Prevent zero divisor
        if (sampledCount == 1) {
            // Divide to get how many requests are predicted in the configured time slot
            projectedCount = timeValueInMillis / passedTimeInMillis;
        }
        if (sampledCount > 1) {
            double oneEvery = 1.0 / (projectedCount * timeValueInMillis);

            // Alter this to add the new value
            oneEvery *= (double) (sampledCount - 1) / sampledCount;
            oneEvery += passedTimeInMillis / sampledCount;

            projectedCount = (1.0 / oneEvery) / timeValueInMillis;
        }
    }

    private long getTimePassedInMillis() {
        return System.currentTimeMillis() - startTimeMillis;
    }

    @Override
    public String toString() {
        return String.format("sampledCount: %s; targetCount: %s; timeValue: %s; timeUnit: %s; projectedCount: %s; timeOccurredSeconds: %s;\n", sampledCount, targetCount, timeValue, timeUnit, projectedCount, TimeUnit.SECONDS.convert(getTimePassedInMillis(), TimeUnit.MILLISECONDS));
    }

}
