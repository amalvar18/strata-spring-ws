package com.newco.strataws.controller;

import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.common.util.AuthUtils;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.exception.NotFoundException;
import com.newco.strataws.model.ServiceRequestHlpr;
import com.newco.strataws.service.SrService;
import com.newco.strataws.siebel.SiebelUtility;
import com.newco.strataws.siebel.SvReqUtility;
import com.newco.surya.base.exceptions.AncestorSiebelUtilityException;

@RestController
public class SvreqRestController {

    @Autowired
    SrService srService;

    @Autowired
    SiebelConfig siebelConfig;

    @Autowired
    Map<String, String> sblSrvrMap;

    private static final Logger logger = LoggerFactory.getLogger(SvreqRestController.class);

    @PostMapping(value = "/svreq/{client}", headers = "Accept=application/json")
    public ResponseEntity<?> createSR(@RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestBody ServiceRequestHlpr serviceRequestHlpr, @PathVariable("client") String clientName) {

        /* logger.debug("Recieved credentials in header - UserId: {} and Password: {}", sblLoginId,
        /*Check if Siebel credentials present in header */
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errorMessage = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in SvreqRestController::createSR()--> - will return NOT ACCEPTABLE: "
                    + errorMessage);
            return new ResponseEntity<>(errorMessage, HttpStatus.NOT_ACCEPTABLE);
        }

        logger.debug("Received ServiceRequestHlpr: {}", serviceRequestHlpr.toString());
        ResponseEntity<ServiceRequestHlpr> responseEntity = null;

        try {
            /*Update siebel config based on request data*/
            /* Will try to fetch server URL from sblSrvrMap config*/
            if (StringUtils
                    .isNotBlank(sblSrvrMap.get(sblEnv + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                logger.info("Request header SblEnv value: {} - updated siebel URL from config map to {}",
                        sblEnv, siebelConfig.getSblServerURL());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "URL:", siebelConfig.getSblServerURL()));
            }
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            siebelConfig.setSblEnv(sblEnv);
            /*logger.debug(serviceRequestHlpr.toString());*/
            ServiceRequestHlpr createdServiceRequestHlpr = srService.createSR(serviceRequestHlpr,
                    siebelConfig);

            responseEntity = new ResponseEntity<ServiceRequestHlpr>(createdServiceRequestHlpr,
                    HttpStatus.OK);
        } catch (LoginException logEx) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + logEx.getMessage();
            logger.error("LoginException in SvreqRestController::createSR() -  will return Unauthorized:");
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        } catch (NotFoundException ne) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            logger.error("NotFoundException in SvreqRestController::createSR() -->" + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in SvreqRestController::createSR()--> ", ase);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in SvreqRestController::createSR()--> ", e);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            /* context.close();*/
        }
        return responseEntity;
    }

    @PutMapping(value = "/svreq/{client}/{srNum}", headers = "Accept=application/json")
    public ResponseEntity<?> updateSR(@RequestBody ServiceRequestHlpr serviceRequestHlpr,
            @PathVariable("srNum") String srNum, @PathVariable("client") String clientName,
            @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd) {

        /* AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);*/

        /* logger.debug("Recieved credentials in header - UserId: {} and Password: {}", sblLoginId,
        sblPasswrd);*/
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in SvreqRestController::updateSR()--> - will return NOT ACCEPTABLE: "
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_ACCEPTABLE);
        }

        /*Verify SR# is not null or blank*/
        if (StringUtils.isBlank(srNum)) {
            String errMsg = StringUtils.joinWith(" ", "error-->", StrataWSConstants.SR_VALUE,
                    StrataWSConstants.NUM_VALUE, StrataWSConstants.NULL_OR_BLANK_MSG);
            logger.error("Error in SvreqRestController::updateSR()--> - will return Bad Request:"
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }

        logger.debug("Received ServiceRequestHlpr: {}", serviceRequestHlpr.toString());
        ResponseEntity<ServiceRequestHlpr> respEntity = null;

        try {
            /*Update siebel config based on request data*/
            /* Will try to fetch server URL from sblSrvrMap config*/
            if (StringUtils
                    .isNotBlank(sblSrvrMap.get(sblEnv + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                logger.info("Request header SblEnv value: {} - updated siebel URL from config map to {}",
                        sblEnv, siebelConfig.getSblServerURL());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "URL:", siebelConfig.getSblServerURL()));
            }
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            siebelConfig.setSblEnv(sblEnv);
            /*logger.debug(serviceRequestHlpr.toString());*/

            logger.debug("Using SiebelConfig {}", siebelConfig.toString());

            ServiceRequestHlpr updServiceRequestHlpr = srService.updateSR(srNum, serviceRequestHlpr,
                    siebelConfig);

            respEntity = new ResponseEntity<>(updServiceRequestHlpr, HttpStatus.OK);
        } catch (LoginException logEx) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + logEx.getMessage();
            logger.error("LoginException in SvreqRestController::updateSR() -  will return Unauthorized:");
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        } catch (NotFoundException ne) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in SvreqRestController::updateSR()--> ", ase);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in SvreqRestController::updateSR()--> ", e);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return respEntity;
    }

    /**
     * Controller method to get SR details
     * 
     * @param srNum
     * @param clientName
     * @param sblEnv
     * @param sblLoginId
     * @param sblPasswrd
     * @return
     * @author Amal Varghese
     */
    @GetMapping(value = "/svreq/{client}/{srNum}")
    public ResponseEntity<?> getSrDetails(@PathVariable("srNum") String srNum,
            @PathVariable("client") String clientName,
            @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd) {

        /* logger.debug("Recieved credentials in header - UserId: {} and Password: {}", sblLoginId,
        sblPasswrd);*/
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in SvreqRestController::getSrDetails()--> - will return NOT ACCEPTABLE: "
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_ACCEPTABLE);
        }

        logger.debug("Received get request for {} SR#: [{}]", clientName, srNum);

        /*Verify SR# is not null or blank*/
        if (StringUtils.isBlank(srNum)) {
            String errMsg = StringUtils.joinWith(" ", "error-->", StrataWSConstants.SR_VALUE,
                    StrataWSConstants.NUM_VALUE, StrataWSConstants.NULL_OR_BLANK_MSG);
            logger.error("Error in SvreqRestController::getSrDetails() - will return Bad Request:"
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<ServiceRequestHlpr> respEntity = null;

        try {
            /*Update siebel config based on request data*/
            /* Will try to fetch server URL from sblSrvrMap config*/
            if (StringUtils
                    .isNotBlank(sblSrvrMap.get(sblEnv + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                logger.info("Request header SblEnv value: {} - updated siebel URL from config map to {}",
                        sblEnv, siebelConfig.getSblServerURL());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "URL:", siebelConfig.getSblServerURL()));
            }
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            siebelConfig.setSblEnv(sblEnv);
            /*logger.debug(serviceRequestHlpr.toString());*/

            ServiceRequestHlpr svreq = srService.getSrDetails(srNum, siebelConfig);

            respEntity = new ResponseEntity<ServiceRequestHlpr>(svreq, HttpStatus.OK);
            logger.debug("Fetched SR details: {}", svreq.toString());
        } catch (LoginException logEx) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + logEx.getMessage();
            logger.error("LoginException in SvreqRestController::getSrDetails() -  will return Unauthorized:");
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        } catch (NotFoundException ne) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in SvreqRestController::getSrDetails()--> ", ase);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in SvreqRestController::getSrDetails()--> ", e);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return respEntity;
    }

    /**
     * Controller method for SR Query
     * 
     * @param clientName
     *            Name of client (abbr. as in Siebel. e.g., BAC)
     * @param sblEnv
     *            Siebel MCS2 or MCS3. Will default to MCS3
     * @param sblLoginId
     *            Siebel functional Id
     * @param sblPasswrd
     *            Password for Siebel functional Id
     * @param queryFieldsMap
     *            Map containing Siebel field name as key and value
     * @return
     * @author Amal Varghese
     */
    @PostMapping(value = "/svreqSearch/{client}")
    public ResponseEntity<?> querySrFields(@PathVariable("client") String clientName,
            @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestBody Map<String, String> queryFieldsMap) {

        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in SvreqRestController::querySrFields()--> - will return NOT ACCEPTABLE: "
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_ACCEPTABLE);
        }

        ResponseEntity<List<ServiceRequestHlpr>> respEntity = null;

        try {
            /* Will try to fetch server URL from sblSrvrMap config*/
            if (StringUtils
                    .isNotBlank(sblSrvrMap.get(sblEnv + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                logger.info("Request header SblEnv value: {} - updated siebel URL from config map to {}",
                        sblEnv, siebelConfig.getSblServerURL());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "URL:", siebelConfig.getSblServerURL()));
            }

            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            siebelConfig.setSblEnv(sblEnv);

            List<ServiceRequestHlpr> svreqList = null;

            svreqList = srService.querySrFields(queryFieldsMap, siebelConfig);

            respEntity = new ResponseEntity<List<ServiceRequestHlpr>>(svreqList, HttpStatus.OK);
            logger.debug("Fetched All SRs");
            // respEntity = new ResponseEntity<ServiceRequestHlpr>(svreq, HttpStatus.OK);
            // logger.debug("Fetched SR details: {}", svreq.toString());
        } catch (LoginException logEx) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + logEx.getMessage();
            logger.error("LoginException in SvreqRestController::querySrFields() -  will return Unauthorized:");
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        } catch (NotFoundException ne) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in SvreqRestController::querySrFields()--> ", ase);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in SvreqRestController::querySrFields()--> ", e);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return respEntity;
    }

    /**
     * @deprecated Use querySRFields instead
     * 
     */
    @GetMapping(value = "/svreqAll/{client}")
    public ResponseEntity<?> getAllSR(@PathVariable("client") String clientName,
            @RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd) {

        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errMsg = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in SvreqRestController::getSrDetails()--> - will return UNAUTHORIZED:"
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<List<ServiceRequestHlpr>> respEntity = null;

        try {
            /* Will try to fetch server URL from sblSrvrMap config*/
            if (StringUtils
                    .isNotBlank(sblSrvrMap.get(sblEnv + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX))) {
                siebelConfig.setSblServerURL(sblSrvrMap.get(sblEnv
                        + StrataWSConstants.SRVR_MAP_URL_KEY_SUFFIX));
                logger.info("Request header SblEnv value: {} - updated siebel URL from config map to {}",
                        sblEnv, siebelConfig.getSblServerURL());
            } else {
                logger.info(StringUtils.joinWith(" ", StrataWSConstants.SRVR_MAP_CONFIG_FETCH_ERROR,
                        "URL:", siebelConfig.getSblServerURL()));
            }

            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            siebelConfig.setSblEnv(sblEnv);
            /*logger.debug(serviceRequestHlpr.toString());*/

            SvReqUtility svReqUtility = null;
            List<ServiceRequestHlpr> svreq;
            try {
                svReqUtility = new SvReqUtility(siebelConfig);
                svreq = svReqUtility.getAllSr("Automation On-Demand");
            } finally {
                SiebelUtility.closeConnection(svReqUtility);
            }
            /*for(int i=0;i<svreq.size();i++){
              
            }*/
            respEntity = new ResponseEntity<List<ServiceRequestHlpr>>(svreq, HttpStatus.OK);
            logger.debug("Fetched SR details: {}", svreq.toString());
            // respEntity = new ResponseEntity<ServiceRequestHlpr>(svreq, HttpStatus.OK);
            // logger.debug("Fetched SR details: {}", svreq.toString());
        } catch (NotFoundException ne) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            return new ResponseEntity<>(errMsg, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in SvreqRestController::getSrDetails()--> ", ase);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            String errMsg = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : " + e.getMessage();
            logger.error("Exception in SvreqRestController::updateSR()--> ", e);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return respEntity;
    }

    /*    public boolean verifyHeaders(String sblEnv, String sblLoginId, String sblPasswrd) {
            if (StringUtils.isBlank(sblLoginId) || StringUtils.isBlank(sblPasswrd)
                    || StringUtils.isBlank(sblEnv)) {
                return false;
            } else {
                return true;
            }
        }*/
    /*    @PostMapping(value = "/svreq", headers = "Accept=application/json")
        public ResponseEntity createSR(@RequestBody ServiceRequestHlpr serviceRequestHlpr) {

             ResponseEntity responseEntity = new ResponseEntity(); 

            AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

            // SiebelConfig siebelConfig = (SiebelConfig) context.getBean(SiebelConfig.class);

            try {
                ServiceRequestHlpr svreq = srService.createSR(serviceRequestHlpr, siebelConfig);

                return new ResponseEntity<>(svreq, HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();

                String errorMessage = "error--> " + e;
                return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
            } finally {
                context.destroy();
            }

        }*/

    /*@PutMapping(value = "/svreq/{srNum}", headers = "Accept=application/json")
    public ResponseEntity updateSR(@RequestBody ServiceRequestHlpr serviceRequestHlpr,
            @PathVariable("srNum") String srNum) {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        try {
            ServiceRequestHlpr svreq = srService.updateSR(srNum, serviceRequestHlpr, siebelConfig);

            return new ResponseEntity<>(svreq, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();

            String errorMessage = "error--> " + e;
            return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
        } finally {
            context.destroy();
        }

    }*/

    /*@GetMapping(value = "/svreq/{srNum}")
    public ResponseEntity getSrDetails(@PathVariable("srNum") String srNum) {
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        try {
            ServiceRequestHlpr svreq = srService.getSrDetails(srNum, siebelConfig);

            return new ResponseEntity<>(svreq, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();

            String errorMessage = "error--> " + e;
            return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
        } finally {
            context.destroy();
        }
    }*/

}
