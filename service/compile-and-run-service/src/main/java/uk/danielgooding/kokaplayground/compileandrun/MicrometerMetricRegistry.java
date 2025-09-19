package uk.danielgooding.kokaplayground.compileandrun;

import com.netflix.concurrency.limits.MetricRegistry;
import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class MicrometerMetricRegistry implements MetricRegistry {
    private final MeterRegistry meterRegistry;
    private final String metricPrefix;
    private static final Logger logger = LoggerFactory.getLogger(MicrometerMetricRegistry.class);

    /// keep a reference to each value we gauge, since micrometer only holds a weak reference.
    private final Collection<Supplier<Number>> gaugeSuppliers;

    public MicrometerMetricRegistry(MeterRegistry meterRegistry, String metricPrefix) {
        this.meterRegistry = meterRegistry;
        this.metricPrefix = metricPrefix;
        this.gaugeSuppliers = new ArrayList<>();
    }

    private Tags tagsFromNameValuePairs(String[] tagNameValuePairs) {
        List<Tag> tags = new ArrayList<>();
        for (int i = 0; i < tagNameValuePairs.length; i += 2) {
            tags.add(Tag.of(tagNameValuePairs[i], tagNameValuePairs[i + 1]));
        }
        return Tags.of(tags);
    }

    @Override
    public SampleListener distribution(String id, String... tagNameValuePairs) {
        // This is supposed to be a histogram, but it's used just for `inflight` which is clearly a gauge.

        return new SampleListener() {
            private Number current;

            {
                gauge(id, this::getCurrent, tagNameValuePairs);
            }

            @Override
            public void addSample(Number number) {
                current = number;
            }

            public Number getCurrent() {
                return current;
            }
        };
    }

    @Override
    public void gauge(String id, Supplier<Number> supplier, String... tagNameValuePairs) {
        meterRegistry.gauge(
                metricPrefix + id,
                tagsFromNameValuePairs(tagNameValuePairs),
                supplier,
                (valueSupplier) -> {
                    Number value = valueSupplier.get();
                    if (value == null) return Double.NaN;
                    return value.doubleValue();
                });
        gaugeSuppliers.add(supplier);
    }


    @Override
    public Counter counter(String id, String... tagNameValuePairs) {
        io.micrometer.core.instrument.Counter counter =
                meterRegistry.counter(
                        metricPrefix + id,
                        tagsFromNameValuePairs(tagNameValuePairs));

        return counter::increment;
    }
}
