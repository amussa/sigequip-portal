package org.openlmis.core.utils;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class DateUtil {
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_YEAR_MONTH = "yyyy-MM";
    public static final String EMPTY_STRING = "";
    public static final String FORMAT_DATE_TIME_DAY_MONTH_YEAR = "dd/MM/yyyy";
    public static final String FORMAT_DATE_TIME_CUBE = "yyyy,MM,dd";
    public static final String FORMAT_DATE_DD_MM_YYYY = "dd-MM-yyyy";

    public static String getFormattedDate(Date date, String format) {
        if (date == null)
            return null;

        try {
            return DateFormatUtils.format(date, format);
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return EMPTY_STRING;
        }
        return new SimpleDateFormat(DateUtil.FORMAT_DATE_TIME).format(date);
    }

    public static Date parseDate(String date) {
        return parseDate(date, FORMAT_DATE_TIME);
    }

    public static Date parseDate(String date, String pattern) {
        if (date == null) {
            return null;
        }

        try {
            return new SimpleDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatDateWithStartDayOfPeriodMonthOffset(String date, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(parseDate(date, FORMAT_DATE_TIME_CUBE));
        if (cal.get(Calendar.DAY_OF_MONTH) < 21) {
            cal.add(Calendar.MONTH, n);
            cal.set(Calendar.DAY_OF_MONTH, 21);
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 21);
        }
        return getFormattedDate(cal.getTime(), FORMAT_DATE_TIME_CUBE);
    }

    public static String formatDateWithStartDayOfPeriod(String date) {
        return formatDateWithStartDayOfPeriodMonthOffset(date, -1);
    }

    public static String formatDateWithEndDayOfPeriodMonthOffset(String date, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(parseDate(date, FORMAT_DATE_TIME_CUBE));
        if (cal.get(Calendar.DAY_OF_MONTH) > 20) {
            cal.add(Calendar.MONTH, n);
            cal.set(Calendar.DAY_OF_MONTH, 20);
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 20);
        }
        return getFormattedDate(cal.getTime(), FORMAT_DATE_TIME_CUBE);
    }

    public static String formatDateWithEndDayOfPeriod(String date) {
        return formatDateWithEndDayOfPeriodMonthOffset(date, 1);
    }

    public static String transform(String src, String srcPattern, String dstPattern) {
        Date dateSrc = parseDate(src, srcPattern);
        return getFormattedDate(dateSrc, dstPattern);
    }

    public static Date getNMonthsDate(String date, String pattern, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parseDate(date, pattern));
        calendar.add(Calendar.MONTH, n);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    public static String getCubeFormatNMonthsDate(String date, String pattern, int n) {
        Date d = getNMonthsDate(date, pattern, n);
        return getFormattedDate(d, FORMAT_DATE_TIME_CUBE);
    }

}
