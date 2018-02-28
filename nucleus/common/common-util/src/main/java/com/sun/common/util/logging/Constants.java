/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.common.util.logging;

/**
 *
 * @author Susan Rai
 */
public final class Constants {

  
    public static final String GF_FILE_HANDLER = "com.sun.enterprise.server.logging.GFFileHandler.";
    public static final String PY_FILE_HANDLER = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.";

    public final static String GF_HANDLER_LOG_TO_FILE = GF_FILE_HANDLER + "logtoFile";

    public final static String PY_HANDLER_LOG_TO_FILE = PY_FILE_HANDLER + "logtoFile";
    public final static String PY_HANDLER_ROTATION_ON_DATE_CHANGE = PY_FILE_HANDLER + "rotationOnDateChange";
    public final static String PY_HANDLER_ROTATION_ON_TIME_LIMIT = PY_FILE_HANDLER + "rotationTimelimitInMinutes";
    public final static String PY_HANDLER_ROTATION_ON_FILE_SIZE = PY_FILE_HANDLER + "rotationLimitInBytes";
    public final static String PY_HANDLER_MAXIMUM_FILES = PY_FILE_HANDLER + "maxHistoryFiles";
    public final static String PY_HANDLER_LOG_FILE = PY_FILE_HANDLER + "file";
    public final static String PY_HANDLER_COMPRESS_ON_ROTATION = PY_FILE_HANDLER + "compressOnRotation";

    public final static String GF_HANDLER_LOG_TO_FILE_DEFAULT_VALUE = "true";
    
    public final static String PY_HANDLER_LOG_TO_FILE_DEFAULT_VALUE = "true";
    public final static String PY_HANDLER_ROTATION_ON_DATE_CHANGE_DEFAULT_VALUE = "false";
    public final static String PY_HANDLER_ROTATION_ON_TIME_LIMIT_DEFAULT_VALUE = "0";
    public final static String PY_HANDLER_ROTATION_ON_FILE_SIZE_DEFAULT_VALUE = "2000000";
    public final static String PY_HANDLER_MAXIMUM_FILES_DEFAULT_VALUE = "0";
    public final static String PY_HANDLER_LOG_FILE_DEFAULT_VALUE = "${com.sun.aas.instanceRoot}/logs/notification.log";
    public final static String PY_HANDLER_COMPRESS_ON_ROTATION_DEFAULT_VALUE = "false";

}
