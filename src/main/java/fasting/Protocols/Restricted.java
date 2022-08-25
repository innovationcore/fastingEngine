package fasting.Protocols;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.twilio.rest.api.v2010.account.Message;
import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class Restricted extends RestrictedBase {
    private Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    //id, participant_uuid, phone_number, participant_type
    private Map<String, String> participantMap;
    private Map<String,Long> stateMap;

    private long startTimestamp = 0;

    public String stateJSON;

    private Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(Restricted.class.getName());

    public Restricted(String participant_uuid) {

    }

    public Restricted(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();

        //hack for time_zone
        //this.participantMap.put("time_zone","US/Eastern");

        new Thread(){
            public void run(){
                try {
                    while (!getState().toString().equals("endOfEpisode")) {

                        if(startTimestamp > 0) {
                            stateJSON = saveStateJSON();
                            //logger.info(stateJSON);
                        }

                        Thread.sleep(1000);
                    }
                } catch (Exception ex) {
                    logger.error("protocols.Restricted Thread: " + ex.toString());
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    logger.error(pw.toString());
                }
            }
        }.start();

    }

    public void incomingText(Map<String,String> incomingMap) {

        try {
            State state = getState();
            switch (state) {
                case initial:
                    //no timers
                    break;
                case waitStart:
                    if(isStartCal(incomingMap.get("Body"))) {
                        receivedStartCal();
                    }
                    break;
                case warnStartCal:
                    if(isStartCal(incomingMap.get("Body"))) {
                        receivedStartCal();
                    }
                    break;
                case startcal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        receivedEndCal();
                    }
                    break;
                case missedStartCal:
                    String missedStartCalMessage = participantMap.get("participant_uuid") + " missedStartCal unexpected message";
                    logger.warn("\t\t " + missedStartCalMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), missedStartCalMessage);
                    break;
                case warnEndCal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        receivedEndCal();
                    }
                    break;
                case endcal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        receivedEndCal();
                    }
                    break;
                case missedEndCal:
                    String missedEndCalMessage = participantMap.get("participant_uuid") + " missedEndCal unexpected message";
                    logger.warn("\t\t " + missedEndCalMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), missedEndCalMessage);
                    break;
                case endOfEpisode:
                    String endOfEpisodeMessage = participantMap.get("participant_uuid") + " endOfEpisode unexpected message";
                    logger.warn("\t\t " + endOfEpisodeMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), endOfEpisodeMessage);
                    break;
                default:
                    logger.error("stateNotify: Invalid state: " + getState());
            }


        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("incomingMessage");
            logger.error(exceptionAsString);
        }
    }

    private boolean isStartCal(String messageBody) {
        boolean isStart = false;
        try {

            if(messageBody.toLowerCase().contains("startcal")) {
                //Launcher.msgUtils.sendMessage(participantMap.get("number"), "startcal accepted");
                isStart = true;
            } else {
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "startcal not found please resend");
                isStart = false;
            }

        } catch (Exception ex) {
            logger.error("isStartCal(): " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(pw.toString());
        }
        return isStart;
    }

    private boolean isEndCal(String messageBody) {
        boolean isEnd = false;
        try {

            if(messageBody.toLowerCase().contains("endcal")) {
                isEnd = true;
            } else {
                isEnd = false;
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "endcal not found please resend");
            }

        } catch (Exception ex) {
            logger.error("isStartCal(): " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(pw.toString());
        }
        return isEnd;
    }

    public String saveStateJSON() {
        String stateJSON = null;
        try {

            Map<String,Long> timerMap = new HashMap<>();
            timerMap.put("stateIndex", Long.valueOf(getState().ordinal()));
            timerMap.put("startTime", startTimestamp);
            timerMap.put("currentTime", System.currentTimeMillis() / 1000);
            timerMap.put("startDeadline", Long.valueOf(getStartDeadline()));
            timerMap.put("startWarnDeadline", Long.valueOf(getStartWarnDeadline()));
            timerMap.put("endDeadline", Long.valueOf(getEndDeadline()));
            timerMap.put("endWarnDeadline", Long.valueOf(getEndWarnDeadline()));

            Map<String,Map<String,Long>> stateSaveMap = new HashMap<>();
            stateSaveMap.put("history",stateMap);
            stateSaveMap.put("timers", timerMap);

            stateJSON = gson.toJson(stateSaveMap);


        } catch (Exception ex) {
            logger.error("saveStateJSON: " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(pw.toString());

        }
        return stateJSON;
    }

    @Override
    public boolean stateNotify(String state){

        //save change to state log
        logState(state);


        if(stateMap != null) {
            stateMap.put(state, System.currentTimeMillis() / 1000);
        }
        if(startTimestamp == 0) {
            startTimestamp = System.currentTimeMillis() / 1000;
        } else {
            stateJSON = saveStateJSON();
        }


        switch (State.valueOf(state)) {
            case initial:
                //no timers
                break;
            case waitStart:

                //setting warn timer
                int startWarnDiff =  timeToD1T1159am();
                if(startWarnDiff <= 0) {
                    startWarnDiff = 300;
                }
                setStartWarnDeadline(startWarnDiff);

                String waitStartMessage = participantMap.get("participant_uuid") + " created state machine: warnStart timeout " + getDateFromEpochTz(getCurrentLocalTime() + startWarnDiff);
                logger.warn("\t\t " + waitStartMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), waitStartMessage);

                break;
            case warnStartCal:
                //set start fail timer
                setStartDeadline(timeToD2359am());

                String warnStartCalMessage = participantMap.get("participant_uuid") + " please submit startcal: startdeadline timeout " + getDateFromEpochTz(getCurrentLocalTime() + timeToD2359am());
                logger.warn("\t\t " + warnStartCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), warnStartCalMessage);


                break;
            case startcal:
                //set warn and end
                setEndWarnDeadline(timeToD19pm());

                String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: endwarndeadline timeout " + getDateFromEpochTz(getCurrentLocalTime() + timeToD19pm());
                logger.info("\t\t " + startCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), startCalMessage);

                break;
            case missedStartCal:
                String missedStartCalMessage = participantMap.get("participant_uuid") + " no startcal was recorded for today.";
                logger.warn("\t\t " + missedStartCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), missedStartCalMessage);
                break;
            case warnEndCal:
                //set end for end
                setEndDeadline(timeToD2359am());

                String warnEndCalMessage = participantMap.get("participant_uuid") + " please submit endcal: enddeadline timeout " + getDateFromEpochTz(getCurrentLocalTime() + timeToD2359am());
                logger.warn("\t\t " + warnEndCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage);

                break;
            case endcal:
                String endCalMessage = participantMap.get("participant_uuid") + " thanks for sending endcal";
                logger.info("\t\t " + endCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), endCalMessage);
                break;
            case missedEndCal:
                String missedEndCalMessage = participantMap.get("participant_uuid") + " no endcal was recorded for today.";
                logger.warn("\t\t " + missedEndCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), missedEndCalMessage);
                break;
            case endOfEpisode:
                //set restart one minute after other timeouts
                setEndOfEpisodeDeadline(timeToD2359am() + 60);
                String endOfEpisode = participantMap.get("participant_uuid") + " end of episode timeout " + getDateFromEpochTz(getCurrentLocalTime() + timeToD2359am() + 60);
                logger.info("\t\t " + endOfEpisode);
                break;
            default:
                logger.error("stateNotify: Invalid state: " + state);
        }

        return true;
    }

    public Map<String, Map<String,Long>> getSaveStateMap() {
        Map<String, Map<String,Long>> saveStateMap = gson.fromJson(stateJSON,typeOfHashMap);
        return saveStateMap;
    }

    public void restoreSaveState(String saveStateJSON) {

        try{

            //Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();
            Map<String, Map<String,Long>> saveStateMap = gson.fromJson(saveStateJSON,typeOfHashMap);

            Map<String,Long> historyMap = saveStateMap.get("history");
            Map<String,Long> timerMap = saveStateMap.get("timers");

            List<String> sortedHistoryList = saveStateMap.get("history").entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            String lastState = sortedHistoryList.get(0);

            long lastStateStartTime = historyMap.get(lastState);
            long saveStartTime = timerMap.get("startTime");
            long saveCurrentTime = timerMap.get("currentTime");
            long diffStateTimer = saveCurrentTime - lastStateStartTime;

            long saveStartWarnDeadline = timerMap.get("startWarnDeadline");
            long saveStartDeadline = timerMap.get("startDeadline");
            long saveEndWarnDeadline = timerMap.get("endWarnDeadline");
            long saveEndDeadline = timerMap.get("endDeadline");

            //set all timers
            setStartWarnDeadline((int)saveStartWarnDeadline);
            setStartDeadline((int)saveStartDeadline);
            setEndWarnDeadline((int)saveEndWarnDeadline);
            setEndDeadline((int)saveEndDeadline);

            switch (State.valueOf(lastState)) {
                case initial:
                    //no timers
                    break;
                case waitStart:
                    //change startWarnDeadline
                    //startTimeoutwaitStartTowarnStartCalHandler();
                    long newStartWarnDeadline = saveStartWarnDeadline - diffStateTimer;
                    setStartWarnDeadline((int)newStartWarnDeadline);
                    receivedWaitStart();
                    break;
                case warnStartCal:
                    //change startDeadline
                    //startTimeoutwarnStartCalTomissedStartCalHandler();
                    long newsStartDeadline = saveStartDeadline - diffStateTimer;
                    setStartDeadline((int)newsStartDeadline);
                    receivedWarnStartCal();
                    break;
                case startcal:
                    //change endWarnDeadline
                    //startTimeoutstartcalTowarnEndCalHandler();
                    long newEndWarnDeadline = saveEndWarnDeadline - diffStateTimer;
                    setEndWarnDeadline((int)newEndWarnDeadline);
                    receivedStartCal();
                    break;
                case missedStartCal:
                    //no timers
                    break;
                case warnEndCal:
                    //change endDeadline
                    //startTimeoutwarnEndCalTomissedEndCalHandler();
                    long newEndDeadline = saveEndDeadline - diffStateTimer;
                    setEndDeadline((int)newEndDeadline);
                    recievedWarnEndCal();
                    break;
                case missedEndCal:
                    break;
                case endOfEpisode:
                    break;
                default:
                    logger.error("restoreSaveState: Invalid state: " + lastState);
            }

            //logger.error("save json: " + saveStateMap.toString());

        } catch (Exception ex) {
            logger.error("restoreSaveState");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(pw.toString());
        }

    }

    private long getCurrentLocalTime() {
        long currentTime = (System.currentTimeMillis()/1000) + getDisplacedTime();

        logger.error("currentTime: " + currentTime + " System time: " + (System.currentTimeMillis()/1000) + " displaced time: " + getDisplacedTime());
        return currentTime;
    }

    private long getDisplacedTime() {
        return getDisplacedTime(Instant.now().getEpochSecond());
    }
    private long getDisplacedTime(long currentTime) {
        long personZone = ZoneId.of( participantMap.get("time_zone"))   // Specify a time zone.
                .getRules()                   // Get the object representing the rules for all the past, present, and future changes in offset used by the people in the region of that zone.
                .getOffset(Instant.ofEpochSecond(currentTime)).getTotalSeconds();   // Get a `ZoneOffset` object representing the number of hours, minutes, and seconds displaced from UTC. Here we ask for the offset in effect right now.

        long machineZone = ZonedDateTime.now().getZone()   // Specify a time zone.
                .getRules()                   // Get the object representing the rules for all the past, present, and future changes in offset used by the people in the region of that zone.
                .getOffset(Instant.ofEpochSecond(currentTime)).getTotalSeconds();

        return machineZone - personZone;

    }

    private int timeToD1T1159am() {
        Date date = new Date();   // given date

        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(date);   // assigns calendar to given date
        long currentTime = (calendar.getTime().getTime()/1000) + getDisplacedTime();

        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 59);
        long d1t1159am = calendar.getTime().getTime()/1000;
        d1t1159am = d1t1159am + getDisplacedTime(d1t1159am);
        System.out.println("\n");
        System.out.println(d1t1159am);
        System.out.println(currentTime);
        System.out.println(d1t1159am - currentTime);
        System.out.println("\n");
        return (int) (d1t1159am - currentTime);
    }

    private int timeToD19pm() {

        Date date = new Date();   // given date

        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(date);   // assigns calendar to given date
        long currentTime = (calendar.getTime().getTime()/1000) + getDisplacedTime();

        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 0);
        long d1t900pm = calendar.getTime().getTime()/1000;
        d1t900pm = d1t900pm + getDisplacedTime(d1t900pm);
        return (int) (d1t900pm - currentTime);

    }

    private int timeToD2359am() {

        Date date = new Date();   // given date

        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(date);   // assigns calendar to given date
        long currentTime = (calendar.getTime().getTime()/1000) + getDisplacedTime();

        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        long d1t400am = calendar.getTime().getTime()/1000;
        d1t400am = d1t400am + getDisplacedTime(d1t400am);

        long d2t359am;

        if(currentTime > d1t400am) {
            d2t359am = d1t400am + 86400 - 60; //add full day of seconds and subtract a minute
        } else {
            d2t359am = d1t400am - 60; //after midnight subtract a minute from 4am
        }

        return (int) (d2t359am - currentTime);

    }

    private String getDateFromEpochTz(long timestamp) {
        timestamp = timestamp * 1000;
        Date date = new Date(timestamp);
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");
        format.setTimeZone(TimeZone.getTimeZone(participantMap.get("time_zone")));
        String formatted = format.format(date);
        return formatted;
    }

    public void logState(String state) {

        if(gson != null) {

            String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat simpleDateFormat =new SimpleDateFormat(pattern, new Locale("en", "USA"));
            String date = simpleDateFormat.format(new Date());

            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            String json_string = gson.toJson(messageMap);
            //String json_string = "{\"state\":\"" + state +  "\"}";

            String insertQuery = "INSERT INTO state_log " +
                    "(participant_uuid, TS, log_json)" +
                    " VALUES ('" + participantMap.get("participant_uuid") + "', '" +
                    date + "', '" + json_string +
                    "')";

            Launcher.dbEngine.executeUpdate(insertQuery);
        }

    }


}
