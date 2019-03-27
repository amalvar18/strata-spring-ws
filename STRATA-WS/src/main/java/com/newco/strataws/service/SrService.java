package com.newco.strataws.service;

import java.util.List;
import java.util.Map;

import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.model.ServiceRequestHlpr;

public interface SrService {

    /**
     * Creates a new Service Request in Siebel
     * 
     * @param serviceRequestHlpr
     * @param siebelConfig
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public ServiceRequestHlpr createSR(ServiceRequestHlpr serviceRequestHlpr, SiebelConfig siebelConfig)
            throws Exception;

    /**
     * Updates the SR fields in Siebel
     * 
     * @param srNumber
     * @param serviceRequestHlpr
     * @param siebelConfig
     * @return
     * @throws Exception
     */
    public ServiceRequestHlpr updateSR(String srNumber, ServiceRequestHlpr serviceRequestHlpr,
            SiebelConfig siebelConfig) throws Exception;

    /**
     * Get SR details from Siebel
     * 
     * @param srNumber
     * @param siebelConfig
     * @return
     * @throws Exception
     */
    public ServiceRequestHlpr getSrDetails(String srNumber, SiebelConfig siebelConfig) throws Exception;

    /**
     * Checks whether the SR# exists in Siebel
     * 
     * @param srNumber
     * @param siebelConfig
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public Boolean checkSrValidity(String srNumber, SiebelConfig siebelConfig) throws Exception;

    /**
     * Get SR details from Siebel based on querymap
     * 
     * @param queryFieldsMap
     *            Query map containing entries having key = 'Siebel field name' and value = 'Value to query'
     * @param siebelConfig
     *            Siebel configuration
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public List<ServiceRequestHlpr> querySrFields(Map<String, String> queryFieldsMap,
            SiebelConfig siebelConfig) throws Exception;
}
