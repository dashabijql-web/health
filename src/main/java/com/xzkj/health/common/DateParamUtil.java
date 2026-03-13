package com.xzkj.health.common;

import java.time.LocalDate;

/**
 * Controller 请求参数日期默认值工具类。
 */
public final class DateParamUtil {

    private DateParamUtil() {}

    /** 默认30天范围：startDate 默认 29 天前，endDate 默认今天 */
    public static String[] range30(String startDate, String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return new String[]{startDate, endDate};
    }

    /** 单日期默认今天 */
    public static String today(String date) {
        return date == null ? LocalDate.now().toString() : date;
    }
}
