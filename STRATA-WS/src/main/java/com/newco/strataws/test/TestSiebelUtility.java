package com.newco.strataws.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

import com.newco.strataws.common.SiebelConstants;
import com.newco.strataws.config.AppConfig;
import com.newco.strataws.config.siebel.SiebelConfig;
import com.newco.strataws.model.ServiceRequestHlpr;
import com.newco.surya.base.exceptions.AncestorSiebelUtilityException;
import com.siebel.data.SiebelBusComp;
import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;

@Component
public class TestSiebelUtility {

    protected static SiebelDataBean dataBean = null;

    private static String login = null;
    private static String passwrd = null;

    private static String sblServerURL = null;
    private boolean sblConnected = false;

    /*private static String sealedFile = "sealedpath";
    private static String secretFile = "secretkey.dat";
    */
    private static String sealedFile = null;
    private static String secretFile = null;

    protected int SBL8_EOF_ERRORCODE = 7668105;

    /*	public static void main(String[] args) {

    		//createSR();
    				
    	}*/

    @Autowired
    ServiceRequestHlpr serviceRequestHlpr;

    public void createSR() throws Exception {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        /* 		AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-context-servlet.xml");
        */
        SiebelConfig siebelConfig = (SiebelConfig) context.getBean(SiebelConfig.class);
        sblServerURL = siebelConfig.getSblServerURL();
        login = siebelConfig.getLogin();
        sealedFile = siebelConfig.getSealedFile();
        secretFile = siebelConfig.getSecretFile();

        /*login = context.getBean(SiebelConfig.class).getLogin();*/
        System.out.println("Server URL from prop file: " + sblServerURL);

        String createdSRNumber = null;

        if (!login()) {
            context.close();
            throw new Exception("Siebel Login Failure");

        } else {
            System.out.println("Successfully logged in to Siebel");

            /*ServiceRequestHlpr serviceRequestHlpr = new ServiceRequestHlpr();*/
            ServiceRequestHlpr serviceRequestHlpr = (ServiceRequestHlpr) context
                    .getBean(ServiceRequestHlpr.class);
            System.out.println(serviceRequestHlpr.getSblSrStatus());

            /*check mandatory fields*/
            if (StringUtils.isBlank(serviceRequestHlpr.getDomain())
                    || StringUtils.isBlank(serviceRequestHlpr.getProcess())
                    || StringUtils.isBlank(serviceRequestHlpr.getSubProcess())
                    || StringUtils.isBlank(serviceRequestHlpr.getSblSrStatus())
                    || StringUtils.isBlank(serviceRequestHlpr.getShortDescription())
                    || StringUtils.isBlank(serviceRequestHlpr.getDescription())) {

                context.close();
                throw new AncestorSiebelUtilityException("Must pass mandatory fields - "
                        + "Domain, Process, SubProcess, Status, Desc, Short Desc ");
            }

            SiebelBusObject lBusObject = null;
            SiebelBusComp lBusComp = null;
            SiebelBusComp lMvg = null;
            SiebelBusComp lPick = null;

            try {
                // ----Get the business Object
                lBusObject = dataBean.getBusObject(SiebelConstants.SERVICE_REQUEST_BC_NAME);
                lBusComp = lBusObject.getBusComp(SiebelConstants.SERVICE_REQUEST_BC_NAME);
                lBusComp.newRecord(0);

                /*throw an Exception if Emp Id is null or blank*/
                if (StringUtils.isBlank(serviceRequestHlpr.getSrContactEmpId())) {
                    throw new AncestorSiebelUtilityException(
                            "Empid is Null or Blank This should never happen!");
                }

                lPick = lBusComp.getPicklistBusComp(SiebelConstants.CONTACT_LASTNAME_FIELD);
                lPick.clearToQuery();

                lPick.activateField(SiebelConstants.CONTACT_PAY_1_FIELD);
                lPick.activateField(SiebelConstants.CONTACT_PAY_2_FIELD);
                lPick.activateField(SiebelConstants.CONTACT_PAY_3_FIELD);

                lPick.setSearchSpec("Employee Number", serviceRequestHlpr.getSrContactEmpId());

                String clientAccount = siebelConfig.getClientAccount();
                System.out.println("clientAccount--> " + clientAccount);

                if (StringUtils.isNotBlank(clientAccount)) {
                    lPick.setSearchSpec("Account", escSearchSpec(clientAccount));
                }

                lPick.setViewMode(9);

                lPick.setSortSpec(SiebelConstants.CONTACT_SORT_SPEC);

                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                } else {
                    // ----Should never happen because the Account from the SR must
                    // exist
                    throw new AncestorSiebelUtilityException(
                            "Contact from SR doesn't exist! This should never happen!");
                }

                setSimplePick(lBusComp, serviceRequestHlpr.getSblSrStatus(),
                        SiebelConstants.STATUS_FIELD, "Name");
                setSimplePick(lBusComp, serviceRequestHlpr.getDomain(), SiebelConstants.DOMAIN_FIELD,
                        "Name");
                setSimplePick(lBusComp, serviceRequestHlpr.getProcess(), SiebelConstants.PROCESS_FIELD,
                        "Name");
                setSimplePick(lBusComp, serviceRequestHlpr.getSubProcess(),
                        SiebelConstants.SUBPROCESS_FIELD, "Name");

                setSimplePick(lBusComp, serviceRequestHlpr.getType(), SiebelConstants.SRTYPE_FIELD,
                        "Name");
                setSimplePick(lBusComp, serviceRequestHlpr.getSblSrOwner(),
                        SiebelConstants.SROWNER_FIELD, "Login Name");
                setSimplePick(lBusComp, serviceRequestHlpr.getPriority(), SiebelConstants.PRIORITY_FIELD,
                        "Name");
                setSimplePick(lBusComp, serviceRequestHlpr.getSource(), SiebelConstants.SR_SOURCE_FIELD,
                        "Name");

                lBusComp.setFieldValue(SiebelConstants.SR_SHORT_DESC_FIELD,
                        serviceRequestHlpr.getShortDescription());
                lBusComp.setFieldValue(SiebelConstants.SR_LONG_DESC_FIELD,
                        serviceRequestHlpr.getDescription());

                /*writeOrDeleteNewRecord(lBusComp);*/
                lBusComp.deleteRecord();

                /* createdSRNumber = lBusComp.getFieldValue("SR Number");*/
                System.out.println("SR Number [" + createdSRNumber + "] created");
                System.out.println("**********************************************************");
                serviceRequestHlpr.setSrNumber(createdSRNumber);
            } catch (SiebelException sblEx) {
                System.out.println(sblEx.toString());
                sblEx.printStackTrace();
            }

            logoff();
        }
        context.close();
    }

    public boolean login() throws Exception {

        System.out.println("Attempting Siebel Login");
        System.setProperty("file.encoding", "Cp1252");
        try {

            dataBean = new SiebelDataBean();

            System.out.println("Using sealed File: " + sealedFile + " and secret file: " + secretFile);
            passwrd = decryptPassword(secretFile, sealedFile);
            System.out.println("login: " + login);
            System.out.println("pass: " + passwrd);
            System.out.println("ServerURL: " + sblServerURL);
            dataBean.login(sblServerURL, login, passwrd, "enu");
            sblConnected = true;
        } catch (SiebelException e) {
            System.out.println(e);
        }
        return sblConnected;
    }

    public static String decryptPassword(String pSecretFileName, String pSealedFileName) throws Exception {
        SecretKey key = (SecretKey) readFromFile(pSecretFileName);
        SealedObject sealedObject = (SealedObject) readFromFile(pSealedFileName);
        String algorithmName = sealedObject.getAlgorithm();
        Cipher cipher = Cipher.getInstance(algorithmName);
        cipher.init(Cipher.DECRYPT_MODE, key);
        String text = (String) sealedObject.getObject(cipher);

        return text;
    }

    private static Object readFromFile(String filename) throws Exception {

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Object object = null;
        String lFilepath = "";
        try {
            // read file path from jvp properties

            /*lFilepath = System
            		.getProperty(StarConstants.BASE_CONFIG_FILE_PATH_ENVID)
            		+ "/passwords/";*/

            fis = new FileInputStream(new File(filename));
            ois = new ObjectInputStream(fis);
            object = ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                ois.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return object;
    }

    public void logoff() throws AncestorSiebelUtilityException {

        System.out.println("*******************************************************");
        System.out.println("logoff() --> Attempting Siebel logoff ... ");

        if (sblConnected) {
            try {
                if (dataBean != null) {
                    dataBean.logoff();
                    dataBean = null;
                }
                sblConnected = false;
                /*System.out.println("Successfully logged out of Siebel");*/
            } catch (SiebelException ex) {
                logSiebelException(ex);
                throw new AncestorSiebelUtilityException(ex);
            } catch (Exception ex1) {
                logSiebelException(ex1);
            } finally {
                sblConnected = false;
            }
        }

        System.out.println("logoff() --> Siebel logoff successful ");
        System.out.println("************************************************");
    }

    public void logException(SiebelException pSiebelExp) {
        System.out.println("------------------------------------");
        System.out.println("Detailed Message: " + pSiebelExp.getDetailedMessage());
        System.out.println("Error Message: " + pSiebelExp.getErrorMessage());
        System.out.println("Error Code: " + pSiebelExp.getErrorCode());
        System.out.println("Message: " + pSiebelExp.getMessage());
        System.out.println("Major Number: " + pSiebelExp.getMajorNumber());
        System.out.println("Minor Number: " + pSiebelExp.getMinorNumber());
        System.out.println("------------------------------------");
    }

    private void logSiebelException(Exception pExp) {
        if (pExp instanceof SiebelException) {
            logException((SiebelException) pExp);
        } else {
            System.out.println(pExp);
        }
    }

    public static String escSearchSpec(String pStr) {
        String lRetVal = pStr;
        if (!StringUtils.isBlank(pStr)) {
            StringBuffer buf = new StringBuffer();
            buf.append("'");
            buf.append(pStr.replaceAll("'", "''"));
            buf.append("'");
            lRetVal = buf.toString();
        }
        return lRetVal;
    }

    protected void setSimplePick(SiebelBusComp pBC, String pValue, String pFieldName, String pPickField)
            throws SiebelException {
        SiebelBusComp lPick = null;
        try {
            if (pValue.trim().length() > 0) {
                lPick = pBC.getPicklistBusComp(pFieldName);
                lPick.clearToQuery();
                // ----Use ~= so that it ignores the case
                lPick.setSearchSpec(pPickField, " ~= '" + pValue + "'");
                lPick.executeQuery(true);

                if (lPick.firstRecord()) {
                    lPick.pick();
                } else {
                    // Debug
                    System.out.println("-------------------------------");
                    System.out.println(pFieldName + ": Could not pick value [" + pValue
                            + "] for pick field [" + pPickField + "]");
                    lPick.clearToQuery();
                    lPick.executeQuery(true);
                    boolean lRetrieval = lPick.firstRecord();
                    try {
                        System.out.println("Displaying all values for field [" + pFieldName + "]");
                        while (lRetrieval) {
                            System.out.println(pPickField + " : " + lPick.getFieldValue(pPickField)
                                    + " (" + lPick.getFieldValue("Value") + ")");
                            // lRetrieval = lPick.nextRecord();
                            try {
                                lRetrieval = lPick.nextRecord();
                            } catch (SiebelException sble) {
                                if (sble.getErrorCode() == SBL8_EOF_ERRORCODE)
                                    lRetrieval = false;
                            }

                        }
                        System.out.println("-------------------------------");
                    } catch (Exception e) {
                        System.out.println("Error in setSimpleQuery debug routine");
                    }
                }
            }
        } finally {
            if (lPick != null) {
                lPick.release();
            }
        }
    }

    /**
     * This is a utility function to emulate undo of record in case of exception while writing the record. Siebel does
     * not automatically revert the changes during writing if an exception arises.
     * 
     * @author Abhijit Suvarna on Oct 17, 2008
     * @param pComp
     * @throws SiebelException
     */
    public static void writeOrDeleteNewRecord(SiebelBusComp pComp) throws SiebelException {
        if (pComp != null) {
            try {
                pComp.writeRecord();
            } catch (SiebelException e) {
                try {
                    System.out
                            .println("SiebelUtility::writeOrDeleteNewRecord() --> Exception while writing record, hence trying to delete");
                    pComp.deleteRecord();
                    System.out
                            .println("SiebelUtility::writeOrDeleteNewRecord() --> Delete record successful");
                } catch (Exception e1) {
                    System.out.println(e1);
                }
                throw e;
            }
        }
    }

}
