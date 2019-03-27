package com.newco.strataws.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.newco.strataws.common.StrataWSConstants;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.model.ActivityHlpr;
import com.newco.strataws.service.ActivityService;
import com.newco.strataws.service.SrService;
import com.newco.strataws.siebel.ActivityUtility;
import com.newco.strataws.siebel.SiebelUtility;

@Service("activityService")
public class ActivityServiceImpl implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    @Autowired
    SrService srService;

    @Override
    public ActivityHlpr createActivity(ActivityHlpr actHlpr, SiebelConfig sblConfig) throws Exception {

        ActivityUtility activityUtility = null;
        ActivityHlpr createdActHlpr = actHlpr;
        String createdActId = "";
        try {
            activityUtility = new ActivityUtility(sblConfig);
            createdActId = activityUtility.createActivity(actHlpr);
            if (StringUtils.isNotBlank(createdActId)) {
                actHlpr = fetchActivityById(createdActId, sblConfig);
            }
        } finally {
            SiebelUtility.closeConnection(activityUtility);
            activityUtility = null;
        }
        return actHlpr;
    }

    @Override
    public ActivityHlpr fetchActivityById(String activityId, SiebelConfig sblConfig) throws Exception {

        ActivityUtility activityUtility = null;
        ActivityHlpr activityHlpr;
        try {
            activityUtility = new ActivityUtility(sblConfig);
            activityHlpr = activityUtility.fetchActivity(activityId);
        } finally {
            SiebelUtility.closeConnection(activityUtility);
        }
        return activityHlpr;
    }

    @Override
    public ActivityHlpr updateActivity(ActivityHlpr actHlpr, SiebelConfig sblConfig) throws Exception {
        ActivityUtility activityUtility = new ActivityUtility(sblConfig);
        ActivityHlpr updatedActHlpr = actHlpr;
        String updateActSrNumber = actHlpr.getParentSrNum();

        /*Check if valid parent SR# if updated*/
        if (StringUtils.isNotBlank(updateActSrNumber)) {

            if (!srService.checkSrValidity(updateActSrNumber, sblConfig)) {
                throw new Exception(StrataWSConstants.SR_VALUE + " "
                        + StrataWSConstants.NOT_FOUND_IN_SBL_MSG);
            }
        }

        String updatedActId = activityUtility.updateActivity(actHlpr);

        if (StringUtils.isNotBlank(updatedActId)) {
            updatedActHlpr = activityUtility.fetchActivity(updatedActId);
        } else {
            logger.info("Activity details not updated for {}" + actHlpr.getActivityId());
        }

        return updatedActHlpr;
    }

    public Boolean checkActivityValidity(String actId, SiebelConfig siebelConfig) throws Exception {

        boolean validActFlag = false;
        ActivityUtility actUtility = null;
        try {
            actUtility = new ActivityUtility(siebelConfig);
            validActFlag = actUtility.checkActValidity(actId);
        } finally {
            SiebelUtility.closeConnection(actUtility);
        }
        return validActFlag;
    }
}
