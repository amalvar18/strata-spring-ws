package com.newco.strataws.common;

public class SiebelConstants {

    /*BusObj and bus Comp names */
    public static final String SERVICE_REQUEST_BC_NAME = "Service Request";
    public static final String SERVICE_REQUEST_BO_NAME = "Service Request";
    public static final String INBOUND_BO_NAME = "eMail Response";
    public static final String ACTIVITY_BO_NAME = "Action";
    public static final String ACTIVITY_BC_NAME = "Action";
    public static final String ACTIVITY_ATTACHMENT_BC_NAME = "Action Attachment";
    public static final String SR_ATTACHMENT_BC_NAME = "Service Request Attachment";

    /*Common Siebel Field Names*/
    public static final String SBL_ROW_ID_FIELD = "Id";

    /* SR Fields */
    public static final String SR_NUM_FIELD_NAME = "SR Number";
    public static final String SR_SBL_ROW_ID_FIELD_NAME = "SR Id";
    public static final String ACCOUNT_FIELD = "Account";
    public static final String STATUS_FIELD = "Status";
    public static final String DOMAIN_FIELD = "Area";
    public static final String PROCESS_FIELD = "Sub-Area";
    public static final String SUBPROCESS_FIELD = "Exult Sub-Process";
    public static final String SROWNER_FIELD = "Owner";
    public static final String SRTYPE_FIELD = "SR Type";
    public static final String SR_SOURCE_FIELD = "Source";
    public static final String PRIORITY_FIELD = "Priority";
    public static final String SEVERITY_FIELD = "Severity";
    public static final String SR_SHORT_DESC_FIELD = "Abstract";
    public static final String SR_LONG_DESC_FIELD = "Description";
    public static final String CONTACT_LASTNAME_FIELD = "Contact Last Name";
    public static final String SR_CONTACT_ID_FIELD = "Contact Emp Id";
    public static final String SR_TARGET_CONTACT_FIELD = "Exult Target Contact";
    public static final String SR_DUE_DATE_FIELD = "Commit Time";
    public static final String SR_VOLUME_FIELD = "Exult Volume";

    /*Contact BusComp Field Names*/
    public static final String CONTACT_EMP_ID_FIELD = "Employee Number";
    public static final String CONTACT_PAY_1_FIELD = "Educational Background";
    public static final String CONTACT_PAY_2_FIELD = "Hobby";
    public static final String CONTACT_PAY_3_FIELD = "Spouse";

    /*Action BusComp field names */
    public static final String EMAIL_ATTACH_FLAG_FIELD = "Email Attachment Flag";
    public static final String ACT_STATUS_FIELD = "Status";
    public static final String ACT_TYPE_FIELD = "Type";
    public static final String ACT_DESC_FIELD = "Description";
    public static final String ACT_COMMENTS_FIELD = "Comment";
    public static final String ACT_DUE_DATE_FIELD = "Due Date";
    public static final String ACT_OWNER_FIELD = "Primary Owned By";

    /* Attachment Buscomp Field names */
    public static final String ACT_ATTACH_FILENAME_FIELD = "ActivityFileName";
    public static final String ACT_ATTACH_ACT_ID_FIELD = "Activity Id";
    public static final String ACT_ATTACH_FILESIZE_FIELD = "ActivityFileSize";
    public static final String ACT_ATTACH_FILEEXTN_FIELD = "ActivityFileExt";
    public static final String ACT_ATTACH_FILESRC_FIELD = "ActivityFileSrcType";
    public static final String ACT_ATTACH_FILE_DEFER_FLG_FIELD = "ActivityFileDeferFlg";
    public static final String ACT_ATTACH_FILE_DOCK_REQ_FLG_FIELD = "ActivityFileDockReqFlg";
    public static final String ACT_ATTACH_FILE_DOCK_STAT_FLG_FIELD = "ActivityFileDockStatFlg";
    public static final String ACT_ATTACH_FILE_AUTO_UPD_FLG_FIELD = "ActivityFileAutoUpdFlg";

    /*Method Names */
    public static final String CREATE_FILE_METHOD_NAME = "CreateFile";
    public static final String GET_FILE_METHOD_NAME = "GetFile";

    /*Status Values*/
    public static final String SBL_OPEN_SR_STATUS = "Open";
    public static final String SBL_CLOSED_SR_STATUS = "Closed";
    public static final String SBL_DONE_STATUS = "Done";

    /*Messages*/
    public static final String CREATE_FILE_SUCCESS_MSG = "Success";

    public static final String CONTACT_SORT_SPEC = "Educational Background(Ascending),Hobby(Ascending),Spouse(Ascending),Created(Descending)";
    public static final String CONTACT_SORT_SPEC_RECENT = "Updated(Descending)";
    public static final String PICKLIST_NAME_VAL = "Name";
    public static final String CONTACT_PICKLIST_LOGIN_NAME_VAL = "Login Name";

    /*Error Codes*/
    public static final int DUP_FILE_RECORD_ERR_CODE = 7668094;
    public static final int SBL8_EOF_ERR_CODE = 7668105;
    public static final int AUTH_FAILURE_SBL_ERR_CODE = 7668281;
    public static final int FIELD_NOT_FOUND_SBL_ERR_CODE = 7668110;

    public static final String SBL_DATE_TIME_PATTERN = "M/d/uuuu h:mm:ss a";
    public static final String SBL_DATE_TIME_PATTERN2 = "MM/dd/uuuu HH:mm:ss";
}
