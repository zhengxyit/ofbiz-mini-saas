/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.base.conversion;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.TimeDuration;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;

import com.ibm.icu.util.Calendar;

/** Date/time Converter classes. */
public class DateTimeConverters implements ConverterLoader {
    public static class CalendarToDate extends AbstractConverter<Calendar, Date> {
        public CalendarToDate() {
            super(Calendar.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public Date convert(Calendar obj) throws ConversionException {
            return obj.getTime();
        }
    }

    public static class CalendarToLong extends AbstractConverter<Calendar, Long> {
        public CalendarToLong() {
            super(Calendar.class, Long.class);
        }

        public Long convert(Calendar obj) throws ConversionException {
            return obj.getTimeInMillis();
        }
    }

    public static class CalendarToString extends AbstractConverter<Calendar, String> {
        public CalendarToString() {
            super(Calendar.class, String.class);
        }

        public String convert(Calendar obj) throws ConversionException {
            Locale locale = obj.getLocale(com.ibm.icu.util.ULocale.VALID_LOCALE).toLocale();
            TimeZone timeZone = UtilDateTime.toTimeZone(obj.getTimeZone().getID());
            DateFormat df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            return df.format(obj);
        }
    }

    public static class CalendarToTimestamp extends AbstractConverter<Calendar, Timestamp> {
        public CalendarToTimestamp() {
            super(Calendar.class, Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public Timestamp convert(Calendar obj) throws ConversionException {
            return new Timestamp(obj.getTimeInMillis());
        }
    }

    public static class DateToCalendar extends GenericLocalizedConverter<Date, Calendar> {
        public DateToCalendar() {
            super(Date.class, Calendar.class);
        }

        public Calendar convert(Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return UtilDateTime.toCalendar(obj, timeZone, locale);
        }
    }

    public static class DateToLong extends AbstractConverter<Date, Long> {
        public DateToLong() {
            super(Date.class, Long.class);
        }

        public Long convert(Date obj) throws ConversionException {
             return obj.getTime();
        }
    }

    public static class DateToSqlDate extends AbstractConverter<Date, java.sql.Date> {
        public DateToSqlDate() {
            super(Date.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public java.sql.Date convert(Date obj) throws ConversionException {
            Calendar cal = Calendar.getInstance();
            cal.setTime(obj);
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return new java.sql.Date(cal.getTimeInMillis());
        }
    }

    public static class DateToSqlTime extends AbstractConverter<Date, java.sql.Time> {
        public DateToSqlTime() {
            super(Date.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public java.sql.Time convert(Date obj) throws ConversionException {
            return new java.sql.Time(obj.getTime());
        }
    }

    public static class DateToString extends GenericLocalizedConverter<Date, String> {
        public DateToString() {
            super(Date.class, String.class);
        }

        @Override
        public String convert(Date obj) throws ConversionException {
            return obj.toString();
        }

        public String convert(Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }
    }

    public static class DateToTimestamp extends AbstractConverter<Date, Timestamp> {
        public DateToTimestamp() {
            super(Date.class, Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public Timestamp convert(Date obj) throws ConversionException {
            return new Timestamp(obj.getTime());
        }
    }

    public static class DurationToBigDecimal extends AbstractConverter<TimeDuration, java.math.BigDecimal> {
        public DurationToBigDecimal() {
            super(TimeDuration.class, java.math.BigDecimal.class);
        }

        public java.math.BigDecimal convert(TimeDuration obj) throws ConversionException {
             return new java.math.BigDecimal(TimeDuration.toLong(obj));
        }
    }

    public static class DurationToDouble extends AbstractConverter<TimeDuration, Double> {
        public DurationToDouble() {
            super(TimeDuration.class, Double.class);
        }

        public Double convert(TimeDuration obj) throws ConversionException {
             return Double.valueOf(TimeDuration.toLong(obj));
        }
    }

    public static class DurationToFloat extends AbstractConverter<TimeDuration, Float> {
        public DurationToFloat() {
            super(TimeDuration.class, Float.class);
        }

        public Float convert(TimeDuration obj) throws ConversionException {
             return Float.valueOf(TimeDuration.toLong(obj));
        }
    }

    public static class DurationToList extends GenericSingletonToList<TimeDuration> {
        public DurationToList() {
            super(TimeDuration.class);
        }
    }

    public static class DurationToLong extends AbstractConverter<TimeDuration, Long> {
        public DurationToLong() {
            super(TimeDuration.class, Long.class);
        }

        public Long convert(TimeDuration obj) throws ConversionException {
             return TimeDuration.toLong(obj);
        }
    }

    public static class DurationToSet extends GenericSingletonToSet<TimeDuration> {
        public DurationToSet() {
            super(TimeDuration.class);
        }
    }

    public static class DurationToString extends AbstractConverter<TimeDuration, String> {
        public DurationToString() {
            super(TimeDuration.class, String.class);
        }

        public String convert(TimeDuration obj) throws ConversionException {
             return obj.toString();
        }
    }

    public static abstract class GenericLocalizedConverter<S, T> extends AbstractLocalizedConverter<S, T> {
        protected GenericLocalizedConverter(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

        public T convert(S obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public T convert(S obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return convert(obj, locale, timeZone, null);
        }
    }

    public static class LongToCalendar extends AbstractLocalizedConverter<Long, Calendar> {
        public LongToCalendar() {
            super(Long.class, Calendar.class);
        }

        public Calendar convert(Long obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault());
        }

        public Calendar convert(Long obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return UtilDateTime.toCalendar(new Date(obj.longValue()), timeZone, locale);
        }

        public Calendar convert(Long obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault());
        }
    }

    public static class NumberToDate extends AbstractConverter<Number, Date> {
        public NumberToDate() {
            super(Number.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public Date convert(Number obj) throws ConversionException {
             return new Date(obj.longValue());
        }
    }

    public static class NumberToDuration extends AbstractConverter<Number, TimeDuration> {
        public NumberToDuration() {
            super(Number.class, TimeDuration.class);
        }

        public TimeDuration convert(Number obj) throws ConversionException {
             return TimeDuration.fromNumber(obj);
        }
    }

    public static class NumberToSqlDate extends AbstractConverter<Number, java.sql.Date> {
        public NumberToSqlDate() {
            super(Number.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public java.sql.Date convert(Number obj) throws ConversionException {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(obj.longValue());
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return new java.sql.Date(cal.getTimeInMillis());
        }
    }

    public static class NumberToSqlTime extends AbstractConverter<Number, java.sql.Time> {
        public NumberToSqlTime() {
            super(Number.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public java.sql.Time convert(Number obj) throws ConversionException {
             return new java.sql.Time(obj.longValue());
        }
    }

    public static class NumberToTimestamp extends AbstractConverter<Number, Timestamp> {
        public NumberToTimestamp() {
            super(Number.class, Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public Timestamp convert(Number obj) throws ConversionException {
             return new Timestamp(obj.longValue());
        }
    }

    public static class SqlDateToDate extends AbstractConverter<java.sql.Date, Date> {
        public SqlDateToDate() {
            super(java.sql.Date.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public Date convert(java.sql.Date obj) throws ConversionException {
            return new Date(obj.getTime());
        }
    }

    public static class SqlDateToList extends GenericSingletonToList<java.sql.Date> {
        public SqlDateToList() {
            super(java.sql.Date.class);
        }
    }

    public static class SqlDateToSet extends GenericSingletonToSet<java.sql.Date> {
        public SqlDateToSet() {
            super(java.sql.Date.class);
        }
    }

    public static class SqlDateToString extends GenericLocalizedConverter<java.sql.Date, String> {
        public SqlDateToString() {
            super(java.sql.Date.class, String.class);
        }

        @Override
        public String convert(java.sql.Date obj) throws ConversionException {
            return obj.toString();
        }

        public String convert(java.sql.Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }
    }

    public static class SqlDateToTime extends AbstractConverter<java.sql.Date, java.sql.Time> {
        public SqlDateToTime() {
            super(java.sql.Date.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public java.sql.Time convert(java.sql.Date obj) throws ConversionException {
            throw new ConversionException("Conversion from Date to Time not supported");
       }
    }

    public static class SqlDateToTimestamp extends AbstractConverter<java.sql.Date, Timestamp> {
        public SqlDateToTimestamp() {
            super(java.sql.Date.class, Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public Timestamp convert(java.sql.Date obj) throws ConversionException {
            return new Timestamp(obj.getTime());
       }
    }

    public static class SqlTimeToList extends GenericSingletonToList<java.sql.Time> {
        public SqlTimeToList() {
            super(java.sql.Time.class);
        }
    }

    public static class SqlTimeToSet extends GenericSingletonToSet<java.sql.Time> {
        public SqlTimeToSet() {
            super(java.sql.Time.class);
        }
    }

    public static class SqlTimeToSqlDate extends AbstractConverter<java.sql.Time, java.sql.Date> {
        public SqlTimeToSqlDate() {
            super(java.sql.Time.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public java.sql.Date convert(java.sql.Time obj) throws ConversionException {
            throw new ConversionException("Conversion from Time to Date not supported");
        }
    }

    public static class SqlTimeToString extends GenericLocalizedConverter<java.sql.Time, String> {
        public SqlTimeToString() {
            super(java.sql.Time.class, String.class);
        }

        public String convert(java.sql.Time obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toTimeFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }
    }

    public static class StringToCalendar extends AbstractLocalizedConverter<String, Calendar> {
        public StringToCalendar() {
            super(String.class, Calendar.class);
        }

        public Calendar convert(String obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public Calendar convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public Calendar convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                Date date = df.parse(trimStr);
                return UtilDateTime.toCalendar(date, timeZone, locale);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToDate extends GenericLocalizedConverter<String, Date> {
        public StringToDate() {
            super(String.class, Date.class);
        }

        public Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateTimeFormat(obj.contains("-") ? UtilDateTime.DATE_TIME_FORMAT : null, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                return df.parse(trimStr);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToDuration extends AbstractLocalizedConverter<String, TimeDuration> {
        public StringToDuration() {
            super(String.class, TimeDuration.class);
        }

        public TimeDuration convert(String obj) throws ConversionException {
            return TimeDuration.parseDuration(obj);
        }

        public TimeDuration convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            if (!obj.contains(":")) {
                // Encoded duration
                try {
                    NumberFormat nf = NumberFormat.getNumberInstance(locale);
                    nf.setMaximumFractionDigits(0);
                    Number number = nf.parse(obj);
                    return TimeDuration.fromNumber(number);
                } catch (ParseException e) {
                    throw new ConversionException(e);
                }
            }
            return convert(obj);
        }

        public TimeDuration convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, locale, timeZone);
        }
    }

    public static class StringToSqlDate extends GenericLocalizedConverter<String, java.sql.Date> {
        public StringToSqlDate() {
            super(String.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public java.sql.Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateFormat(formatString, timeZone, locale);
            }
            try {
                Date parsedDate = df.parse(trimStr);
                Calendar cal = UtilDateTime.toCalendar(parsedDate, timeZone, locale);
                cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return new java.sql.Date(cal.getTimeInMillis());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToSqlTime extends GenericLocalizedConverter<String, java.sql.Time> {
        public StringToSqlTime() {
            super(String.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public java.sql.Time convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toTimeFormat(formatString, timeZone, locale);
            }
            try {
                return new java.sql.Time(df.parse(trimStr).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToTimestamp extends GenericLocalizedConverter<String, Timestamp> {
        public StringToTimestamp() {
            super(String.class, Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && targetClass == this.getTargetClass();
        }

        public Timestamp convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String str = obj.trim();
            if (str.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                // These hacks are a bad idea, but they are included
                // for backward compatibility.
                if (str.length() > 0 && !str.contains(":")) {
                    str = str + " 00:00:00.00";
                }
                // hack to mimic Timestamp.valueOf() method
                if (str.length() > 0 && !str.contains(".")) {
                    str = str + ".0";
                } else {
                    // DateFormat has a funny way of parsing milliseconds:
                    // 00:00:00.2 parses to 00:00:00.002
                    // so we'll add zeros to the end to get 00:00:00.200
                    String[] timeSplit = str.split("[.]");
                    if (timeSplit.length > 1 && timeSplit[1].length() < 3) {
                        str = str + "000".substring(timeSplit[1].length());
                    }
                }
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                return new Timestamp(df.parse(str).getTime());
            } catch (ParseException e) {
                // FIXME: This change needs to be reverted. The Timestamp format is
                // defined by the JDBC specification. Passing an invalid format is an
                // application error and the application needs to be fixed.
                //
                // before throwing an exception, try a generic format first
                df = DateFormat.getDateTimeInstance();
                if (timeZone != null) {
                    df.setTimeZone(timeZone);
                }
                try {
                    return new Timestamp(df.parse(str).getTime());
                } catch (ParseException e2) {
                    throw new ConversionException(e);
                }
            }
        }
    }

    public static class StringToTimeZone extends AbstractConverter<String, TimeZone> {
        public StringToTimeZone() {
            super(String.class, TimeZone.class);
        }

        public TimeZone convert(String obj) throws ConversionException {
            TimeZone tz = UtilDateTime.toTimeZone(obj);
            if (tz != null) {
                return tz;
            } else {
                throw new ConversionException("Could not convert " + obj + " to TimeZone: ");
            }
        }
    }

    public static class TimestampToDate extends AbstractConverter<Timestamp, Date> {
        public TimestampToDate() {
            super(Timestamp.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public Date convert(Timestamp obj) throws ConversionException {
            return new Timestamp(obj.getTime());
        }
    }

    public static class TimestampToList extends GenericSingletonToList<Timestamp> {
        public TimestampToList() {
            super(Timestamp.class);
        }
    }

    public static class TimestampToSet extends GenericSingletonToSet<Timestamp> {
        public TimestampToSet() {
            super(Timestamp.class);
        }
    }

    public static class TimestampToSqlDate extends AbstractConverter<Timestamp, java.sql.Date> {
        public TimestampToSqlDate() {
            super(Timestamp.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public java.sql.Date convert(Timestamp obj) throws ConversionException {
            return new java.sql.Date(obj.getTime());
        }
    }

    public static class TimestampToSqlTime extends AbstractConverter<Timestamp, java.sql.Time> {
        public TimestampToSqlTime() {
            super(Timestamp.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        public java.sql.Time convert(Timestamp obj) throws ConversionException {
            return new java.sql.Time(obj.getTime());
        }
    }

    public static class TimestampToString extends GenericLocalizedConverter<Timestamp, String> {
        public TimestampToString() {
            super(Timestamp.class, String.class);
        }

        @Override
        public String convert(Timestamp obj) throws ConversionException {
            return obj.toString();
        }

        public String convert(Timestamp obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }
    }

    public static class TimeZoneToString extends AbstractConverter<TimeZone, String> {
        public TimeZoneToString() {
            super(TimeZone.class, String.class);
        }

        public String convert(TimeZone obj) throws ConversionException {
            return obj.getID();
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(DateTimeConverters.class);
    }
}
