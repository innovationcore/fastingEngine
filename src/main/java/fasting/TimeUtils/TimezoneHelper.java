package fasting.TimeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.text.*;

public class TimezoneHelper {
    final int SEC_IN_DAY = 86400;

    private String userTimezone;
    private String machineTimezone;
    private int timezoneDifference;
    private Boolean isUserAheadOfMachine;
    private Logger logger;

    /**
    * initialize the timezone helper with the user's timezone and the machine's timezone (in seconds)
    */
    public TimezoneHelper(String userTimezone, String machineTimezone) {
        this.userTimezone = userTimezone;
        this.machineTimezone = machineTimezone;
        this.timezoneDifference = calculateTZOffset();
        System.out.println("Timezone difference: " + this.timezoneDifference);
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
    public int calculateTZOffset() {
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
        LocalDateTime dt = LocalDateTime.now();
        LocalDateTime dtNoon = dt.withHour(11).withMinute(59).withSecond(30);
        ZonedDateTime fromZonedDateTime;
        ZonedDateTime toZonedDateTime;
        long diff;
        if (this.isUserAheadOfMachine) {
            fromZonedDateTime = dt.atZone(ZoneId.of(this.userTimezone));
            toZonedDateTime = dtNoon.atZone(ZoneId.of(this.machineTimezone));
            diff = Duration.between(toZonedDateTime, fromZonedDateTime).getSeconds();
        } else {
            fromZonedDateTime = dt.atZone(ZoneId.of(this.machineTimezone));
            toZonedDateTime = dtNoon.atZone(ZoneId.of(this.userTimezone));
            diff = Duration.between(toZonedDateTime, fromZonedDateTime).getSeconds();
        }
        return (int) (diff * -1);
    }

    /**
    * return the seconds until 9pm -1 min for user timezone
    */
    public int getSecondsTo2059pm() {
        LocalDateTime dt = LocalDateTime.now();
        LocalDateTime dt9pm = dt.withHour(20).withMinute(59).withSecond(30);
        ZonedDateTime fromZonedDateTime;
        ZonedDateTime toZonedDateTime;
        long diff;
        if (this.isUserAheadOfMachine) {
            fromZonedDateTime = dt.atZone(ZoneId.of(this.userTimezone));
            toZonedDateTime = dt9pm.atZone(ZoneId.of(this.machineTimezone));
            diff = Duration.between(toZonedDateTime, fromZonedDateTime).getSeconds();
        } else {
            fromZonedDateTime = dt.atZone(ZoneId.of(this.machineTimezone));
            toZonedDateTime = dt9pm.atZone(ZoneId.of(this.userTimezone));
            diff = Duration.between(toZonedDateTime, fromZonedDateTime).getSeconds();
        }
        return (int) (diff * -1);
    }

    /**
    * return the seconds until 4am -1 min (next day) for user timezone
    */
    public int getSecondsTo359am() {
        LocalDateTime dt = LocalDateTime.now();
        LocalDateTime dt4am = dt.withHour(03).withMinute(59).withSecond(30);
        ZonedDateTime fromZonedDateTime;
        ZonedDateTime toZonedDateTime;
        long diff;
        if (this.isUserAheadOfMachine) {
            fromZonedDateTime = dt.atZone(ZoneId.of(this.userTimezone));
            toZonedDateTime = dt4am.atZone(ZoneId.of(this.machineTimezone));
            diff = Duration.between(toZonedDateTime, fromZonedDateTime).getSeconds();
        } else {
            fromZonedDateTime = dt.atZone(ZoneId.of(this.machineTimezone));
            toZonedDateTime = dt4am.atZone(ZoneId.of(this.userTimezone));
            diff = Duration.between(toZonedDateTime, fromZonedDateTime).getSeconds();
        }
        // if machine time is after midnight and before 3:59:30, don't add a day
        ArrayList<Integer> machineHMS = getMachineHMS();
        Integer hour = machineHMS.get(0);
        Integer minute = machineHMS.get(1);
        Integer second = machineHMS.get(2);
        if ((hour > 0 && minute < 0 && second < 0) && 
            (hour < 3 && minute < 59 && second < 30)) {
            return (-1 * ((int) diff));
        } else {
            return (-1 * ((int) diff - SEC_IN_DAY));
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