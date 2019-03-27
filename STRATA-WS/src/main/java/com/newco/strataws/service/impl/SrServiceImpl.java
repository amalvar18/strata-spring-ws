package com.newco.strataws.service.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.exception.NotFoundException;
import com.newco.strataws.model.ServiceRequestHlpr;
import com.newco.strataws.service.SrService;
import com.newco.strataws.siebel.SiebelUtility;
import com.newco.strataws.siebel.SvReqUtility;

@Service("srService")
public class SrServiceImpl implements SrService {

    private static final Logger logger = LoggerFactory.getLogger(SrServiceImpl.class);

    @Override
    public ServiceRequestHlpr createSR(ServiceRequestHlpr svreq, SiebelConfig sblConfig) throws Exception {

        SvReqUtility svReqUtility = new SvReqUtility(sblConfig);
        String createdSRNum = svReqUtility.createSR(svreq);

        if (StringUtils.isNotBlank(createdSRNum)) {
            svreq = getSrDetails(createdSRNum, sblConfig);
        }

        /*SiebelUtility siebelUtility = new SiebelUtility(sblConfig);
        siebelUtility.createSR(serviceRequestHlpr);*/

        return svreq;
    }

    @Override
    public ServiceRequestHlpr updateSR(String srNumber, ServiceRequestHlpr svreq, SiebelConfig sblConfig)
            throws Exception {

        SvReqUtility svReqUtility = new SvReqUtility(sblConfig);
        ServiceRequestHlpr updatedSRHlpr = svreq;

        if (!checkSrValidity(srNumber, sblConfig)) {
            throw new NotFoundException(StrataWSConstants.SR_VALUE, true);
        }

        String currentSrStatus = svReqUtility.getSrDetails(srNumber).getSblSrStatus();

        if (currentSrStatus.equalsIgnoreCase(SiebelConstants.SBL_CLOSED_SR_STATUS)) {

            logger.error("Error in updateSR()--> " + "Trying to update a closed SR: {}", srNumber);
            throw new Exception(StrataWSConstants.SR_CLOSED_MSG);
        }

        String updatedSrNum = svReqUtility.updateSR(srNumber, svreq);

        if (StringUtils.isNotBlank(updatedSrNum)) {
            updatedSRHlpr = svReqUtility.getSrDetails(updatedSrNum);
        }
        return updatedSRHlpr;
    }

    @Override
    public ServiceRequestHlpr getSrDetails(String srNumber, SiebelConfig sblConfig) throws Exception {

        /*SiebelUtility siebelUtility = new SiebelUtility(siebelConfig);*/
        SvReqUtility svReqUtility = null;
        ServiceRequestHlpr svreq;
        try {
            svReqUtility = new SvReqUtility(sblConfig);
            svreq = svReqUtility.getSrDetails(srNumber);
        } finally {
            SiebelUtility.closeConnection(svReqUtility);
        }
        return svreq;
    }

    public Boolean checkSrValidity(String srNumber, SiebelConfig siebelConfig) throws Exception {

        boolean validSrFlag = false;
        SvReqUtility svReqUtility = null;
        try {
            svReqUtility = new SvReqUtility(siebelConfig);
            validSrFlag = svReqUtility.checkSrValidity(srNumber);
        } finally {
            SiebelUtility.closeConnection(svReqUtility);
        }
        return validSrFlag;
    }

    @Override
    public List<ServiceRequestHlpr> querySrFields(Map<String, String> queryFieldsMap,
            SiebelConfig sblConfig) throws Exception {

        SvReqUtility svReqUtility = null;
        List<ServiceRequestHlpr> svreqList;
        try {
            svReqUtility = new SvReqUtility(sblConfig);
            svreqList = svReqUtility.querySRFields(queryFieldsMap);
        } finally {
            SiebelUtility.closeConnection(svReqUtility);
        }
        return svreqList;
    }

    /*@Override
    public ServiceRequestHlpr updateSR(String srNumber, ServiceRequestHlpr serviceRequestHlpr,
            SiebelConfig siebelConfig) throws Exception {

        SiebelUtility siebelUtility = new SiebelUtility(siebelConfig);
        ServiceRequestHlpr updatedSRHlpr = serviceRequestHlpr;

        String currentSrStatus = siebelUtility.getSrDetails(srNumber).getSblSrStatus();

        if (currentSrStatus.equalsIgnoreCase(SiebelConstants.SBL_CLOSED_SR_STATUS)) {

            logger.error("Error in updateSR() " + "-" + " Trying to update closed SR");
            throw new Exception(StrataWSConstants.SR_CLOSED_MSG);
        }

        String updatedSrNum = siebelUtility.updateSR(srNumber, serviceRequestHlpr);

        if (StringUtils.isNotBlank(updatedSrNum)) {
            updatedSRHlpr = siebelUtility.getSrDetails(updatedSrNum);
        }
        return updatedSRHlpr;
    }*/
}
