package com.vatek.hrmtoolnextgen.constant;

public class ErrorConstant {
    public static class Code {
        public static final String SUCCESS = "00";
        public static final String LOGIN_INVALID = "01";
        public static final String USER_INACTIVE = "02";
        public static final String NOT_FOUND = "03";
        public static final String ALREADY_EXISTS = "04";
        public static final String PERMISSION_DENIED = "05";
        public static final String INTERNAL_SERVER_ERROR = "06";
        public static final String TOKEN_REFRESH_EXCEPTION = "07";
        public static final String MISSING_FIELD = "08";
        public static final String AUTHENTICATION_ERROR = "09";
        public static final String UNAUTHORIZED = "10";
        public static final String OVERLAPPING_DATE = "11";
        public static final String FROM_DATE_TO_DATE_VALIDATE = "12";
        public static final String CANNOT_LOG_TIMESHEET = "13";
        public static final String CANNOT_LOG_ON_WEEKEND = "14";
        public static final String CANNOT_LOG_ON_FULL_DAY_OFF = "15";
        public static final String NOT_NULL = "16";
        public static final String NOT_EMPTY = "17";
    }

    public static class Type {
        public static final String LOGIN_INVALID = "LOGIN_INVALID";
        public static final String USER_INACTIVE = "USER_INACTIVE";
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
        public static final String MISSING_FIELD = "MISSING_FIELD";
        public static final String TOKEN_REFRESH_EXCEPTION = "TOKEN_REFRESH_EXCEPTION";
        public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
        public static final String AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String OVERLAPPING_DATE = "OVERLAPPING_DATE";
        public static final String FROM_DATE_TO_DATE_VALIDATE = "FROM_DATE_TO_DATE_VALIDATE";
        public static final String CANNOT_LOG_TIMESHEET = "CANNOT_LOG_TIMESHEET";
        public static final String CANNOT_LOG_ON_WEEKEND = "CANNOT_LOG_ON_WEEKEND";
        public static final String CANNOT_LOG_ON_FULL_DAY_OFF = "CANNOT_LOG_ON_FULL_DAY_OFF";
        public static final String NOT_NULL = "NOT_NULL";
        public static final String NOT_BLANK = "NOT_BLANK";
    }

    public static class Message {
        public static final String LOGIN_INVALID = "Username or password invalid.";
        public static final String USER_INACTIVE = "User inactive.";
        public static final String CANNOT_UPDATE_ANOTHER_PROJECT = "Cannot update another project";
        public static final String SUCCESS = "SUCCESS.";
        public static final String ALREADY_EXISTS = "%s already exists.";
        public static final String CANNOT_UPDATE_TIMESHEET = "Cannot update timesheet";
        public static final String CANNOT_CHANGE_TIMESHEET_STATUS = "Cannot change timesheet status";
        public static final String NOT_FOUND = "%s not found.";
        public static final String NOT_BLANK = " %s not blank.";
        public static final String NOT_NULL = " %s not null.";
        public static final String END_OF_TIME = "Time activate expired";
        public static final String UNAUTHORIZED = "Unauthorized";
        public static final String OVERLAPPING_DATE = "Range date is overlapping";
        public static final String FROM_DATE_TO_DATE_VALIDATE = "From date must be less than to date";
        public static final String CANNOT_LOG_TIMESHEET = "Cannot log timesheet, total normal working hours must not be greater than %s";
        public static final String CANNOT_LOG_ON_WEEKEND = "Cannot Log On Weekend";
        public static final String CANNOT_LOG_ON_FULL_DAY_OFF = "Cannot Log On Full Day Off";
    }
}
