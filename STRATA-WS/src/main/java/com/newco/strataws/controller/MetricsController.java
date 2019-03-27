package com.newco.strataws.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.common.metrics.MetricService;

@RestController
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    @Autowired
    private MetricService metricService;

    @GetMapping(value = "/status-metrics")
    public Map<?, ?> getStatusMetric(HttpServletRequest request) {
        try {
            Map<?, ?> statusMap = new ConcurrentHashMap<Integer, Integer>();
            statusMap = metricService.getStatusMetric();
            return statusMap;
        } catch (Exception e) {

            String errMsg = StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in MetricsController::getStatusMetric()--> ", e);

            Map<HttpStatus, String> myMap = new HashMap<HttpStatus, String>();
            myMap.put(HttpStatus.INTERNAL_SERVER_ERROR, errMsg);
            return myMap;
        }
    }

    @GetMapping(value = "/metrics")
    public Map<?, ?> getMetric() {
        try {
            return metricService.getFullMetric();
        } catch (Exception e) {

            String errMsg = StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in MetricsController::getStatusMetric()--> ", e);

            Map<HttpStatus, String> myMap = new HashMap<HttpStatus, String>();
            myMap.put(HttpStatus.INTERNAL_SERVER_ERROR, errMsg);
            return myMap;
        }
    }

    @GetMapping(value = "/metrics-graph")
    public Object[][] drawMetric() {

        try {
            final Object[][] result = metricService.getGraphData();
            for (int i = 1; i < result[0].length; i++) {
                result[0][i] = result[0][i].toString();
            }
            return result;
        } catch (Exception e) {

            String errMsg = StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in MetricsController::drawMetric()--> ", e);
            return null;
            /*Map<HttpStatus, String> myMap = new HashMap<HttpStatus, String>();
            myMap.put(HttpStatus.INTERNAL_SERVER_ERROR, errMsg);
            return myMap;*/
        }
    }

    /*    @RequestMapping(value = "/graph", method = RequestMethod.GET)
        @ResponseBody
        public String showGraphPage() {
            return "graph";
        }*/
}
