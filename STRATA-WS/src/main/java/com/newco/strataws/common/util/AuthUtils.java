package com.newco.strataws.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;

public class AuthUtils {

    public static boolean verifyHeaders(String sblEnv, String sblLoginId, String sblPasswrd) {
        if (StringUtils.isBlank(sblLoginId) || StringUtils.isBlank(sblPasswrd)
                || StringUtils.isBlank(sblEnv)) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean verifySftpHeaders(String sftpUser, String sftpPass) {
        if (StringUtils.isBlank(sftpUser) || StringUtils.isBlank(sftpPass)) {
            return false;
        } else {
            return true;
        }
    }

    public static String decryptPassword(String secretFileName, String sealedFileName) throws Exception {
        SecretKey key = (SecretKey) AuthUtils.readFromFile(secretFileName);
        SealedObject sealedObject = (SealedObject) AuthUtils.readFromFile(sealedFileName);
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
        /*String lFilepath = "";*/
        try {
            // read file path from jvp properties

            /*
             * lFilepath = System
             * .getProperty(StarConstants.BASE_CONFIG_FILE_PATH_ENVID) +
             * "/passwords/";
             */

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

}
