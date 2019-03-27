package com.newco.strataws.common;

public class StrataWSConstants {

    public static final String SBL_LOGIN_FAILED_MSG = "Siebel Login Failure";
    public static final String SBL_LOGIN_SUCCESS_MSG = "Successfully logged in to Siebel";

    public static final String GENERIC_ERROR_MSG = "Webservice encountered an error";
    public static final String SFTP_REFUSED_CONNECTION_MSG = "SFTP Connection Refused";
    public static final String FILE_DOWNLOAD_FAILED_MSG = "Failed to download the file";
    public static final String FILE_NOT_FOUND_MSG = "File was not found at the location";

    public static final String MANDATORY_FIELDS_EMPTY_MSG = "Must pass mandatory fields";
    public static final String ACT_MANDATORY_FIELDS_MSG = "Type, Status, Comments";
    public static final String SR_MANDATORY_FIELDS_MSG = "Domain, Process, SubProcess, Status, Descr, Short Descr, Contact Emp Id";

    public static final String FILE_INPUT_FAILED_MSG = "Failed to read the file";
    public static final String FILE_UPLOAD_FAILED_MSG = "Failed to upload the file to server";
    public static final String FILE_EMPTY_MSG = "Nothing was uploaded because the file provided was empty";
    public static final String FILE_EXTN_INVALID_MSG = "The file type you are trying to upload is not supported";
    public static final String SBL_FILE_RECORD_CREATION_FAILED_MSG = "Error while creating file record in Siebel";
    public static final String SBL_FILE_RECORD_ALREADY_EXISTS_MSG = "File record already exists for the SR or activity in Siebel"
            + " Please ensure the file name is unique";

    public static final String NULL_OR_BLANK_MSG = "is null or blank";
    public static final String EMP_ID_NULL_OR_BLANK_MSG = "Employee Id is null or blank";
    public static final String SBL_CONTACT_NOT_FOUND_MSG = "Contact was not found in Siebel - Please check employee Id and/or account";
    public static final String FILEPATH_NULL_OR_BLANK_MSG = "File path is null or blank";

    public static final String SR_VALUE = "Service Request";
    public static final String ACTIVITY_VALUE = "Activity";
    public static final String ID_VALUE = "Id";
    public static final String NUM_VALUE = "Number";
    public static final String PARENT_VALUE = "Parent Id";

    public static final String WINDOWS_PATH_PREFIX = "\\\\";
    public static final String UNIX_PATH_PREFIX = "/";

    public static final String ATTACHMENT_ORIGINAL_MESSAGE_FILENAME = "Original Message.txt";

    public static final String SBL_CRED_MISSING_MSG = "Request header is missing Siebel MCS environment or credentials";
    public static final String SBL_FS_CRED_MISSING_MSG = "Request header is missing Siebel SFTP credentials";

    public static final String NOT_FOUND_IN_SBL_MSG = "not found in Siebel";
    public static final String SR_CLOSED_MSG = "SR is closed - Cannot modify closed SRs";

    public static final String SRVR_MAP_URL_KEY_SUFFIX = "_SBL_URL";
    public static final String SRVR_MAP_SRVR_NAME_KEY_SUFFIX = "_SBL_SRVR_NAME";
    public static final String SRVR_MAP_CONFIG_FETCH_ERROR = "Could not fetch value from server mapping config! Will use default";

    public static final String QUERY_MAP_EMPTY_ERR = "Query Map is empty or does not have enough fields. Please ensure at least three fields";
    /* public static String BAC_SBL_ORG = "Bank Of America Organization"; */
    public static String BAC_SBL_SERVER_SITE = "CORPORATE";
    public static String DEFAULT_BAC_SBL_LOGIN = "LOGIN";
    public static String DEFAULT_SBL_DOMAIN = "Leaves";
    public static String DEFAULT_SBL_PROCESS = "Internal Reports";
    public static String DEFAULT_SBL_SR_TYPE = "Standard Request";
    public static String DEFAULT_SBL_SUBPROCESS = "Scheduled Reports";
    public static String DEFAULT_SBL_SR_SOURCE = "Email";
    public static String DEFAULT_SBL_SR_PRIORITY = "BAU";
    public static String DEFAULT_SBL_SR_SEVERITY = "4-Low";

}
