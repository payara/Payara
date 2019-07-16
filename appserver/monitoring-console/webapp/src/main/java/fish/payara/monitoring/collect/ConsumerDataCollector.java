package fish.payara.monitoring.collect;

public class ConsumerDataCollector implements MonitoringDataCollector {

    private static final char TAG_SEPARATOR = ' ';
    private static final char TAG_ASSIGN = '=';

    private final MonitoringDataSink sink;
    private final StringBuilder tags;

    public ConsumerDataCollector(MonitoringDataSink sink) {
        this(sink, new StringBuilder());
    }

    public ConsumerDataCollector(MonitoringDataSink sink, StringBuilder fullKey) {
        this.sink = sink;
        this.tags = fullKey;
    }

    @Override
    public MonitoringDataCollector collect(CharSequence key, long value) {
        int len = tags.length();
        if (tags.length() > 0) {
            tags.append(TAG_SEPARATOR);
        }
        tags.append(key);
        accept(value);
        tags.setLength(len);
        return this;
    }

    @Override
    public MonitoringDataCollector tag(CharSequence name, CharSequence value) {
        StringBuilder tagged = new StringBuilder(tags);
        if (tagged.length() > 0) {
            tagged.append(TAG_SEPARATOR);
        }
        tagged.append(name).append(TAG_ASSIGN).append(value);
        return new ConsumerDataCollector(sink, tagged);
    }

    private void accept(long value) {
        sink.accept(tags, value);
    }
}
