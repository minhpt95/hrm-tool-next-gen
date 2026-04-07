package com.minhpt.hrmtoolnextgen.constant;

public class RoleConstant {
    public static final String ADMIN = "ADMIN";
    public static final String IT_ADMIN = "IT_ADMIN";
    public static final String PROJECT_MANAGER = "PROJECT_MANAGER";
    public static final String USER = "USER";
    public static final String HR = "HR";

    public static final String[] ADMIN_AUTHORITIES = {
            ADMIN,
            IT_ADMIN
    };

    public static final String[] USER_ACCESS_AUTHORITIES = {
            USER,
            PROJECT_MANAGER,
            HR,
            ADMIN,
            IT_ADMIN
    };
}
