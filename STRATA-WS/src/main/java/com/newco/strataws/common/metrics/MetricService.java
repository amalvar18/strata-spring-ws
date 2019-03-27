package com.newco.strataws.common.metrics;

import java.util.Map;

public interface MetricService {

    void increaseCount(final String request, final int status);

    Map<?, ?> getStatusMetric();

    Map<?, ?> getFullMetric();

    Object[][] getGraphData();
}
