package fish.payara.monitoring.collect;

public class SinkDataCollector implements MonitoringDataCollector {

    private static final char TAG_SEPARATOR = ' ';
    private static final char TAG_ASSIGN = '=';

    private final MonitoringDataSink sink;
    private final StringBuilder tags;

    public SinkDataCollector(MonitoringDataSink sink) {
        this(sink, new StringBuilder());
    }

    public SinkDataCollector(MonitoringDataSink sink, StringBuilder fullKey) {
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
        if (value == null || value.length() == 0) {
            return this;
        }
        StringBuilder tagged = new StringBuilder(tags);
        int nameIndex = indexOf(name);
        if (nameIndex >= 0) {
            tagged.setLength(nameIndex);
        } else if (tagged.length() > 0) {
            tagged.append(TAG_SEPARATOR);
        }
        tagged.append(name).append(TAG_ASSIGN).append(value);
        return new SinkDataCollector(sink, tagged);
    }

    private int indexOf(CharSequence name) {
        String tag = name.toString();
        if (tags.indexOf(tag + TAG_ASSIGN) == 0) {
            return 0;
        }
        int idx = tags.indexOf(TAG_SEPARATOR + tag + TAG_ASSIGN);
        return idx < 0 ? idx : idx + 1;
    }

    private void accept(long value) {
        sink.accept(tags, value);
    }
}
