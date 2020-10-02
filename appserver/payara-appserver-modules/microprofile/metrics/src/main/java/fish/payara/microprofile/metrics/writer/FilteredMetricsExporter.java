package fish.payara.microprofile.metrics.writer;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.Tag;

public class FilteredMetricsExporter extends OpenMetricsExporter {

    private final Collection<String> metricNames;

    public FilteredMetricsExporter(Writer out, Collection<String> metricNames) {
        super(out);
        this.metricNames = metricNames;
    }

    protected FilteredMetricsExporter(Type scope, PrintWriter out, Set<String> typeWrittenByGlobalName,
            Set<String> helpWrittenByGlobalName, Collection<String> metricNames) {
        super(scope, out, typeWrittenByGlobalName, helpWrittenByGlobalName);
        this.metricNames = metricNames;
    }

    @Override
    public MetricExporter in(Type scope, boolean asNode) {
        return new FilteredMetricsExporter(scope, out, typeWrittenByGlobalName, helpWrittenByGlobalName, metricNames);
    }

    @Override
    protected void appendTYPE(String globalName, OpenMetricsType type) {
        // Do nothing
    }

    @Override
    protected void appendHELP(String globalName, Metadata metadata) {
        // Do nothing
    }
    
    @Override
    protected void appendValue(String globalName, Tag[] tags, Number value) {
        String key = globalName + tagsToString(tags);
        if (metricNames.contains(key)) {
            out.append(key)
               .append('=')
               .append(roundValue(value))
               .append(',');
        }
    }

}
