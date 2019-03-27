package com.newco.strataws.common.metrics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

@Service("metricService")
public class MetricServiceImpl implements MetricService {

    private static ConcurrentMap<Integer, Integer> statusMetric;
    private static ConcurrentMap<String, ConcurrentHashMap<Integer, Integer>> metricMap;
    private static ConcurrentMap<String, ConcurrentHashMap<Integer, Integer>> timeMap;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public MetricServiceImpl() {
        super();
        statusMetric = new ConcurrentHashMap<Integer, Integer>();
        metricMap = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
        timeMap = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
    }

    @Override
    public void increaseCount(final String request, final int status) {
        increaseStatusMetric(status);
        increaseMainMetric(request, status);
        updateTimeMap(status);
    }

    @Override
    public Map<Integer, Integer> getStatusMetric() {
        return statusMetric;
    }

    @Override
    public Map<String, ConcurrentHashMap<Integer, Integer>> getFullMetric() {
        return metricMap;
    }

    @Override
    public Object[][] getGraphData() {
        final int colCount = statusMetric.keySet().size() + 1;
        final Set<Integer> allStatus = statusMetric.keySet();
        final int rowCount = timeMap.keySet().size() + 1;

        Map<String, ConcurrentHashMap<Integer, Integer>> sortedMap = new TreeMap<String, ConcurrentHashMap<Integer, Integer>>(
                timeMap);

        final Object[][] result = new Object[rowCount][colCount];
        result[0][0] = "Time";

        int j = 1;
        for (final int status : allStatus) {
            result[0][j] = status;
            j++;
        }
        int i = 1;
        ConcurrentMap<Integer, Integer> tempMap;
        for (final Entry<String, ConcurrentHashMap<Integer, Integer>> entry : sortedMap.entrySet()) {
            result[i][0] = entry.getKey();
            tempMap = entry.getValue();
            for (j = 1; j < colCount; j++) {
                result[i][j] = tempMap.get(result[0][j]);
                if (result[i][j] == null) {
                    result[i][j] = 0;
                }
            }
            i++;
        }

        return result;
    }

    /*@Override
    public Object[][] getGraphData() {
        final int colCount = statusMetric.keySet().size() + 1;
        final Set<Integer> allStatus = statusMetric.keySet();
        final int rowCount = timeMap.keySet().size() + 1;

        final Object[][] result = new Object[rowCount][colCount];
        result[0][0] = "Time";

        int j = 1;
        for (final int status : allStatus) {
            result[0][j] = status;
            j++;
        }
        int i = 1;
        ConcurrentMap<Integer, Integer> tempMap;
        for (final Entry<String, ConcurrentHashMap<Integer, Integer>> entry : timeMap.entrySet()) {
            result[i][0] = entry.getKey();
            tempMap = entry.getValue();
            for (j = 1; j < colCount; j++) {
                result[i][j] = tempMap.get(result[0][j]);
                if (result[i][j] == null) {
                    result[i][j] = 0;
                }
            }
            i++;
        }

        return result;
    }*/

    private void increaseStatusMetric(final int status) {

        final Integer statusCount = statusMetric.get(status);
        if (statusCount == null) {
            statusMetric.put(status, 1);
        } else {
            statusMetric.put(status, statusCount + 1);
        }
    }

    private void increaseMainMetric(final String request, final int status) {

        ConcurrentHashMap<Integer, Integer> statusMap = metricMap.get(request);
        if (statusMap == null) {
            statusMap = new ConcurrentHashMap<Integer, Integer>();
        }

        Integer count = statusMap.get(status);
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        statusMap.put(status, count);
        metricMap.put(request, statusMap);
    }

    private void updateTimeMap(final int status) {

        final String time = dateFormat.format(new Date());
        ConcurrentHashMap<Integer, Integer> statusMap = timeMap.get(time);
        if (statusMap == null) {
            statusMap = new ConcurrentHashMap<Integer, Integer>();
        }

        Integer count = statusMap.get(status);
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        statusMap.put(status, count);
        timeMap.put(time, statusMap);
    }

}
