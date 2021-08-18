package com.sirionlabs.helper.servicelevel;

import org.databene.commons.ArrayUtil;
import org.databene.commons.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class TimeDifferenceFunction  {

    private final static Logger logger = LoggerFactory.getLogger(TimeDifferenceFunction.class);

    public String timeDifferenceFunctionCalculation(String startDate, String endDate, String startDateFormat, String endDateFormat, String timeZone,String weekTypeStr,String workingHours) {

        LocalDateTimeCalculation localDateTimeCalculation = new LocalDateTimeCalculation(startDate, endDate, startDateFormat, endDateFormat, timeZone);

        String differenceInSeconds = null;

        try {
            setStartAndEndDate(localDateTimeCalculation);
        } catch (Exception e) {
            logger.error("Exception while setting start date and end date");
        }

        ZonedDateTime zdt1 = localDateTimeCalculation.startDateOutput;
        ZonedDateTime zdt2 = localDateTimeCalculation.endDateOutput;

        LocalDateTime localDateTime1;
        LocalDateTime localDateTime2;

        localDateTime1 = zdt1.withZoneSameInstant(ZoneId.of(timeZone)).toLocalDateTime();
        localDateTime2 = zdt2.withZoneSameInstant(ZoneId.of(timeZone)).toLocalDateTime();
        List<LocalDay> days = null;
        boolean isNegative = localDateTimeCalculation.isNegative;

        try {
            LocalDaysBuilder localDaysBuilder = new LocalDaysBuilder();
            days = localDaysBuilder.build(localDateTime1, localDateTime2);
        } catch (Exception ex) {

            logger.error("Exception while getting days count");
        }

        WeekType weekTypeDef = null;
        try {

            WeekTypeBuilder weekTypeBuilder = new WeekTypeBuilder();
            weekTypeDef = weekTypeBuilder.build(weekTypeStr);
        } catch (Exception e) {
            logger.error("Exception while getting week Type");
        }
        BusinessHoursUtils businessHoursUtils = new BusinessHoursUtils();

        days = businessHoursUtils.filterWeekends(days, weekTypeDef);

        WorkingTime workingTime = WorkingTime.getDefault();
        try {
            WorkingTimeBuilder workingTimeBuilder = new WorkingTimeBuilder();
            workingTime = workingTimeBuilder.build(workingHours);

        } catch (Exception e) {
            logger.error("Exception while getting working Time");
        }
        try {
            differenceInSeconds = String.valueOf(businessHoursUtils.calculateTime(days, workingTime) * (isNegative ? -1 : 1));
        }catch (Exception e){
            logger.error("Exception while getting difference in seconds");
        }

        return differenceInSeconds;
    }

    private void setStartAndEndDate(LocalDateTimeCalculation localDateTimeCalculation) {
        LocalDateTime startDateOutput;
        LocalDateTime endDateOutput;


        startDateOutput = parseLocalDateTime(localDateTimeCalculation, true);
        endDateOutput = parseLocalDateTime(localDateTimeCalculation, false);

        //if startDate is after endDate, swap the dates and set isNegative true
        if (startDateOutput.isAfter(endDateOutput)) {
            localDateTimeCalculation.isNegative = true;
            LocalDateTime temp = startDateOutput;
            startDateOutput = LocalDateTime.of(endDateOutput.toLocalDate(), endDateOutput.toLocalTime());
            endDateOutput = LocalDateTime.of(temp.toLocalDate(), temp.toLocalTime());
        }

        ZoneId zone = ZoneId.of(localDateTimeCalculation.timeZone);
        localDateTimeCalculation.startDateOutput = ZonedDateTime.of(startDateOutput, zone);
        localDateTimeCalculation.endDateOutput = ZonedDateTime.of(endDateOutput, zone);
    }

    private LocalDateTime parseLocalDateTime(LocalDateTimeCalculation localDateTimeCalculation, boolean isStart) {
        String dateFormat, date;
        if(isStart) {
            date = localDateTimeCalculation.startDate;
            dateFormat = localDateTimeCalculation.startDateFormat;
        } else {
            date = localDateTimeCalculation.endDate;
            dateFormat = localDateTimeCalculation.endDateFormat;
        }
        DateTimeFormatter dateDTF = DateTimeFormatter.ofPattern(dateFormat);
        LocalDateTime localDateTime;
        try{
            localDateTime = LocalDateTime.parse(date, dateDTF);
        }catch(Exception ex){
            try{
                LocalDate ld = LocalDate.parse(date, dateDTF);
                if(isStart) {
                    localDateTime = LocalDateTime.of(ld, LocalTime.MIN);
                }else {
                    localDateTime = LocalDateTime.of(ld, LocalTime.MAX);
                }
            }catch(Exception e){
                throw e;
            }
        }

        return localDateTime;
    }

    private class LocalDateTimeCalculation {
        String startDate;
        String endDate;
        String startDateFormat;
        String endDateFormat;
        String timeZone;
        ZonedDateTime startDateOutput;
        ZonedDateTime endDateOutput;
        boolean isNegative;

        LocalDateTimeCalculation(String startDate, String endDate, String startDateFormat, String endDateFormat, String timeZone) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.startDateFormat = startDateFormat;
            this.endDateFormat = endDateFormat;
            this.timeZone = timeZone;
        }
    }

    class LocalDay {

        LocalDate date;

        LocalTime startTime;

        LocalTime endTime;

        LocalDay(LocalDate date, LocalTime startTime, LocalTime endTime) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        LocalDate getDate() {
            return date;
        }

        LocalTime getStartTime() {
            return startTime;
        }

        LocalTime getEndTime() {
            return endTime;
        }

    }

    static class WorkingTime {

        LocalTime start;

        LocalTime end;

        WorkingTime(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        LocalTime getStart() {
            return start;
        }

        LocalTime getEnd() {
            return end;
        }

       static WorkingTime getDefault(){
            LocalTime start = LocalTime.MIN;
            LocalTime end = LocalTime.MAX;
            return new WorkingTime(start, end);
        }
    }

    class WeekType {

        int startDayNum;

        String startDayName;

        int endDayNum;

        String endDayName;

        WeekType(int startDayNum, String startDayName, int endDayNum, String endDayName) {
            this.startDayNum = startDayNum;
            this.startDayName = startDayName;
            this.endDayNum = endDayNum;
            this.endDayName = endDayName;
        }

        int getStartDayNum() {
            return startDayNum;
        }

        int getEndDayNum() {
            return endDayNum;
        }
    }

    class LocalDaysBuilder {

        List<LocalDay> build(final LocalDateTime dt1, final LocalDateTime dt2) {
            if (dt1 == null || dt2 == null)
                throw new IllegalArgumentException("Start or end time cannot be empty");

            if (dt1.isAfter(dt2))
                throw new IllegalArgumentException("Start time cannot be greater than the end time");

            List<LocalDay> days = new ArrayList<>();
            LocalDateTime startDate = dt1;
            boolean startAdded = false;
            while (startDate.compareTo(dt2) <= 0) {
                LocalDate date = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth());
                LocalTime startTime = LocalTime.MIN;

                if (!startAdded) {
                    startTime = LocalTime.of(startDate.getHour(), startDate.getMinute(), startDate.getSecond());
                    startAdded = true;
                }

                LocalTime endTime = LocalTime.MAX;
                LocalDay day = new LocalDay(date, startTime, endTime);
                days.add(day);
                startDate = startDate.plusDays(1).toLocalDate().atStartOfDay();
            }

            int size = days.size();
            LocalDay lastDay = days.get(size - 1);
            days.remove(size - 1);
            LocalTime endTimeForLastDay = LocalTime.of(dt2.getHour(), dt2.getMinute(), dt2.getSecond());
            LocalDay modifiedEndDay = new LocalDay(lastDay.getDate(), lastDay.getStartTime(), endTimeForLastDay);
            days.add(modifiedEndDay);
            return days;
        }
    }

    class BusinessHoursUtils {

        List<LocalDay> filterWeekends(List<LocalDay> days, WeekType weekType) {
            List<LocalDay> daysExcludingWeekend = new ArrayList<>();
            for (LocalDay day : days) {
                LocalDate date = day.getDate();
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                if (dayOfWeek.getValue() < weekType.getStartDayNum() || dayOfWeek.getValue() > weekType.getEndDayNum())
                    continue;
                daysExcludingWeekend.add(day);
            }

            return daysExcludingWeekend;
        }

        long calculateTime(List<LocalDay> days, WorkingTime workingTime) {

            long effectiveHours = 0;
            for (LocalDay day : days) {

                LocalTime effectiveStart = day.getStartTime().isAfter(workingTime.getStart()) ? day.getStartTime() : workingTime.getStart();
                LocalTime effectiveEnd = day.getEndTime().isBefore(workingTime.getEnd()) ? day.getEndTime() : workingTime.getEnd();

                if (effectiveEnd.isBefore(effectiveStart)) {
                    continue;
                }

                long seconds = effectiveStart.until(effectiveEnd, ChronoUnit.SECONDS);
                if (effectiveEnd.compareTo(LocalTime.MAX) == 0)
                    seconds += 1;
                effectiveHours = effectiveHours + seconds;
            }

            return effectiveHours;
        }
    }

    class WeekTypeBuilder {

        Map<String, Integer> dayInitialsToDayNum;

        {
            dayInitialsToDayNum = new HashMap<>();
            dayInitialsToDayNum.put("MON", 1);
            dayInitialsToDayNum.put("TUE", 2);
            dayInitialsToDayNum.put("WED", 3);
            dayInitialsToDayNum.put("THU", 4);
            dayInitialsToDayNum.put("FRI", 5);
            dayInitialsToDayNum.put("SAT", 6);
            dayInitialsToDayNum.put("SUN", 7);
        }

        WeekType build(String weekTypeStr) {

            if (StringUtil.isEmpty(weekTypeStr))
                throw new IllegalArgumentException("WeekType string cannot be empty");

            String[] days = weekTypeStr.split("-");

            if (ArrayUtil.isEmpty(days) || days.length != 2)
                System.out.println("exit");

            String weekStart = days[0].toUpperCase();
            String weekEnd = days[1].toUpperCase();

            if (!dayInitialsToDayNum.keySet().contains(weekStart) || !dayInitialsToDayNum.keySet().contains(weekEnd)) {
                System.out.println("exit");
            }

            int startDayNum = dayInitialsToDayNum.get(weekStart);
            int endDayNum = dayInitialsToDayNum.get(weekEnd);

            if (startDayNum > endDayNum)
                System.out.println("exit");
            WeekType weekType = new WeekType(startDayNum, weekStart, endDayNum, weekEnd);
            return weekType;
        }
    }

    class WorkingTimeBuilder {

        String TIME_FORMAT = "HH:mm";

        WorkingTime build(String workingHoursStr) throws Exception{

            if (StringUtil.isEmpty(workingHoursStr))
                throw new IllegalArgumentException("Input string cannot be empty");

            String[] hours = workingHoursStr.split("-");

            if (ArrayUtil.isEmpty(hours) || hours.length != 2) {
                throw new Exception("Invalid working hours");

            }

            String startTime = hours[0];
            String endTime = hours[1];

            String[] startTimeFields = startTime.split(":");
            String[] endTimeFields = endTime.split(":");

            if (ArrayUtil.isEmpty(startTimeFields) || startTimeFields.length != 2 || ArrayUtil.isEmpty(endTimeFields) || endTimeFields.length != 2) {
                throw new Exception("Invalid working hours");
            }

            WorkingTime workingTime;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
            try {
                LocalTime localStartTime = LocalTime.parse(startTime, formatter);
                LocalTime localEndTime = LocalTime.parse(endTime, formatter);

                if (localEndTime.compareTo(localStartTime) <= 0)
                    throw new Exception("End time cannot be before start time");

                workingTime = new WorkingTime(localStartTime, localEndTime);
            } catch (Exception ex) {
                throw new Exception("Invalid Working Hours", ex);
            }

            return workingTime;
        }
    }

}