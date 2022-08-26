package fasting.TimeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.util.*;
import java.text.*;

public class TimezoneHelper {
    private String userTimezone;
    private String machineTimezone;
    private int timezoneDifference;
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
    * + if ahead, - if behind
    */
    public int calculateTZOffset() {
        String timeZone1 = this.userTimezone;
		String timeZone2 = this.machineTimezone;
		
		LocalDateTime dt = LocalDateTime.now();
		ZonedDateTime fromZonedDateTime = dt.atZone(ZoneId.of(timeZone1));
		ZonedDateTime toZonedDateTime = dt.atZone(ZoneId.of(timeZone2));
		long diff = Duration.between(fromZonedDateTime, toZonedDateTime).toMillis();
        diff /= 1000; // converting to seconds
        return (int) diff;
    }

    /**
    * return the seconds until Noon -1 min for user timezone
    */
    public int getSecondsTo1159am() {
        LocalDateTime dt = LocalDateTime.now();
        LocalDateTime dtNoon = dt.withHour(11).withMinute(59).withSecond(30);
        ZonedDateTime fromZonedDateTime = dt.atZone(ZoneId.of(this.machineTimezone));
        ZonedDateTime toZonedDateTime = dtNoon.atZone(ZoneId.of(this.userTimezone));
        long diff = Duration.between(toZonedDateTime, fromZonedDateTime).toSeconds();
        return (int) diff;
    }

    /**
    * return the seconds until 9pm -1 min for user timezone
    */
    public int getSecondsTo2059pm() {
        LocalDateTime dt = LocalDateTime.now();
        LocalDateTime dtNoon = dt.withHour(20).withMinute(59).withSecond(30);
        ZonedDateTime fromZonedDateTime = dt.atZone(ZoneId.of(this.machineTimezone));
        ZonedDateTime toZonedDateTime = dtNoon.atZone(ZoneId.of(this.userTimezone));
        long diff = Duration.between(toZonedDateTime, fromZonedDateTime).toSeconds();
        return (int) diff;
    }

    /**
    * return the seconds until midnight -1 min for user timezone
    */
    public int getSecondsTo2359pm() {
        LocalDateTime dt = LocalDateTime.now();
        LocalDateTime dtNoon = dt.withHour(23).withMinute(59).withSecond(30);
        ZonedDateTime fromZonedDateTime = dt.atZone(ZoneId.of(this.machineTimezone));
        ZonedDateTime toZonedDateTime = dtNoon.atZone(ZoneId.of(this.userTimezone));
        long diff = Duration.between(toZonedDateTime, fromZonedDateTime).toSeconds();
        return (int) diff;
    }

    /**
    * returns a user's local time as String
    */
    public String getUserLocalTime() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        df.setTimeZone(TimeZone.getTimeZone(this.userTimezone));

        return df.format(date);
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