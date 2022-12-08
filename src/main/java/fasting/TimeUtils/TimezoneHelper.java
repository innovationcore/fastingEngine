package fasting.TimeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.text.*;

public class TimezoneHelper {
    final int SEC_IN_DAY = 86400;
    final int SEC_IN_4_HOURS = 14400;

    private String userTimezone;
    private String machineTimezone;
    private Integer timezoneDifference;
    private Logger logger;

    /**
    * initialize the timezone helper with the user's timezone and the machine's timezone (in seconds)
    */
    public TimezoneHelper(String userTimezone, String machineTimezone) {
        this.userTimezone = userTimezone;
        this.machineTimezone = machineTimezone;
        this.timezoneDifference = calculateTZOffset();
        this.logger = LoggerFactory.getLogger(TimezoneHelper.class.getName());
        logger.info("TimezoneHelper initialized with user timezone: " + userTimezone + " and machine timezone: " + machineTimezone + " and timezone difference: " + timezoneDifference);
    }

    /**
    * return the timezone difference in seconds
    * - if behind, + if ahead
    */
    public Integer calculateTZOffset() {
        String timeZone1 = this.userTimezone;
		String timeZone2 = this.machineTimezone;
		
		LocalDateTime dt = LocalDateTime.now();
		ZonedDateTime fromZonedDateTime = dt.atZone(ZoneId.of(timeZone1));
		ZonedDateTime toZonedDateTime = dt.atZone(ZoneId.of(timeZone2));
		long diff = Duration.between(fromZonedDateTime, toZonedDateTime).getSeconds();
        return (int) (diff);
    }

    /**
    * return the date string from adding seconds
    */
    public String getDateFromAddingSeconds(long seconds) {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        nowUserTimezone = nowUserTimezone.plusSeconds(seconds);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String formattedString = nowUserTimezone.format(formatter);
        return formattedString;
    }

    /**
    * return the seconds until Noon -1 min for user timezone
    */
    public int getSecondsTo1159am() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTimeNoon = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 11, 59, 30);
        long secondsUntilNoon = Duration.between(nowUserLocalTime, userLocalTimeNoon).getSeconds();
        return (int) secondsUntilNoon;
    }

    /**
    * return the seconds until 9pm -1 min for user timezone
    */
    public int getSecondsTo2059pm() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime9pm = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 20, 59, 30);
        long secondsUntil9pm = Duration.between(nowUserLocalTime, userLocalTime9pm).getSeconds();
        return (int) secondsUntil9pm;
    }

    /**
    * return the seconds until 4am -1 min (next day) for user timezone
    */
    public int getSecondsTo359am() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 23, 59, 30);
        long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();
        secondsUntil4am += SEC_IN_4_HOURS; 
        // doing it this way avoids months with 30 and 31 days, instead of adding a day to getDayofMonth() (which may fail), 
        //just add 4 hours of seconds onto a midnight time
        return (int) secondsUntil4am;
    }

    /**
    * return the seconds until 4am for user timezone (This should only be used in resetting the episode)
    */
    public int getSecondsTo4am() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        int hour = nowUserLocalTime.getHour();

        // if current hour is between midnight and 3:59:59, just get seconds to midnight
        if (hour >= 00 && hour < 4){
            LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 04, 00, 00);
            long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();
            return (int) secondsUntil4am;
        } else if (hour == 4){ // if the hour is 4am return 0, reset now
            return 0;
        } else { // if hour is 5am to 23:59:59, add a day and get seconds to 4am
            LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 04, 00, 00);
            userLocalTime4am = userLocalTime4am.plusDays(1);
            long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();
            return (int) secondsUntil4am;
        }
    }

    /**
    * returns a user's local time as String
    */
    public String getUserLocalTime() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone(this.userTimezone));
        return df.format(date);
    }

    /**
    * returns the machines local time as String
    */
    public ArrayList<Integer> getMachineHMS() {
        ArrayList<Integer> hms = new ArrayList<Integer>();
        Instant instant = Instant.now();
        ZoneId zoneId = ZoneId.of(this.machineTimezone);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);
        hms.add(zonedDateTime.getHour());
        hms.add(zonedDateTime.getMinute());
        hms.add(zonedDateTime.getSecond());
        return hms;
    }

    public Boolean isBetween3AMand3PM() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime3am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 03, 00, 00);
        LocalDateTime userLocalTime3pm = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 15, 00, 00);
        long secondsUntil3am = Duration.between(nowUserLocalTime, userLocalTime3am).getSeconds();
        long secondsUntil3pm = Duration.between(nowUserLocalTime, userLocalTime3pm).getSeconds();

        if (secondsUntil3am <= 0 && secondsUntil3pm >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean isBetween12AMand4AM() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime12am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 00, 00, 00);
        LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 4, 00, 00);
        long secondsUntil12am = Duration.between(nowUserLocalTime, userLocalTime12am).getSeconds();
        long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();

        if (secondsUntil12am <= 0 && secondsUntil4am >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean isBetween3AMand3PM(long unixTS) {
        Instant unixUTC = Instant.ofEpochMilli(unixTS*1000L);
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(unixUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime3am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 03, 00, 00);
        LocalDateTime userLocalTime3pm = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 15, 00, 00);
        long secondsUntil3am = Duration.between(nowUserLocalTime, userLocalTime3am).getSeconds();
        long secondsUntil3pm = Duration.between(nowUserLocalTime, userLocalTime3pm).getSeconds();

        if (secondsUntil3am <= 0 && secondsUntil3pm >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public String yesterdaysDate() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime yesterday = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 00, 00, 00);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        yesterday = yesterday.minusDays(1);
        return yesterday.format(formatter);
    }

    // is same day <4am
    public boolean isSameDay(long lastKnownTime){
        // check if lastKnownTime is on the same day as now and before the next day at 4am

        Instant lastKnownUTC = Instant.ofEpochMilli(lastKnownTime*1000L);
        ZoneId lastKnownTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime lastKnownTimezone = ZonedDateTime.ofInstant(lastKnownUTC, lastKnownTZ);
        LocalDateTime lastKnownLocalTime = lastKnownTimezone.toLocalDateTime();

        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();

        LocalDateTime currentTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 04, 05, 00);
        currentTime4am = currentTime4am.plusDays(1);
        long secondsUntil4am = Duration.between(lastKnownLocalTime, currentTime4am).getSeconds();

        if (secondsUntil4am > 86400){
            return false;
        } else {
            return true;
        }
    }

    public long parseTime(String time, boolean forYesterday){
        // parse time string into seconds
        // time string should be in format HH:MM:SS
        try {
            String[] timeArray = time.split(":");
            int hours = Integer.parseInt(timeArray[0]);
            int minutes = Integer.parseInt(timeArray[1].substring(0,2));
            String ampm = timeArray[1].substring(2,4).toLowerCase();
            if (ampm.equals("pm")){
                hours += 12;
            }

            Instant nowUTC = Instant.now();
            ZoneId userTZ = ZoneId.of(this.userTimezone);
            ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
            LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
            // if current time is after 12am and before 4 am, set the date to yesterday
            if (isBetween12AMand4AM()){
                forYesterday = true;
            } 
            
            LocalDateTime currentDateAndTime = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), hours, minutes, 00);
            if (forYesterday){
                currentDateAndTime = currentDateAndTime.minusDays(1);
            }
            return currentDateAndTime.toEpochSecond(userTZ.getRules().getOffset(currentDateAndTime));
        } catch (Exception e){
            // if fails to parse time, return the time now
            //e.printStackTrace();
            return getUnixTimestampNow();
        }
    }

    public long getUnixTimestampNow(){
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        return nowUserLocalTime.toEpochSecond(userTZ.getRules().getOffset(nowUserLocalTime));
    }

    // returns -1 if before 9 hours, 0 if between 9 and 11 hours, 1 if after 11 hours
    public int determineGoodFastTime(long start, long end) {
        int duration = (int)(end - start);
        if (duration < 32400) { // 9 hours
            return -1;
        } else if (duration > 39600) { // 11 hours
            return 1;
        } else {
            return 0;
        }
    }

    // start unix time, end unix time, time in seconds (9 hours = 32400)
    public String getHoursMinutesBefore(long start, long end, long cutoffTime) {
        long duration = end - start;
        long durationBeforeCutoff = cutoffTime - duration;
        if (durationBeforeCutoff < 0) {
            return "0h 0m";
        }
        long hours = durationBeforeCutoff / 3600;
        long minutes = (durationBeforeCutoff % 3600) / 60;
        return hours + "h " + String.format("%02dm", minutes);
    }

    public boolean isAfter8PM(long endTime){
        Instant endUTC = Instant.ofEpochMilli(endTime*1000L);
        ZoneId endTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime endUserTimezone = ZonedDateTime.ofInstant(endUTC, endTZ);
        LocalDateTime endUserLocalTime = endUserTimezone.toLocalDateTime();
        LocalDateTime endUserLocalTime8pm = LocalDateTime.of(endUserLocalTime.getYear(), endUserLocalTime.getMonth(), endUserLocalTime.getDayOfMonth(), 20, 00, 00);
        long secondsUntil8pm = Duration.between(endUserLocalTime, endUserLocalTime8pm).getSeconds();
        if (secondsUntil8pm < 0){
            return true;
        } else {
            return false;
        }
    }

    public boolean isTimeForYesterday(long unixTS){
        Instant unixUTC = Instant.ofEpochMilli(unixTS*1000L);
        ZoneId unixTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime userTimezoneDate = ZonedDateTime.ofInstant(unixUTC, unixTZ);
        LocalDateTime unixLocalTime = userTimezoneDate.toLocalDateTime();

        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();

        LocalDateTime yesterdayLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 04, 00, 00);
        yesterdayLocalTime4am = yesterdayLocalTime4am.minusDays(1);
        LocalDateTime todayLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 04, 00, 00);
        long secondsYesterday4am = Duration.between(unixLocalTime, yesterdayLocalTime4am).getSeconds(); 
        long secondsToday4am = Duration.between(unixLocalTime, todayLocalTime4am).getSeconds(); 

        if (secondsYesterday4am <= 0 && secondsToday4am >= 0){
            return true;
        } else {
            return false;
        }
    }

    public long parseSQLTimestamp(String sqlTimeString) {
        // 2022-11-07 21:06:07.343
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime datetime = LocalDateTime.parse(sqlTimeString, formatter);
        ZoneId sqlTZ = ZoneId.of("UTC");
        ZonedDateTime zonedDateTime = ZonedDateTime.of(datetime, sqlTZ);
        LocalDateTime nowSQLLocalTime = zonedDateTime.toLocalDateTime();

        long epochts = nowSQLLocalTime.toEpochSecond(sqlTZ.getRules().getOffset(nowSQLLocalTime));
        return epochts;
    }

    /**
    * gets the user's timezone
    */
    public String getUserTimezone() {
        return this.userTimezone;
    }

    /**
    * gets the machine's timezone
    */
    public String getMachineTimezone() {
        return this.machineTimezone;
    }

    /**
    * gets the timezoneDifference
    */
    public int getTimezoneDifference() {
        return this.timezoneDifference;
    }

    /**
    * sets the userTimezone(String)
    */
    public void setUserTimezone(String userTimezone) {
        this.userTimezone = userTimezone;
    }

    /**
    * sets the machineTimezone(String)
    */
    public void setMachineTimezone(String machineTimezone) {
        this.machineTimezone = machineTimezone;
    }

    /**
    * sets the timezoneDifference(int)
    */
    public void setTimezoneDifference(int timezoneDifference) {
        this.timezoneDifference = timezoneDifference;
    }
}