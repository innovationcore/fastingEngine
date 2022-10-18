package fasting.TimeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import java.text.*;

public class TimezoneHelper {
    final int SEC_IN_DAY = 86400;
    final int SEC_IN_4_HOURS = 14400;

    private String userTimezone;
    private String machineTimezone;
    private Integer startYear;
    private Integer startMonth;
    private Integer startDay;
    private Integer timezoneDifference;
    private Boolean isUserAheadOfMachine;
    private Logger logger;

    /**
    * initialize the timezone helper with the user's timezone and the machine's timezone (in seconds)
    */
    public TimezoneHelper(String userTimezone, String machineTimezone) {
        this.userTimezone = userTimezone;
        this.machineTimezone = machineTimezone;

        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        this.startYear = nowUserLocalTime.getYear();
        this.startMonth = nowUserLocalTime.getMonthValue();
        this.startDay = nowUserLocalTime.getDayOfMonth();

        this.timezoneDifference = calculateTZOffset();
        if (this.timezoneDifference > 0) {
            this.isUserAheadOfMachine = true;
        } else {
            this.isUserAheadOfMachine = false;
        }
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
        return ((int) secondsUntil4am);
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
            return ((int) secondsUntil4am);
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

        if (secondsUntil3am < 0 && secondsUntil3pm > 0) {
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

        // TODO: Instant isn't getting the correct date from epoch
        Instant lastKnownUTC = Instant.ofEpochMilli(lastKnownTime);
        ZoneId lastKnownTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime lastKnownTimezone = ZonedDateTime.ofInstant(lastKnownUTC, lastKnownTZ);
        LocalDateTime lastKnownLocalTime = lastKnownTimezone.toLocalDateTime();

        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();

        LocalDateTime currentTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 04, 00, 00);
        long secondsUntil4am = Duration.between(currentTime4am, lastKnownLocalTime).getSeconds();
        System.out.println('\n');
        System.out.println("lastKnownLocalTime: " + lastKnownLocalTime);
        System.out.println("currentTime4am: " + currentTime4am); 
        System.out.println("secondsUntil4am: " + secondsUntil4am);
        System.out.println('\n');

        if (secondsUntil4am > 86400){
            return false;
        } else {
            return true;
        }
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