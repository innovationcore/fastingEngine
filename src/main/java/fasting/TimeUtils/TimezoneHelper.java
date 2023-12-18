package fasting.TimeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class TimezoneHelper {
    private static final long SEC_IN_DAY = 86400;
    private final int SEC_IN_4_HOURS = 14400;
    private String userTimezone;
    private String machineTimezone;
    private Integer timezoneDifference;
    private final Logger logger;

    /**
    * initialize the timezone helper with the user's timezone and the machine's timezone (in seconds)
    */
    public TimezoneHelper(String userTimezone, String machineTimezone) {
        this.userTimezone = userTimezone;
        this.machineTimezone = machineTimezone;
        this.timezoneDifference = calculateTZOffset();
        this.logger = LoggerFactory.getLogger(TimezoneHelper.class);
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
        return nowUserTimezone.format(formatter);
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
    * return the seconds until 4am - 30 sec (next day) for user timezone
    */
    public int getSecondsTo359am() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        if(isBetween12AMand359AM()){
            LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 3, 59, 30);
            long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();
            return (int) secondsUntil4am;
        } else {
            LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 23, 59, 30);
            long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();
            secondsUntil4am += SEC_IN_4_HOURS;
            // doing it this way avoids months with 30 and 31 days, instead of adding a day to getDayofMonth() (which may fail),
            //just add 4 hours of seconds onto a midnight time
            return (int) secondsUntil4am;
        }
    }

    /**
    * !!! IMPORTANT !!! THIS SHOULD ONLY BE USED TO RESTORE PARTICIPANT STATE, WILL CREATE A LOOP OF TEXTS IF USED ELSEWHERE
     * return the seconds until 4am for user timezone
    */
    public int getSecondsTo4am() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        int hour = nowUserLocalTime.getHour();

        // if current hour is between midnight and 3:59:59, just get seconds to midnight
        if (hour < 4){
            LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 4, 0, 0);
            long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();
            return (int) secondsUntil4am;
        } else if (hour == 4){ // if the hour is 4am return 0, reset now
            return 0;
        } else { // if hour is 5am to 23:59:59, add a day and get seconds to 4am
            LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 4, 0, 0);
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
     * checks if current time is between 3am and 3 pm
     */
    public Boolean isBetween3AMand3PM() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime3am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 3, 0, 0);
        LocalDateTime userLocalTime3pm = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 15, 0, 0);
        long secondsUntil3am = Duration.between(nowUserLocalTime, userLocalTime3am).getSeconds();
        long secondsUntil3pm = Duration.between(nowUserLocalTime, userLocalTime3pm).getSeconds();

        return secondsUntil3am <= 0 && secondsUntil3pm >= 0;
    }

    /**
     * checks if between 3am and 3pm given a unix epoch time
     * @param unixTS
     * @return true/false
     */
    public Boolean isBetween3AMand3PM(long unixTS) {
        Instant unixUTC = Instant.ofEpochMilli(unixTS*1000L);
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(unixUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime3am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 3, 0, 0);
        LocalDateTime userLocalTime3pm = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 15, 0, 0);
        long secondsUntil3am = Duration.between(nowUserLocalTime, userLocalTime3am).getSeconds();
        long secondsUntil3pm = Duration.between(nowUserLocalTime, userLocalTime3pm).getSeconds();

        return secondsUntil3am <= 0 && secondsUntil3pm >= 0;
    }

    /**
     * checks to see if current time is between midnight and 4am
     * @return true/false
     */
    public Boolean isBetween12AMand4AM() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime12am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 0, 0, 0);
        LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 4, 0, 0);
        long secondsUntil12am = Duration.between(nowUserLocalTime, userLocalTime12am).getSeconds();
        long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();

        return secondsUntil12am <= 0 && secondsUntil4am >= 0;
    }

    /**
     * checks to see if current time is between midnight and 3:59:30am
     * @return
     */
    public Boolean isBetween12AMand359AM() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime12am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 0, 0, 0);
        LocalDateTime userLocalTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 3, 59, 30);
        long secondsUntil12am = Duration.between(nowUserLocalTime, userLocalTime12am).getSeconds();
        long secondsUntil4am = Duration.between(nowUserLocalTime, userLocalTime4am).getSeconds();

        return secondsUntil12am <= 0 && secondsUntil4am >= 0;
    }

    /**
     * checks to see if the current time is the same day as the provided lastKnownTime
     * @param lastKnownTime
     * @return
     */
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

        LocalDateTime currentTime4am = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 4, 5, 0);
        currentTime4am = currentTime4am.plusDays(1);
        long secondsUntil4am = Duration.between(lastKnownLocalTime, currentTime4am).getSeconds();

        return secondsUntil4am <= 86400;
    }

    /**
     * Parses the times sent in by participants
     * @param time
     * @return unix timestamp of the parsed time, -1L if some error occurs
     */
    public long parseTime(String time){
        // parse time string into seconds
        // time string should be in format HH:MM:SS
        boolean forYesterday = false;
        try {
            int hours = -1;
            int minutes = -1;
            String ampm = "";

            time = time.toLowerCase();

            if (!(time.contains("a") || time.contains("p"))) {
                return -1L;
            }

            if (time.contains(":")) {
                // 00:00
                String[] timeArray = time.split(":");
                hours = Integer.parseInt(timeArray[0].trim());
                minutes = Integer.parseInt(timeArray[1].trim().substring(0, 2));
            } else {
                // 5pm
                if (time.toLowerCase().contains("p")) {
                    String[] timeArray = time.split("p");
                    hours = Integer.parseInt(timeArray[0].trim());
                    minutes = 0;
                } else if (time.toLowerCase().contains("a")){
                    String[] timeArray = time.split("a");
                    hours = Integer.parseInt(timeArray[0].trim());
                    minutes = 0;
                } else {
                    try {
                        hours = Integer.parseInt(time.trim());
                        minutes = 0;
                    } catch (Exception e) {
                        logger.error("parseTime", e);
                        return -1L;
                    }
                }
            }

            if (time.toLowerCase().contains("a")){
                ampm = "am";
            } else if (time.toLowerCase().contains("p")){
                ampm = "pm";
            }

            // if the hour is 12 and its PM, don't do anything. Else add 12 hours
            if (ampm.equals("pm") && hours != 12){
                hours += 12;
            }

            // if the hour is 12 and its AM, set hours to 0
            if (ampm.equals("am") && hours == 12){
                hours = 0;
            }

            // if current time is after 12am and before 4 am, set the date to yesterday
            if (isBetween12AMand4AM()) {
                forYesterday = true;
            }

            Instant nowUTC = Instant.now();
            ZoneId userTZ = ZoneId.of(this.userTimezone);
            ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
            LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
            
            LocalDateTime currentDateAndTime = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), hours, minutes, 0);
            if (forYesterday){
                currentDateAndTime = currentDateAndTime.minusDays(1);
            }

            return currentDateAndTime.toEpochSecond(userTZ.getRules().getOffset(currentDateAndTime));
        } catch (Exception e){
            logger.error("parseTime", e);
            // if fails to parse time, return the time now
            return -1L;
        }
    }

    /**
     * parses the times received by manually moving the state machine on the website
     * @param time
     * @return unix timestamp of the parsed time, -1L if some error occurs
     */
    public long parseTimeWebsite(String time){
        // parse time string into seconds
        // time string should be in format HH:MM:SS
        boolean forYesterday = false;
        try {
            int hours = -1;
            int minutes = -1;

            time = time.toLowerCase();

            if (time.contains(":")) {
                // 00:00
                String[] timeArray = time.split(":");
                hours = Integer.parseInt(timeArray[0].trim());
                minutes = Integer.parseInt(timeArray[1].trim().substring(0, 2));
            } else {
                // 5pm
                if (time.toLowerCase().contains("p")) {
                    String[] timeArray = time.split("p");
                    hours = Integer.parseInt(timeArray[0].trim());
                    minutes = 0;
                } else if (time.toLowerCase().contains("a")){
                    String[] timeArray = time.split("a");
                    hours = Integer.parseInt(timeArray[0].trim());
                    minutes = 0;
                } else {
                    try {
                        hours = Integer.parseInt(time.trim());
                        minutes = 0;
                    } catch (Exception e) {
                        logger.error("parseTimeWebsite", e);
                        return -1L;
                    }
                }
            }

            // if current time is after 12am and before 4 am, set the date to yesterday
            if (isBetween12AMand4AM()){
                forYesterday = true;
            }

            Instant nowUTC = Instant.now();
            ZoneId userTZ = ZoneId.of(this.userTimezone);
            ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
            LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();

            LocalDateTime currentDateAndTime = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), hours, minutes, 0);
            if (forYesterday){
                currentDateAndTime = currentDateAndTime.minusDays(1);
            }

            return currentDateAndTime.toEpochSecond(userTZ.getRules().getOffset(currentDateAndTime));
        } catch (Exception e){
            logger.error("parseTimeWebsite", e);
            // if fails to parse time, return the time now
            return -1L;
        }
    }

    /**
     * Gets the current unix timestamp
     * @return unix timestamp
     */
    public long getUnixTimestampNow(){
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        return nowUserLocalTime.toEpochSecond(userTZ.getRules().getOffset(nowUserLocalTime));
    }

    // returns -1 if before 9 hours, 0 if between 9 and 11 hours, 1 if after 11 hours

    /**
     * Determines if participant ate for too short, too long, or just right
     * @param start
     * @param end
     * @return -1 if too short (<9 hours), 0 if good (9-11 hours), 1 if too long (>11 hours)
     */
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

    // start unix time, end unix time, time in seconds (10 hours = 36000)

    /**
     * Gets the amount of time to the end of a fast
     * @param start unix time
     * @param end unix time
     * @param cutoffTime time in seconds (10 hours = 36000 seconds)
     * @return number of hours and minutes before in format ##h ##m
     */
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

    /**
     * Checks if time is after 8pm
     * @param endTime unix time
     * @return true/false
     */
    public boolean isAfter8PM(long endTime){
        Instant endUTC = Instant.ofEpochMilli(endTime*1000L);
        ZoneId endTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime endUserTimezone = ZonedDateTime.ofInstant(endUTC, endTZ);
        LocalDateTime endUserLocalTime = endUserTimezone.toLocalDateTime();
        LocalDateTime endUserLocalTime8pm = LocalDateTime.of(endUserLocalTime.getYear(), endUserLocalTime.getMonth(), endUserLocalTime.getDayOfMonth(), 20, 0, 0);
        long secondsUntil8pm = Duration.between(endUserLocalTime, endUserLocalTime8pm).getSeconds();
        return secondsUntil8pm < 0;
    }

    /**
     * parses the SQL timestamp into a unix timestamp to be used with the app
     * @param sqlTimeString in format 'yyyy-MM-dd HH:mm:ss'
     * @return unix timestamp
     */
    public long parseSQLTimestamp(String sqlTimeString) {
        // 2022-11-07 21:06:07.343
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime datetime = LocalDateTime.parse(sqlTimeString, formatter);
        ZoneId sqlTZ = ZoneId.of("UTC");
        ZonedDateTime zonedDateTime = ZonedDateTime.of(datetime, sqlTZ);
        LocalDateTime nowSQLLocalTime = zonedDateTime.toLocalDateTime();

        return nowSQLLocalTime.toEpochSecond(sqlTZ.getRules().getOffset(nowSQLLocalTime));
    }

    /**
     * return the seconds until Friday at 5pm in the users timezone
     * @return number of seconds
     */
    public int getSecondsToFriday5pm() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();


        LocalDateTime userLocalTimeNoon = nowUserLocalTime.with(DayOfWeek.FRIDAY).withHour(17).withMinute(0).withSecond(0).withNano(0);
        if (nowUserLocalTime.isAfter(userLocalTimeNoon)) {
            userLocalTimeNoon = userLocalTimeNoon.plusWeeks(1);
        }
        long secondsUntilNoon = Duration.between(nowUserLocalTime, userLocalTimeNoon).getSeconds();
        return (int) secondsUntilNoon;
    }

    /**
     * Gets the time string in 10 hours
     * @param startTS the start time in epoch seconds
     * @return string time in 10 hours
     */
    public String getTimeIn10Hours(long startTS) {
        // Convert epoch seconds to milliseconds
        long startTimeMillis = startTS * 1000;

        // Calculate the time 10 hours later
        long endTimeMillis = startTimeMillis + (10 * 60 * 60 * 1000);

        // Create a TimeZone object based on the provided timezoneId
        TimeZone timezone = TimeZone.getTimeZone(this.userTimezone);

        // Create a Date object from the calculated time
        Date endTime = new Date(endTimeMillis);

        // Format the date to the desired format
        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
        dateFormat.setTimeZone(timezone);

        String formattedTime = dateFormat.format(endTime);

        return formattedTime;
    }

    /**
     * return the seconds until 5pm in user's timezone
     * @return seconds
     */
    public int getSecondsTo8pm() {
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);
        LocalDateTime nowUserLocalTime = nowUserTimezone.toLocalDateTime();
        LocalDateTime userLocalTime5pm = LocalDateTime.of(nowUserLocalTime.getYear(), nowUserLocalTime.getMonth(), nowUserLocalTime.getDayOfMonth(), 20, 0, 0);
        long secondsUntil8pm = Duration.between(nowUserLocalTime, userLocalTime5pm).getSeconds();
        if (secondsUntil8pm < 0) {
            secondsUntil8pm += SEC_IN_DAY;
        }
        return (int) secondsUntil8pm;
    }

    public ZonedDateTime getZonedDateTime8am() {
        // Get the current time in the user's timezone
        Instant nowUTC = Instant.now();
        ZoneId userTZ = ZoneId.of(this.userTimezone);
        ZonedDateTime nowUserTimezone = ZonedDateTime.ofInstant(nowUTC, userTZ);

        // Check if 8am has already passed for today
        ZonedDateTime userLocalTime8am = nowUserTimezone.with(LocalTime.of(8, 0));
        if (nowUserTimezone.isAfter(userLocalTime8am)) {
            // If 8am has already passed, get the time until tomorrow at 8am
            ZonedDateTime tomorrowUserTimezone = nowUserTimezone.plusDays(1).with(LocalTime.of(8, 0));
            Duration timeUntilTomorrow8am = Duration.between(nowUserTimezone, tomorrowUserTimezone);
            return nowUserTimezone.plus(timeUntilTomorrow8am).withZoneSameInstant(ZoneOffset.UTC);
        } else {
            // If 8am has not yet passed, return the time until 8am today
            Duration timeUntil8am = Duration.between(nowUserTimezone, userLocalTime8am);
            return nowUserTimezone.plus(timeUntil8am).withZoneSameInstant(ZoneOffset.UTC);
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