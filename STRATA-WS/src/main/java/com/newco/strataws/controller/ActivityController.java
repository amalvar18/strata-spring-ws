package com.newco.strataws.controller;

import java.util.Map;

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
import com.newco.strataws.model.ActivityHlpr;
import com.newco.strataws.service.ActivityService;
import com.newco.surya.base.exceptions.AncestorSiebelUtilityException;

@RestController
public class ActivityController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

    @Autowired
    SiebelConfig siebelConfig;

    @Autowired
    ActivityService activityService;

    @Autowired
    Map<String, String> sblSrvrMap;

    @GetMapping(value = "/activity/{client}/{activityId}")
    public ResponseEntity<?> fetchActivityDetails(@RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @PathVariable("client") String clientName, @PathVariable("activityId") String activityId) {

        /*Check if Siebel credentials present in header */
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errorMessage = "error--> " + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in ActivityController::fetchActivityDetails() - will return Unauthorized: "
                    + errorMessage);
            return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
        }

        logger.debug("Received get request for Act Id: {}", activityId);

        if (StringUtils.isBlank(activityId)) {
            String errMsg = StringUtils.joinWith(" ", "error-->", StrataWSConstants.ACTIVITY_VALUE,
                    StrataWSConstants.NUM_VALUE, StrataWSConstants.NULL_OR_BLANK_MSG);
            logger.error("Error in ActivityController::fetchActivityDetails() - will return Bad Request:"
                    + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<ActivityHlpr> respEntity = null;

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
            siebelConfig.setSblEnv(sblEnv);
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);

            ActivityHlpr actHlpr = activityService.fetchActivityById(activityId, siebelConfig);
            respEntity = new ResponseEntity<ActivityHlpr>(actHlpr, HttpStatus.OK);
        } catch (NotFoundException ne) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            return new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in ActivityController::fetchActivityDetails()--> ", ase);
            return new ResponseEntity<>(errorMessage, HttpStatus.CONFLICT);
        } catch (Exception e) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + e.getMessage();
            logger.error("Exception in ActivityController::fetchActivityDetails()--> ", e);
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return respEntity;
    }

    @PostMapping(value = "/activity/{client}", headers = "Accept=application/json")
    public ResponseEntity<?> createActivity(@RequestHeader(value = "Siebel-Env") String sblEnv,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestBody ActivityHlpr activityHlpr) {

        /*Check if Siebel credentials present in header */
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errorMessage = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in ActivityController::createActivity() - will respond with Unauthorized: "
                    + errorMessage);
            return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
        }

        logger.debug("Received Activity: {}", activityHlpr.toString());
        ResponseEntity<ActivityHlpr> respEntity = null;

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
            siebelConfig.setSblEnv(sblEnv);
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);

            ActivityHlpr actHlpr = activityService.createActivity(activityHlpr, siebelConfig);
            respEntity = new ResponseEntity<ActivityHlpr>(actHlpr, HttpStatus.OK);
        } catch (AncestorSiebelUtilityException ase) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in ActivityController::createActivity()--> ", ase);
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + e.getMessage();
            logger.error("Exception in ActivityController::createActivity()--> ", e);
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return respEntity;
    }

    @PutMapping(value = "/activity/{client}/{activityId}", headers = "Accept=application/json")
    public ResponseEntity<?> updateActivity(@RequestHeader(value = "Siebel-Env") String sblEnv,
            @PathVariable("client") String clientName, @PathVariable("activityId") String activityId,
            @RequestHeader(value = "Siebel-Login-Id") String sblLoginId,
            @RequestHeader(value = "Siebel-Password") String sblPasswrd,
            @RequestBody ActivityHlpr activityHlpr) {

        /*Check if Siebel credentials present in header */
        if (!AuthUtils.verifyHeaders(sblEnv, sblLoginId, sblPasswrd)) {
            String errorMessage = "error-->" + StrataWSConstants.SBL_CRED_MISSING_MSG;
            logger.error("Error in ActivityController::updateActivity() - will return Unauthorized: "
                    + errorMessage);
            return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
        }
        if (StringUtils.isBlank(activityId)) {
            String errMsg = StrataWSConstants.ACTIVITY_VALUE + " " + StrataWSConstants.ID_VALUE + " "
                    + StrataWSConstants.NULL_OR_BLANK_MSG;
            logger.error("Error in ActivityController::updateActivity()--> " + errMsg);
            return new ResponseEntity<>(errMsg, HttpStatus.BAD_REQUEST);
        }
        ResponseEntity<ActivityHlpr> respEntity = null;

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
            siebelConfig.setSblEnv(sblEnv);
            siebelConfig.setLogin(sblLoginId);
            siebelConfig.setSblPasswrd(sblPasswrd);
            activityHlpr.setActivityId(activityId);

            ActivityHlpr actHlpr = activityService.updateActivity(activityHlpr, siebelConfig);

            respEntity = new ResponseEntity<ActivityHlpr>(actHlpr, HttpStatus.OK);
        } catch (NotFoundException ne) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ne.getCustomMessage();
            return new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
        } catch (AncestorSiebelUtilityException ase) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + ase.getErrorMessage();
            logger.error("SiebelUtilityException in ActivityController::updateActivity()--> ", ase);
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {

            String errorMessage = "error--> " + StrataWSConstants.GENERIC_ERROR_MSG + " : "
                    + e.getMessage();
            logger.error("Exception in ActivityController::updateActivity()--> ", e);
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return respEntity;
    }
}
