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
    private Integer timezoneDifference;
    private Boolean isUserAheadOfMachine;
    private Logger logger;

    /**
    * initialize the timezone helper with the user's timezone and the machine's timezone (in seconds)
    */
    public TimezoneHelper(String userTimezone, String machineTimezone) {
        this.userTimezone = userTimezone;
        this.machineTimezone = machineTimezone;
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