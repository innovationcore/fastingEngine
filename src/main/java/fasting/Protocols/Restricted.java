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
    private boolean pauseMessages;
    private Map<String,String> incomingMap;

    public String stateJSON;

    private Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(Restricted.class.getName());

    public Restricted(String participant_uuid) {

    }

    public Restricted(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();
        this.pauseMessages = false;

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());
        // restoreSaveState();

        new Thread(){
            public void run(){
                try {
                    while (!getState().toString().equals("endOfEpisode")) {

                        if(startTimestamp > 0) {
                            stateJSON = saveStateJSON();
                            Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                            //logger.info(stateJSON);
                        }

                        Thread.sleep(900000); // 900000 = 15 mins
                    }
                } catch (Exception ex) {
                    logger.error("protocols.Restricted Thread");
                    logger.error(ex.getMessage());
                }
            }
        }.start();

    }

    public void incomingText(Map<String,String> incomingMap) {
        this.incomingMap = incomingMap;
        try {
            State state = getState();
            switch (state) {
                case initial:
                    //no timers
                    break;
                case waitStart:
                    if (isDayoff(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"STARTCAL\" and \"ENDCAL\" today.");
                    } else if (isEndCal(incomingMap.get("Body"))) {
                        // send endcal message for yesterday
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), TZHelper.yesterdaysDate()+ ": " + pickRandomEndCalMessage());
                        // update endcal time in state_log
                        String[] endCalSplit = incomingMap.get("Body").split("\\s+");
                        long unixTS;
                        if (endCalSplit.length >= 2){
                            unixTS = TZHelper.parseTime(endCalSplit[1], true);
                        } else {
                            unixTS = TZHelper.getUnixTimestampNow();
                        }
                        Launcher.dbEngine.saveEndCalTime(participantMap.get("participant_uuid"), unixTS);
                        // send successrate message depending on if <9, 9-11, or >11
                        long startTime = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                        long endTime = Launcher.dbEngine.getEndCalTime(participantMap.get("participant_uuid"));
                        int validTRE = TZHelper.determineGoodFastTime(startTime, endTime);

                        if (validTRE == -1){
                            // update the success rate
                            Launcher.dbEngine.setSuccessNextDay(participantMap.get("participant_uuid"), false);
                            String before9Msg = pickRandomLess9TRE(startTime, endTime);
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), before9Msg);
                        } else if (validTRE == 1) {
                            // update the success rate
                            Launcher.dbEngine.setSuccessNextDay(participantMap.get("participant_uuid"), false);
                            String after11Msg = pickRandomGreater11TRE();
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), after11Msg);
                        } else {
                            String successMsg = pickRandomSuccessTRE();
                            // update the success rate
                            Launcher.dbEngine.setSuccessNextDay(participantMap.get("participant_uuid"), true);
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), successMsg);
                        }

                        // send message if endcal is after 8pm
                        if (TZHelper.isAfter8PM(endTime)) {
                            String after8PMMsg = randomAfter8PMMessage();
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), after8PMMsg);
                        }
                    } else if(isStartCal(incomingMap.get("Body"))) {
                        receivedStartCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case warnStartCal:
                    if (isDayoff(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"STARTCAL\" and \"ENDCAL\" today.");
                    } else if (isEndCal(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), TZHelper.yesterdaysDate() + ": " + pickRandomEndCalMessage());
                        // update startcal time in state_log
                        String[] endCalSplit = incomingMap.get("Body").split("\\s+");
                        long unixTS;
                        if (endCalSplit.length >= 2){
                            unixTS = TZHelper.parseTime(endCalSplit[1], true);
                        } else {
                            unixTS = TZHelper.getUnixTimestampNow();
                        }
                        Launcher.dbEngine.saveEndCalTime(participantMap.get("participant_uuid"), unixTS);
                        long startTime1 = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                        long endTime1 = Launcher.dbEngine.getEndCalTime(participantMap.get("participant_uuid"));
                        int validTRE1 = TZHelper.determineGoodFastTime(startTime1, endTime1);

                        if (validTRE1 == -1){
                            // update the success rate
                            Launcher.dbEngine.setSuccessNextDay(participantMap.get("participant_uuid"), false);
                            String before9Msg1 = pickRandomLess9TRE(startTime1, endTime1);
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), before9Msg1);
                        } else if (validTRE1 == 1) {
                            // update the success rate
                            Launcher.dbEngine.setSuccessNextDay(participantMap.get("participant_uuid"), false);
                            String after11Msg1 = pickRandomGreater11TRE();
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), after11Msg1);
                        } else {
                            // update the success rate
                            Launcher.dbEngine.setSuccessNextDay(participantMap.get("participant_uuid"), true);
                            String successMsg1 = pickRandomSuccessTRE();
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), successMsg1);
                        }

                        // send message if endcal is after 8pm
                        if (TZHelper.isAfter8PM(endTime1)) {
                            String after8PMMsg1 = randomAfter8PMMessage();
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), after8PMMsg1);
                        }
                    } else if(isStartCal(incomingMap.get("Body"))) {
                        receivedStartCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case startcal:
                    if (isDayoff(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"ENDCAL\" today. ");
                    } else if(isStartCal(incomingMap.get("Body"))){
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "You've already started consuming calories for the day. Text \"ENDCAL\" when you finish your TRE today.");
                    } else if(isEndCal(incomingMap.get("Body"))) {
                        String[] endCalSplit = incomingMap.get("Body").split("\\s+");
                        boolean isBetween3AMand3PM;
                        if (endCalSplit.length >= 2){
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(TZHelper.parseTime(endCalSplit[1], false));
                        } else {
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(); // gets time now
                        }
                        if (isBetween3AMand3PM) {
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. " +
                                                                                        "Try again in the evening. Text 270-402-2214 if you want to receive " +
                                                                                        "a call about how to manage TRE safely.");
                        } else {
                            receivedEndCal();
                        }
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case missedStartCal:
                    String missedStartCalMessage =  participantMap.get("participant_uuid") + " missedStartCal unexpected message";
                    logger.warn(missedStartCalMessage);
                    break;
                case warnEndCal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        String[] endCalSplit = incomingMap.get("Body").split("\\s+");
                        boolean isBetween3AMand3PM;
                        if (endCalSplit.length >= 2){
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(TZHelper.parseTime(endCalSplit[1], false));
                        } else {
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(); // gets time now
                        }
                        if (isBetween3AMand3PM){
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. " +
                                                                                        "Try again in the evening. Text 270-402-2214 if you want to receive " +
                                                                                        "a call about how to manage TRE safely.");
                        } else {
                            receivedEndCal();
                        }
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day. ");
                    }
                    break;
                case endcal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        String[] endCalSplit = incomingMap.get("Body").split("\\s+");
                        boolean isBetween3AMand3PM;
                        if (endCalSplit.length >= 2){
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(TZHelper.parseTime(endCalSplit[1], false));
                        } else {
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(); // gets time now
                        }
                        if (isBetween3AMand3PM){
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. " +
                                                                                        "Try again in the evening. Text 270-402-2214 if you want to receive " +
                                                                                        "a call about how to manage TRE safely.");
                        } else {
                            receivedEndCal();
                        }
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case missedEndCal:
                    String missedEndCalMessage = participantMap.get("participant_uuid") + " missedEndCal unexpected message";
                    logger.warn(missedEndCalMessage);
                    break;
                case endOfEpisode:
                    String endOfEpisodeMessage = participantMap.get("participant_uuid") + " endOfEpisode unexpected message";
                    logger.warn(endOfEpisodeMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    break;
                default:
                    logger.error("stateNotify: Invalid state: " + getState());
            }


        } catch (Exception ex) {
            logger.error("incomingText");
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    private boolean isDayoff(String messageBody) {
        boolean isDayoff = false;
        try {
            if(messageBody.toLowerCase().contains("dayoff")) {
                isDayoff = true;
            } else {
                isDayoff = false;
            }

        } catch (Exception ex) {
            logger.error("isDayoff()");
            logger.error(ex.getMessage());
        }
        return isDayoff;
    }

    private boolean isStartCal(String messageBody) {
        boolean isStart = false;
        try {

            if(messageBody.toLowerCase().contains("startcal")) {
                isStart = true;
            } else {
                isStart = false;
            }

        } catch (Exception ex) {
            logger.error("isStartCal()");
            logger.error(ex.getMessage());
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
            }

        } catch (Exception ex) {
            logger.error("isStartCal()");
            logger.error(ex.getMessage());
        }
        return isEnd;
    }

    public String saveStateJSON() {
        String stateJSON = null;
        try {
            Map<String,Long> timerMap = new HashMap<>();
            timerMap.put("stateIndex", Long.valueOf(getState().ordinal()));
            timerMap.put("startTime", startTimestamp);
            timerMap.put("currentTime", System.currentTimeMillis() / 1000); //unix seconds
            timerMap.put("startDeadline", Long.valueOf(getStartDeadline()));
            timerMap.put("startWarnDeadline", Long.valueOf(getStartWarnDeadline()));
            timerMap.put("endDeadline", Long.valueOf(getEndDeadline()));
            timerMap.put("endWarnDeadline", Long.valueOf(getEndWarnDeadline()));
            timerMap.put("endOfEpisodeDeadline", Long.valueOf(getEndOfEpisodeDeadline()));

            Map<String,Map<String,Long>> stateSaveMap = new HashMap<>();
            stateSaveMap.put("history",stateMap);
            stateSaveMap.put("timers", timerMap);

            stateJSON = gson.toJson(stateSaveMap);

        } catch (Exception ex) {
            logger.error("saveStateJSON");
            logger.error(ex.getMessage());

        }
        return stateJSON;
    }

    @Override
    public boolean stateNotify(String state){

        // can't add a pause here, because it will get stuck if it restarts next day
        logState(state);
        
        long unixTS;

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
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case warnStartCal:
                //set start fail timer
                setStartDeadline(TZHelper.getSecondsTo359am()); // timeToD2359am());
                String warnStartCalMessageLog = participantMap.get("participant_uuid") + " please submit startcal: startdeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo359am());
                logger.warn(warnStartCalMessageLog);
                // send reminder message at noon
                String warnStartCalMessage = "No \"STARTCAL\" received yet today. Please send us your \"STARTCAL\" so we know when your calories began today.";
                if (!pauseMessages){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnStartCalMessage);
                }
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case startcal:
                //set warn timer
                // In the CSV with responses they don't have anything for sending startcal
                int secondsTo2059pm = TZHelper.getSecondsTo2059pm();
                // if after 9pm, don't immediately send warnEndCal message. Wait some time so user has time to respond
                if (secondsTo2059pm < 0) {
                    secondsTo2059pm += 300; // add 5 minutes
                }
                setEndWarnDeadline(secondsTo2059pm); //timeToD19pm());
                String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: endwarndeadline timeout " + TZHelper.getDateFromAddingSeconds(secondsTo2059pm);
                logger.info(startCalMessage);
                
                // update startcal time in state_log
                if (incomingMap == null) {
                    unixTS = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                    if (unixTS == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                } else {
                    String[] startCalSplit = incomingMap.get("Body").split("\\s+");
                    if (startCalSplit.length >= 2){
                        unixTS = TZHelper.parseTime(startCalSplit[1], false);
                    } else {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                }

                Launcher.dbEngine.saveStartCalTime(participantMap.get("participant_uuid"), unixTS);
                
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case missedStartCal:
                String missedStartCalMessage = "We haven't heard from you in a while. Remember to text \"STARTCAL\" when your calories start " +
                                                "in the morning and \"ENDCAL\" when your calories finish at night! Let us know if you need help.";
                logNoEndCal();
                if (getDaysWithoutEndCal() >= 2){ // if it has been 2 days without startcal, text this
                    missedStartCalMessage = "We haven't heard from you in a while. Text our study team at 270-402-2214 if you're struggling to stick with the time-restricted eating.";
                    resetNoEndCal();
                }
                if (!pauseMessages) {
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), missedStartCalMessage);
                    Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false);
                }
                logger.warn(missedStartCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case warnEndCal:
                //set end for endcal
                setEndDeadline(TZHelper.getSecondsTo359am());
                String warnEndCalMessage = participantMap.get("first_name") +  ", we haven't heard from you. Remember to text \"ENDCAL\" when you go calorie free.";
                if (!pauseMessages){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage);
                }
                logger.warn(warnEndCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case endcal:
                String endCalMessage = pickRandomEndCalMessage();
                logger.info(endCalMessage);
                if (!pauseMessages){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), endCalMessage);
                }
                resetNoEndCal();
                // update endcal time in state_log
                if (incomingMap == null) {
                    unixTS = Launcher.dbEngine.getEndCalTime(participantMap.get("participant_uuid"));
                    if (unixTS == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                } else {
                    String[] endCalSplit = incomingMap.get("Body").split("\\s+");
                    if (endCalSplit.length >= 2){
                        unixTS = TZHelper.parseTime(endCalSplit[1], false);
                    } else {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                }
                // save endcal time to state_log
                Launcher.dbEngine.saveEndCalTime(participantMap.get("participant_uuid"), unixTS);
                // determine if the user endcal'd <9, 9-11, >11
                long startTime = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                long endTime = Launcher.dbEngine.getEndCalTime(participantMap.get("participant_uuid"));
                int validTRE = TZHelper.determineGoodFastTime(startTime, endTime);
                if (validTRE == -1){
                    if (!pauseMessages){
                        // update the success rate
                        Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false);
                        String before9Msg = pickRandomLess9TRE(startTime, endTime);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), before9Msg);
                    }
                } else if (validTRE == 1) {
                    if (!pauseMessages){
                        // update the success rate
                        Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false);
                        String after11Msg = pickRandomGreater11TRE();
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), after11Msg);
                    }
                } else {
                    if (!pauseMessages){
                        // update the success rate
                        Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), true);
                        String successMsg = pickRandomSuccessTRE();
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), successMsg);
                    }
                }

                // send message if endcal is after 8pm
                if (TZHelper.isAfter8PM(endTime)) {
                    if (!pauseMessages){
                        String after8PMMsg = randomAfter8PMMessage();
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), after8PMMsg);
                    }
                }
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case missedEndCal:
                // TRF ended too late messages should be sent after 11pm, maybe reminder
                String missedEndCalMessage = participantMap.get("participant_uuid") + " no endcal was recorded for today.";
                logger.warn(missedEndCalMessage);
                logNoEndCal();
                if (getDaysWithoutEndCal() >= 2){
                    String missed2EndCals = "We haven't heard from you in a while. Text our study team at 270-402-2214 if you're struggling to stick with the time-restricted eating.";
                    if (!pauseMessages){
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), missed2EndCals);
                    }
                    resetNoEndCal();
                }
                if (!pauseMessages){
                    Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false);
                }
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case endOfEpisode:
                //set restart one minute after other timeouts
                setEndOfEpisodeDeadline(TZHelper.getSecondsTo4am());// timeToD2359am() + 60);
                String endOfEpisode = participantMap.get("participant_uuid") + " end of episode timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo4am());
                logger.info(endOfEpisode);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
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

    public void restoreSaveState() {
        try{
            String saveStateJSON = Launcher.dbEngine.getSaveState(participantMap.get("participant_uuid"));

            if (!saveStateJSON.equals("")){
                Map<String, Map<String,Long>> saveStateMap = gson.fromJson(saveStateJSON,typeOfHashMap);

                Map<String,Long> historyMap = saveStateMap.get("history");
                Map<String,Long> timerMap = saveStateMap.get("timers");

                int stateIndex = (int) timerMap.get("stateIndex").longValue();
                String stateName = State.values()[stateIndex].toString();

                long saveCurrentTime = timerMap.get("currentTime");

                // if same day (<4am) below is correct
                // if next day (>=4am) need to reset to waitStart

                // if past 4am, reset everything to beginning
                boolean isSameDay = TZHelper.isSameDay(saveCurrentTime);
                if (!isSameDay) {
                    stateName = "waitStart";
                }

                switch (State.valueOf(stateName)) {
                    case initial:
                        //no timers
                        break;
                    case waitStart:
                        //resetting warn timer
                        int startWarnDiff =  TZHelper.getSecondsTo1159am();  //timeToD1T1159am();
                        if(startWarnDiff <= 0) {
                            startWarnDiff = 300;
                        }
                        this.pauseMessages = true;
                        setStartWarnDeadline(startWarnDiff);
                        receivedWaitStart(); // initial to waitStart
                        this.pauseMessages = false;
                        break;
                    case warnStartCal:
                        //reset startDeadline
                        int secondsTo359am0 = TZHelper.getSecondsTo359am();
                        if (secondsTo359am0 < 0) {
                            secondsTo359am0 = 0;
                        }
                        this.pauseMessages = true;
                        setStartDeadline(secondsTo359am0); // timeToD2359am());
                        receivedWarnStartCal(); // initial to warnStartCal
                        this.pauseMessages = false;
                        break;
                    case startcal:
                        //reset endWarnDeadline
                        int secondsTo2059pm = TZHelper.getSecondsTo2059pm();
                        if (secondsTo2059pm < 0) {
                            secondsTo2059pm = 0;
                        }
                        long unixTS = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                        if (unixTS == 0) {
                            unixTS = TZHelper.getUnixTimestampNow();
                        }
                        Launcher.dbEngine.saveStartCalTime(participantMap.get("participant_uuid"), unixTS);
                        this.pauseMessages = true;
                        setEndWarnDeadline(secondsTo2059pm); //timeToD19pm());
                        receivedStartCal();
                        this.pauseMessages = false;
                        break;
                    case missedStartCal:
                        //no timers
                        break;
                    case warnEndCal:
                        //reset endDeadline
                        int secondsTo359am1 = TZHelper.getSecondsTo359am();
                        if (secondsTo359am1 < 0) {
                            secondsTo359am1 = 0;
                        }
                        this.pauseMessages = true;
                        setEndDeadline(secondsTo359am1);
                        recievedWarnEndCal();
                        this.pauseMessages = false;
                        break;
                    case missedEndCal:
                        break;
                    case endcal:
                        //no timers
                        break;
                    case endOfEpisode:
                        // reset endOfEpisodeDeadline
                        int secondsTo4am = TZHelper.getSecondsTo4am();
                        if (secondsTo4am < 0) {
                            secondsTo4am = 0;
                        }
                        this.pauseMessages = true;
                        setEndOfEpisodeDeadline(secondsTo4am);
                        // quickest path to endOfEpisode, move it but don't save it
                        receivedStartCal();
                        receivedEndCal();
                        this.pauseMessages = false;
                        break;
                    default:
                        logger.error("restoreSaveState: Invalid state: " + stateName);
                }
            }
            else {
                logger.info("restoreSaveState: no save state found for " + participantMap.get("participant_uuid"));
                int startWarnDiff =  TZHelper.getSecondsTo1159am();  //timeToD1T1159am();
                if(startWarnDiff <= 0) {
                    startWarnDiff = 300;
                }
                setStartWarnDeadline(startWarnDiff);
                receivedWaitStart(); // initial to waitStart
            }

        } catch (Exception ex) {
            logger.error("restoreSaveState");
            logger.error(ex.getMessage());
        }
    }

    private void logNoEndCal(){
        Launcher.dbEngine.updateDaysWithoutEndCal(participantMap.get("participant_uuid"));
    }

    private void resetNoEndCal(){
        Launcher.dbEngine.resetDaysWithoutEndCal(participantMap.get("participant_uuid"));
    }

    private int getDaysWithoutEndCal(){
        return Launcher.dbEngine.getDaysWithoutEndCal(participantMap.get("participant_uuid"));
    }

    public void logState(String state) {
        if(gson != null) {
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            if (pauseMessages) {
                messageMap.put("restored","true");
            }
            String json_string = gson.toJson(messageMap);

            String insertQuery = "INSERT INTO state_log " +
                    "(participant_uuid, TS, log_json)" +
                    " VALUES ('" + participantMap.get("participant_uuid") + "', " +
                    "GETUTCDATE(), '" + json_string +
                    "')";

            Launcher.dbEngine.executeUpdate(insertQuery);
        }
    }

    public String pickRandomEndCalMessage() {
        // this is a list of responses for when a participant sends endcal
        final List<String> endCalMessages = Collections.unmodifiableList(
        new ArrayList<String>() {{
            add("Thanks! You are now running calorie free!");
            add("Thanks for your response. You can do it!");
            add("Thanks, [NAME]! Here we go! Make sure your kitchen is in lock down!");
            add("Message received. Good luck!");
            add("Great. Now lock those kitchen cabinets!");
            add("Thanks. You are now nibble free.");
            add("Message received. Now stay out of the kitchen. Doctor's orders!");
            add("Thanks. Now go bolt the refrigerator door shut.");
            add("Got it. Now put your tummy to sleep until the morning!");
            add("Muchas gracias. Now go put your feet up. You've earned it!");
        }});
        Random rand = new Random();
        int rnd = new Random().nextInt(endCalMessages.size());
        String message = endCalMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        return message;
    }

    public String pickRandomSuccessTRE(){
        // this is a list of responses for when a participant sends endcal
        final List<String> successMessages = Collections.unmodifiableList(
        new ArrayList<String>() {{
            add("Good show! Your overall success rate is now [SUCCESS]!");
            add("Perfect! Your overall success rate is now [SUCCESS]!");
            add("Smashing! Your overall success rate is now [SUCCESS]!");
            add("Bravo, [NAME]! Your overall success rate is now [SUCCESS]!");
            add("Jolly good show! Your overall success rate is now [SUCCESS]!");
            add("Superb! Your overall success rate is now [SUCCESS]!");
            add("Great! Your overall success rate is now [SUCCESS]!");
        }});
        Random rand = new Random();
        int rnd = new Random().nextInt(successMessages.size());
        String message = successMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        if (message.contains("[SUCCESS]")) {
            String successRate = Launcher.dbEngine.getSuccessRate(participantMap.get("participant_uuid"));
            message = message.replace("[SUCCESS]", successRate);
        }
        return message;
    }

    public String pickRandomLess9TRE(long startTime, long endTime){
        // this is a list of responses for when a participant sends endcal
        final List<String> successMessages = Collections.unmodifiableList(
        new ArrayList<String>() {{
            add("You ended your time-restricted eating [SHORT] too early. Your success rate is now [SUCCESS]. Try planning ahead for how you will end your TRE.");
            add("You ended your time-restricted eating too early! [NAME], your success rate is now [SUCCESS]. Have small snacks or meals ready so you can stay on time.");
            add("[NAME], you ended your time-restricted eating too early! Your success rate is now [SUCCESS]. If you need help, get a family member to help you end your fasting time!");
            add("You ended your time-restricted eating too early. Your success rate is now [SUCCESS]. Try putting Post-Its on your fridge and cupboards to help you remember your target End Calories time!");
        }});
        Random rand = new Random();
        int rnd = new Random().nextInt(successMessages.size());
        String message = successMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        if (message.contains("[SUCCESS]")) {
            String successRate = Launcher.dbEngine.getSuccessRate(participantMap.get("participant_uuid"));
            message = message.replace("[SUCCESS]", successRate);
        }
        if (message.contains("[SHORT]")) {
            String shortTime = TZHelper.getHoursMinutesBefore(startTime, endTime, 32400L); // 9 hours
            message = message.replace("[SHORT]", shortTime);
        }
        return message;
    }

    public String pickRandomGreater11TRE(){
        // this is a list of responses for when a participant sends endcal
        final List<String> successMessages = Collections.unmodifiableList(
        new ArrayList<String>() {{
            add("You ended your TRE too late. Your success rate is now [SUCCESS]. Try planning ahead for how you will end calories earlier.");
            add("You slipped up and ended too late. Your success rate is now [SUCCESS]. Let's get back on track tomorrow!");
            add("[NAME], you exceeded the target eating window! Your success rate is now [SUCCESS]. If you need help, get a family member to help you end your calories on time!");
            add("You slipped up and consumed calories for too long. Your success rate is now [SUCCESS]. Let's get back on track tomorrow!");
        }});
        Random rand = new Random();
        int rnd = new Random().nextInt(successMessages.size());
        String message = successMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        if (message.contains("[SUCCESS]")) {
            String successRate = Launcher.dbEngine.getSuccessRate(participantMap.get("participant_uuid"));
            message = message.replace("[SUCCESS]", successRate);
        }
        return message;
    }

    public String randomAfter8PMMessage(){
        // this is a list of responses for when a participant sends endcal
        final List<String> successMessages = Collections.unmodifiableList(
        new ArrayList<String>() {{
            add("You ended your time-restricted eating too late. Try to keep your calories before 8pm.");
            add("You ended your time-restricted eating after 8pm. Try planning ahead for how you will end your daily calories earlier.");
            add("You ended your time-restricted eating after 8pm. Try using a recurring alarm or a family member to help you end calories earlier.");
        }});
        Random rand = new Random();
        int rnd = new Random().nextInt(successMessages.size());
        String message = successMessages.get(rnd);
        return message;
    }
}
