package com.newco.strataws.service;

import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.model.ActivityHlpr;

public interface ActivityService {

    /**
     * Creates a new Activity in Siebel
     * 
     * @param activityHlpr
     * @param siebelConfig
     * @return activityHlpr
     * @throws Exception
     * @author Amal Varghese
     */
    public ActivityHlpr createActivity(ActivityHlpr activityHlpr, SiebelConfig siebelConfig)
            throws Exception;

    /**
     * Fetches activity details from Siebel based on the activity Id
     * 
     * @param activityId
     * @param siebelConfig
     * @return activityHlpr
     * @throws Exception
     * @author Amal Varghese
     */
    public ActivityHlpr fetchActivityById(String activityId, SiebelConfig siebelConfig) throws Exception;

    /**
     * Updates activity details in Siebel
     * 
     * @param activityHlpr
     * @param siebelConfig
     * @return ActivityHlpr
     * @throws Exception
     * @author Amal Varghese
     */
    public ActivityHlpr updateActivity(ActivityHlpr activityHlpr, SiebelConfig siebelConfig)
            throws Exception;

    /**
     * Checks whether the Activity Id exists in Siebel
     * 
     * @param activityId
     * @param siebelConfig
     * @return
     * @throws Exception
     * @author Amal Varghese
     */
    public Boolean checkActivityValidity(String activityId, SiebelConfig siebelConfig) throws Exception;

}
