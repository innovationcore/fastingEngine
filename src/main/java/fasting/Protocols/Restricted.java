package fasting.Protocols;

import fasting.TimeUtils.TimezoneHelper;
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
    private TimezoneHelper TZHelper;

    public String stateJSON;

    private Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(Restricted.class.getName());

    public Restricted(String participant_uuid) {

    }

    public Restricted(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());

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
                    logger.warn(missedStartCalMessage);
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
                    logger.warn(missedEndCalMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), missedEndCalMessage);
                    break;
                case endOfEpisode:
                    String endOfEpisodeMessage = participantMap.get("participant_uuid") + " endOfEpisode unexpected message";
                    logger.warn(endOfEpisodeMessage);
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
                int startWarnDiff =  TZHelper.getSecondsTo1159am();  //timeToD1T1159am();
                if(startWarnDiff <= 0) {
                    startWarnDiff = 300;
                }
                setStartWarnDeadline(startWarnDiff);

                String waitStartMessage = participantMap.get("participant_uuid") + " created state machine: warnStart timeout " + TZHelper.getDateFromAddingSeconds(startWarnDiff);
                logger.warn(waitStartMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), waitStartMessage);

                break;
            case warnStartCal:
                //set start fail timer
                setStartDeadline(TZHelper.getSecondsTo359am()); // timeToD2359am());

                String warnStartCalMessage = participantMap.get("participant_uuid") + " please submit startcal: startdeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo359am());
                logger.warn(warnStartCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), warnStartCalMessage);


                break;
            case startcal:
                //set warn and end
                setEndWarnDeadline(TZHelper.getSecondsTo2059pm()); //timeToD19pm());

                String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: endwarndeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo2059pm());
                logger.info(startCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), startCalMessage);

                break;
            case missedStartCal:
                String missedStartCalMessage = participantMap.get("participant_uuid") + " no startcal was recorded for today.";
                logger.warn(missedStartCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), missedStartCalMessage);
                break;
            case warnEndCal:
                //set end for end
                setEndDeadline(TZHelper.getSecondsTo359am()); //timeToD2359am());

                String warnEndCalMessage = participantMap.get("participant_uuid") + " please submit endcal: enddeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo359am()); // timeToD2359am());
                logger.warn(warnEndCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage);

                break;
            case endcal:
                String endCalMessage = participantMap.get("participant_uuid") + " thanks for sending endcal";
                logger.info(endCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), endCalMessage);
                break;
            case missedEndCal:
                String missedEndCalMessage = participantMap.get("participant_uuid") + " no endcal was recorded for today.";
                logger.warn(missedEndCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), missedEndCalMessage);
                break;
            case endOfEpisode:
                //set restart one minute after other timeouts
                setEndOfEpisodeDeadline(TZHelper.getSecondsTo4am());// timeToD2359am() + 60);
                String endOfEpisode = participantMap.get("participant_uuid") + " end of episode timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo4am());
                logger.info(endOfEpisode);
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

    public void logState(String state) {
        if(gson != null) {
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            String json_string = gson.toJson(messageMap);

            String insertQuery = "INSERT INTO state_log " +
                    "(participant_uuid, TS, log_json)" +
                    " VALUES ('" + participantMap.get("participant_uuid") + "', " +
                    "GETUTCDATE(), '" + json_string +
                    "')";

            Launcher.dbEngine.executeUpdate(insertQuery);
        }
    }


}
