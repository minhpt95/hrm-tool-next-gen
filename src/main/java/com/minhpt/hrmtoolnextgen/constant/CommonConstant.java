package com.minhpt.hrmtoolnextgen.constant;

public class CommonConstant {
    public static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\\\\|;:\\'\\\",<.>/?";
    public static final String AWS_S3_ACCESS_KEY_ID = "aws.s3.accessKeyId";
    public static final String AWS_S3_SECRET_ACCESS_KEY = "aws.s3.secretAccessKey";
    public static final String AWS_S3_REGION = "aws.s3.region";
    public static final String AWS_S3_BUCKET = "aws.s3.bucket";
    public static final String AWS_S3_HRM_TOOL_FOLDER = "hrm-tool";
    public static final String AWS_S3_AVATAR_IMAGES_FOLDER = "avatar-images";

    /** Number of days ahead to check for upcoming birthdays. */
    public static final int UPCOMING_BIRTHDAY_DAYS = 4;

    /** Default length for randomly generated user passwords. */
    public static final int DEFAULT_PASSWORD_LENGTH = 12;

    /** Length for securely generated password-reset tokens. */
    public static final int RESET_TOKEN_LENGTH = 32;
}
