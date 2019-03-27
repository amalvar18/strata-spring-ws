package com.newco.strataws.common.metrics;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

@Component
public class MetricFilter implements Filter {

    @Autowired
    private MetricService metricService;

    /*    private static final Logger logger = LoggerFactory.getLogger(MetricFilter.class);*/

    @Override
    public void init(final FilterConfig config) throws ServletException {

        if (metricService == null) {
            metricService = (MetricService) WebApplicationContextUtils.getRequiredWebApplicationContext(
                    config.getServletContext()).getBean("metricService");
            /*Map<?, ?> statusMetricMap;
            statusMetricMap = metricService.getStatusMetric();
            for (Map.Entry<?, ?> pair : statusMetricMap.entrySet()) {
                logger.debug(pair.getKey() + " : " + pair.getValue());
            }*/
            /*logger.debug("metricService--> {}");*/
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
            final FilterChain chain) throws java.io.IOException, ServletException {
        final HttpServletRequest httpRequest = ((HttpServletRequest) request);
        final String req = httpRequest.getMethod() + " " + httpRequest.getRequestURI();

        chain.doFilter(request, response);

        final int status = ((HttpServletResponse) response).getStatus();
        metricService.increaseCount(req, status);
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

}
