package com.vatek.hrmtoolnextgen.util;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import java.util.Currency;
import java.util.Locale;


public class CommonUtils {


    public static Pageable buildPageable(int pageIndex, int pageSize) {
        return PageRequest.of(pageIndex, pageSize);
    }

    public static Pageable buildPageable(int pageIndex, int pageSize,Direction direction,String... properties) {
        return PageRequest.of(pageIndex, pageSize,direction,properties);
    }

    public static String randomPassword(int length){
        return RandomStringUtils.secure().nextAlphanumeric(length);
    }

    public static String convertMoneyToText(String input,Locale locale) {
        String output;

        if(locale == null){
            locale = Locale.getDefault();
        }

        Currency currency = Currency.getInstance(locale);

        try {
            NumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT);
            output = ruleBasedNumberFormat.format(Long.parseLong(input)) + " " + currency;
        } catch (Exception e) {
            output = 0 + " " + currency;
        }
        return output.toUpperCase();
    }
}
