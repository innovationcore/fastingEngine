package fasting.Protocols.CCW.CCW_Restricted;

import fasting.TimeUtils.TimezoneHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CCW_Restricted extends CCW_RestrictedBase {
    private final int TRIAL_PERIOD = 7; // days
    private final Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    //id, participant_uuid, phone_number, participant_type
    private final Map<String, String> participantMap;
    private final Map<String,Long> stateMap;
    private long startTimestamp = 0;
    public final TimezoneHelper TZHelper;
    private boolean pauseMessages;
    private boolean isReset;
    private boolean isDayOff;
    private boolean isFromYesterday = false;
    private Map<String,String> incomingMap;
    private int endcalRepeats;

    private boolean wasSucessfulFast;
    private int numberOfCyclesInProtocol; // keeps a count of the number of cycles that a participant has been in
    public String stateJSON;
    private final Gson gson;
    public ScheduledExecutorService uploadSave;
    private static final Logger logger = LoggerFactory.getLogger(CCW_Restricted.class.getName());

    public CCW_Restricted(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();
        this.pauseMessages = false;
        this.isReset = false;
        this.isDayOff = false;
        this.endcalRepeats = 0;
        this.wasSucessfulFast = false;

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());

        // get number of days in protocol
        this.numberOfCyclesInProtocol = Launcher.dbEngine.getNumberOfCycles(participantMap.get("participant_uuid"));

        //create timer
        this.uploadSave = Executors.newScheduledThreadPool(1);
        //set timer
        this.uploadSave.scheduleAtFixedRate(() -> {
            try {
                if (!getState().toString().equals("endOfEpisode") || !getState().toString().equals("endProtocol")) {

                    if (startTimestamp > 0) {
                        stateJSON = saveStateJSON();
                        boolean didUpload = Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                        if (!didUpload) {
                            logger.error("saveState failed to upload for participant: " + participantMap.get("participant_uuid"));
                        }
                    }

                    String currentTimezone = Launcher.dbEngine.getParticipantTimezone(participantMap.get("participant_uuid"));
                    if (!participantMap.get("time_zone").equals(currentTimezone) && !currentTimezone.equals("")) {
                        participantMap.put("time_zone", currentTimezone);
                        TZHelper.setUserTimezone(currentTimezone);
                    }
                }
            } catch (Exception ex) {
                logger.error("protocols.HPM_Restricted Thread");
                logger.error(ex.getMessage());
            }
        }, 30, 900, TimeUnit.SECONDS); //900 sec is 15 mins

    } // HPM_restricted

    public void incomingText(Map<String,String> incomingMap) {
        this.incomingMap = incomingMap;
        try {
            State state = getState();
            switch (state) {
                case initial:
                    //no timers
                    break;
                case waitStart: // fall through to warnstart bc same
                case warnStartCal:
                    if (isDayoff(incomingMap.get("Body"))) {
                        receivedDayOff();
                    } else if (isEndCal(incomingMap.get("Body"))){
                        // send error to participant
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Cannot receive \"ENDCAL\" after 4am. Please note the time of \"ENDCAL\" and tell the study coordinator at your next communication.");
                        // send message to study admin
                        Launcher.msgUtils.sendMessage("+12704022214", "Participant " +participantMap.get("first_name")+ " " + participantMap.get("last_name") + " sent ENDCAL after 4am and before STARTCAL.");
                    } else if(isStartCal(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] startCalSplit = textBody.split(" ", 2);
                        if (startCalSplit.length >= 2) {
                            if (!(startCalSplit[1].toLowerCase().contains("a") || startCalSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time including \"am\" or \"pm\". For example, \"STARTCAL 7:30 am\".");
                                break;
                            }

                            long parsedTime = TZHelper.parseTime(startCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".");
                                break;
                            }
                        } else {
                            // if just sent startcal make sure its not startcal9:45 or something similar
                            if (startCalSplit[0].length() > 8){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".");
                                break;
                            }
                        }
                        receivedStartCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case startcal:
                    if (isDayoff(incomingMap.get("Body"))) {
                        receivedDayOff();
                    } else if(isStartCal(incomingMap.get("Body"))){
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "You've already started consuming calories for the day. Text \"ENDCAL\" when you finish your TRE today.");
                    } else if(isEndCal(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        boolean isBetween3AMand3PM;
                        if (endCalSplit.length >= 2){
                            if (!(endCalSplit[1].toLowerCase().contains("a") || endCalSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time including \"am\" or \"pm\". For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(parsedTime);
                        } else {
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(); // gets time now
                            // if just sent stopcal/endcal make sure its not endcal9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("stopcal")){
                                if (endCalSplit[0].length() > 7){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                    break;
                                }
                            } else if (incomingMap.get("Body").toLowerCase().contains("endcal")){
                                if (endCalSplit[0].length() > 6){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                    break;
                                }
                            }
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
                    if (isDayoff(incomingMap.get("Body"))){
                        receivedDayOff();
                    } else if (isEndCal(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        boolean isBetween3AMand3PM;
                        if (endCalSplit.length >= 2){
                            if (!(endCalSplit[1].toLowerCase().contains("a") || endCalSplit[1].toLowerCase().contains("p"))) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time including \"am\" or \"pm\". For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(parsedTime);
                        } else {
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(); // gets time now
                            // if just sent stopcal/endcal make sure its not endcal9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("stopcal")){
                                if (endCalSplit[0].length() > 7){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                    break;
                                }
                            } else if (incomingMap.get("Body").toLowerCase().contains("endcal")){
                                if (endCalSplit[0].length() > 6){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                    break;
                                }
                            }
                        }
                        if (isBetween3AMand3PM){
                            if(!this.isDayOff){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. " +
                                        "Try again in the evening. Text 270-402-2214 if you want to receive " +
                                        "a call about how to manage TRE safely.");
                            }
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
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        boolean isBetween3AMand3PM;
                        if (endCalSplit.length >= 2){
                            if (!(endCalSplit[1].toLowerCase().contains("a") || endCalSplit[1].toLowerCase().contains("p"))) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time including \"am\" or \"pm\". For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(parsedTime);
                        } else {
                            isBetween3AMand3PM = TZHelper.isBetween3AMand3PM(); // gets time now
                            // if just sent stopcal/endcal make sure its not endcal9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("stopcal")){
                                if (endCalSplit[0].length() > 7){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                    break;
                                }
                            } else if (incomingMap.get("Body").toLowerCase().contains("endcal")){
                                if (endCalSplit[0].length() > 6){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                    break;
                                }
                            }
                        }
                        if (isBetween3AMand3PM){
                            if(!this.isDayOff){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. " +
                                        "Try again in the evening. Text 270-402-2214 if you want to receive " +
                                        "a call about how to manage TRE safely.");
                            }
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
                    if(isEndCal(incomingMap.get("Body"))) {
                        receivedEndCal();
                    }
                    else if (isDayoff(incomingMap.get("Body"))){
                        receivedDayOff();
                    }else {
                        String endOfEpisodeMessage = participantMap.get("participant_uuid") + " endOfEpisode unexpected message";
                        logger.warn(endOfEpisodeMessage);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    }
                    break;
                case dayOffEndOfEpisode:
                    logger.warn(participantMap.get("participant_uuid") + " dayOffEndOfEpisode unexpected message");
                    break;
                case resetEpisodeVariables:
                    logger.warn(participantMap.get("participant_uuid") + " resetEpisodeVariables unexpected message");
                    break;
                case dayOffWait:
                    logger.warn(participantMap.get("participant_uuid") + " dayOffWait unexpected message");
                    break;
                case dayOffWarn:
                    logger.warn(participantMap.get("participant_uuid") + " dayOffWarn unexpected message");
                    break;
                case dayOffStartCal:
                    logger.warn(participantMap.get("participant_uuid") + " dayOffStartCal unexpected message");
                    break;
                case dayOffWarnEndCal:
                    logger.warn(participantMap.get("participant_uuid") + " dayOffWarnEndCal unexpected message");
                    break;
                case endProtocol:
                    logger.warn(participantMap.get("participant_uuid") + " endProtocol unexpected message");
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
            if(messageBody.toLowerCase().contains("dayoff") || messageBody.toLowerCase().contains("day off")) {
                isDayoff = true;
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
            if(messageBody.toLowerCase().contains("endcal") || messageBody.toLowerCase().contains("stopcal")) {
                isEnd = true;
            }

        } catch (Exception ex) {
            logger.error("isEndCal()");
            logger.error(ex.getMessage());
        }
        return isEnd;
    }

    public String saveStateJSON() {
        String stateJSON = null;
        try {
            Map<String,Long> timerMap = new HashMap<>();
            timerMap.put("stateIndex", (long) getState().ordinal());
            timerMap.put("startTime", startTimestamp);
            timerMap.put("currentTime", System.currentTimeMillis() / 1000); //unix seconds
            timerMap.put("startDeadline", (long) getStartDeadline());
            timerMap.put("startWarnDeadline", (long) getStartWarnDeadline());
            timerMap.put("endDeadline", (long) getEndDeadline());
            timerMap.put("endWarnDeadline", (long) getEndWarnDeadline());
            timerMap.put("endOfEpisodeDeadline", (long) getEndOfEpisodeDeadline());

            stateMap.put("endcalCount", (long) endcalRepeats);

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

        long unixTS;

        logState(state);

        if (stateMap != null) {
            stateMap.put(state, System.currentTimeMillis() / 1000);
        }
        if (startTimestamp == 0) {
            startTimestamp = System.currentTimeMillis() / 1000;
        } else {
            stateJSON = saveStateJSON();
        }

        switch (State.valueOf(state)) {
            case initial:
                //no timers
                break;
            case waitStart:
                this.numberOfCyclesInProtocol = Launcher.dbEngine.getNumberOfCycles(participantMap.get("participant_uuid"));
                //setting warn timer
                int startWarnDiff =  TZHelper.getSecondsTo1159am();
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
                setStartDeadline(TZHelper.getSecondsTo359am());
                String warnStartCalMessageLog = participantMap.get("participant_uuid") + " please submit startcal: startdeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo359am());
                logger.warn(warnStartCalMessageLog);
                // send reminder message at noon
                String warnStartCalMessage = "No \"STARTCAL\" received yet today. Please send us your \"STARTCAL\" so we know when your calories began today.";
                if (!this.pauseMessages && !this.isFromYesterday) {
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnStartCalMessage);
                } else {
                    this.isFromYesterday = false;
                    this.pauseMessages = false;
                }
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case startcal:
                //set warn timer
                int secondsTo2059pm = TZHelper.getSecondsTo2059pm();
                if (secondsTo2059pm < 0) {
                    secondsTo2059pm = 300; // add 5 minutes
                }
                setEndWarnDeadline(secondsTo2059pm);
                String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: endwarndeadline timeout " + TZHelper.getDateFromAddingSeconds(secondsTo2059pm);
                logger.info(startCalMessage);

                // update startcal time in state_log
                if (incomingMap == null) {
                    unixTS = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                    if (unixTS == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                } else {
                    String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                    String[] startCalSplit = textBody.split(" ", 2);
                    if (startCalSplit.length >= 2){
                        unixTS = TZHelper.parseTime(startCalSplit[1]);
                    } else {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                }

                Launcher.dbEngine.saveStartCalTime(participantMap.get("participant_uuid"), unixTS);

                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));

                // if startcal time is good, send message with when they should send their endcal time
                String time10HoursLater = TZHelper.getTimeIn10Hours(unixTS);
                if (!this.pauseMessages && !this.isDayOff) {
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Thank you for sending your STARTCAL! Be sure to finish your last calorie today by " + time10HoursLater + ".");
                }
                break;
            case missedStartCal:
                String missedStartCalMessage = "We haven't heard from you in a while. Remember to text \"STARTCAL\" when your calories start " +
                        "in the morning and \"ENDCAL\" when your calories finish at night! Let us know if you need help.";
                logNoEndCal();
                if (getDaysWithoutEndCal() >= 2){ // if it has been 2 days without startcal, text this
                    missedStartCalMessage = "We haven't heard from you in a while. Text our study team at 270-402-2214 if you're struggling to stick with the time-restricted eating.";
                    resetNoEndCal();
                }
                if (!this.pauseMessages && !this.isDayOff) {
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), missedStartCalMessage);
                    if (this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD) {
                        Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false, false);
                    }
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
                if (!this.pauseMessages){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage);
                } else {
                    this.pauseMessages = false;
                }
                logger.warn(warnEndCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case endcal:
                endcalRepeats += 1;
                boolean isRepeat = endcalRepeats > 1;
                // this doesn't rely on success rate, it is sent no matter what
                String endCalMessage = pickRandomEndCalMessage();
                logger.info(endCalMessage);
                if (!this.pauseMessages){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), endCalMessage);
                }

                // reset the counter for no endcals
                resetNoEndCal();

                // get last known endcal time (0 if none). If not zero check if it is from the same day. Update success rate
                long unixTSIfExists = Launcher.dbEngine.getEndCalTime(participantMap.get("participant_uuid"));

                // update endcal time in state_log
                // if incoming map is null it is being manually moved from website
                if (incomingMap == null) {
                    if (unixTSIfExists == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
                    } else {
                        unixTS = unixTSIfExists;
                    }
                } else {
                    String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                    String[] endCalSplit = textBody.split(" ", 2);
                    if (endCalSplit.length >= 2){
                        unixTS = TZHelper.parseTime(endCalSplit[1]);
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
                    if (!this.pauseMessages && !this.isDayOff){
                        // update the success rate
                        if (this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD) {
                            Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false, isRepeat);
                        }

                        String before9Msg = pickRandomLess9TRE(startTime, endTime);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), before9Msg);
                    }
                } else if (validTRE == 1) {
                    if (!this.pauseMessages && !this.isDayOff){
                        // update the success rate
                        if (this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD) {
                            Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false, isRepeat);
                        }

                        String after11Msg = pickRandomGreater11TRE();
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), after11Msg);
                    }
                } else {
                    if (!this.pauseMessages){
                        // update the success rate
                        if(TZHelper.isAfter8PM(endTime) && !this.isDayOff){
                            if (this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD) {
                                Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false, isRepeat);
                            }
                            String after8PMMsg = randomAfter8PMMessage();
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), after8PMMsg);
                            if (this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD) {
                                String neutralMsg = pickNeutralTRE();
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), neutralMsg);
                            }
                        } else{
                            if (!this.isDayOff && this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD) {
                                Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), true, isRepeat);

                                this.wasSucessfulFast = true;
                                if (this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD) {
                                    String successMsg = pickRandomSuccessTRE();
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), successMsg);
                                }
                            }
                        }
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
                    if (!this.pauseMessages && !this.isDayOff){
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), missed2EndCals);
                    }
                    resetNoEndCal();
                }
                if (!this.pauseMessages && !this.isDayOff && this.numberOfCyclesInProtocol >= this.TRIAL_PERIOD){
                    Launcher.dbEngine.setSuccessRate(participantMap.get("participant_uuid"), false, false);
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
                long endcalTime = Launcher.dbEngine.getEndCalTime(participantMap.get("participant_uuid"));
                boolean didSendEndCalToday = TZHelper.isSameDay(endcalTime);
                if (!didSendEndCalToday && this.isDayOff){
                    // if user didnt send endcal and dayoff is true text Matt
                    String dayOffMissedEndCalMsg = "Participant " + participantMap.get("first_name") + " " + participantMap.get("last_name") + " ("+participantMap.get("number")+") missed endcal and is on dayoff. If they send endcal in the next cycle it will count towards their success rate.";
                    Launcher.msgUtils.sendMessage("+12704022214", dayOffMissedEndCalMsg);
                }
                break;
            case resetEpisodeVariables:
                this.endcalRepeats = 0;
                this.isDayOff = false;
                this.wasSucessfulFast = false;
                break;
            case dayOffWait:
                this.isDayOff = true;
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"STARTCAL\" and \"ENDCAL\" today.");
                logger.info(participantMap.get("participant_uuid") + " DayOff in waitStart");
                break;
            case dayOffWarn:
                this.isDayOff = true;
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"STARTCAL\" and \"ENDCAL\" today.");
                logger.info(participantMap.get("participant_uuid") + " DayOff in warnStart");
                break;
            case dayOffStartCal:
                this.isDayOff = true;
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"ENDCAL\" today.");
                logger.info(participantMap.get("participant_uuid") + " DayOff in StartCal");
                break;
            case dayOffWarnEndCal:
                this.isDayOff = true;
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"ENDCAL\" today.");
                logger.info(participantMap.get("participant_uuid") + " DayOff in WarnEndCal");
                break;
            case dayOffEndOfEpisode:
                if (this.isDayOff) {
                    logger.warn(participantMap.get("participant_uuid") + " REPEATED DayOff in endOfEpisode");
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "You have already sent \"DAYOFF\" for today.");
                    break;
                }
                this.isDayOff = true;
                // check if endcal was successful or not, variable that is reset
                String updatedPercentage = Launcher.dbEngine.updateSuccessRate(participantMap.get("participant_uuid"), this.wasSucessfulFast);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. You're success rate is now: " + updatedPercentage);
                logger.info(participantMap.get("participant_uuid") + " DayOff in endOfEpisode");
                break;
            case endProtocol:
                logger.warn(participantMap.get("participant_uuid") + " is not longer in protocol.");
                break;
            default:
                logger.error("stateNotify: Invalid state: " + state);
        }

        return true;
    }

    public void restoreSaveState(boolean isReset) {
        try{
            String saveStateJSON = Launcher.dbEngine.getSaveState(participantMap.get("participant_uuid"));

            if (isReset) {
                this.isReset = true;
                logger.info("restoreSaveState: resetting participant: " + participantMap.get("participant_uuid"));
                int startWarnDiff =  TZHelper.getSecondsTo1159am();
                if(startWarnDiff <= 0) {
                    startWarnDiff = 300;
                }
                setStartWarnDeadline(startWarnDiff);
                receivedWaitStart(); // initial to waitStart
                this.isReset = false;
            }
            else {
                if (!saveStateJSON.equals("")) {
                    Map<String, Map<String, Long>> saveStateMap = gson.fromJson(saveStateJSON, typeOfHashMap);

                    Map<String, Long> historyMap = saveStateMap.get("history");
                    Map<String, Long> timerMap = saveStateMap.get("timers");

                    int stateIndex = (int) timerMap.get("stateIndex").longValue();
                    String stateName = State.values()[stateIndex].toString();

                    long saveCurrentTime = timerMap.get("currentTime");

                    // if same day (<4am) below is correct
                    // if next day (>=4am) need to reset to waitStart

                    // if past 4am, reset everything to beginning
                    boolean isSameDay = TZHelper.isSameDay(saveCurrentTime);
                    if (!isSameDay) {
                        // if state is endProtocol, do not restart cycle
                        if (!stateName.equals("endProtocol")) {
                            stateName = "waitStart";
                        }
                        this.isDayOff = false;
                        this.endcalRepeats = 0;
                    } else {
                        String lastDayOffString = Launcher.dbEngine.getLastDayOff(participantMap.get("participant_uuid"));
                        if (!lastDayOffString.equals("")) {
                            this.isDayOff = TZHelper.isSameDay(TZHelper.parseSQLTimestamp(lastDayOffString));
                        } else {
                            this.isDayOff = false;
                        }
                        try {
                            this.endcalRepeats = historyMap.get("endcalCount").intValue();
                        } catch (NullPointerException npex) {
                            this.endcalRepeats = 0;
                        }
                    }

                    switch (State.valueOf(stateName)) {
                        case initial:
                        case missedStartCal:
                        case endcal:
                        case endProtocol:
                        case missedEndCal:
                        case resetEpisodeVariables:
                        case dayOffStartCal:
                        case dayOffWait:
                        case dayOffWarn:
                        case dayOffWarnEndCal:
                        case dayOffEndOfEpisode:
                            //no timers
                            break;
                        case waitStart:
                            //resetting warn timer
                            int startWarnDiff = TZHelper.getSecondsTo1159am();  //timeToD1T1159am();
                            if (startWarnDiff <= 0) {
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
                } else {
                    logger.info("restoreSaveState: no save state found for " + participantMap.get("participant_uuid"));
                    int startWarnDiff = TZHelper.getSecondsTo1159am();  //timeToD1T1159am();
                    if (startWarnDiff <= 0) {
                        startWarnDiff = 300;
                    }
                    setStartWarnDeadline(startWarnDiff);
                    receivedWaitStart(); // initial to waitStart
                }
            }

        } catch (Exception ex) {
            logger.error("restoreSaveState");
            ex.printStackTrace();
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
            messageMap.put("protocol", "TRE");
            if (this.pauseMessages) {
                messageMap.put("restored", "true");
            }
            if (this.isReset) {
                messageMap.put("RESET", "true");
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
        int rnd = new Random().nextInt(endCalMessages.size());
        String message = endCalMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        return message;
    }

    public String pickNeutralTRE(){
        // this is a list of responses for when a participant sends endcal
        final List<String> neutralMessages = Collections.unmodifiableList(
                new ArrayList<String>() {{
                    add("Your overall success rate is now [SUCCESS].");
                }});
        int rnd = new Random().nextInt(neutralMessages.size());
        String message = neutralMessages.get(rnd);
        if (message.contains("[SUCCESS]")) {
            String successRate = Launcher.dbEngine.getSuccessRate(participantMap.get("participant_uuid"));
            if (successRate.equals("")) {
                if (this.isDayOff){
                    successRate = "100.00%";
                } else {
                    successRate = "0.00%";
                }
            }
            message = message.replace("[SUCCESS]", successRate);
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
        int rnd = new Random().nextInt(successMessages.size());
        String message = successMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        if (message.contains("[SUCCESS]")) {
            String successRate = Launcher.dbEngine.getSuccessRate(participantMap.get("participant_uuid"));
            if (successRate.equals("")) {
                if (this.isDayOff){
                    successRate = "100.00%";
                } else {
                    successRate = "0.00%";
                }
            }
            message = message.replace("[SUCCESS]", successRate);
        }
        return message;
    }

    public String pickRandomLess9TRE(long startTime, long endTime){
        // this is a list of responses for when a participant sends endcal
        final List<String> successMessages;
        if (this.numberOfCyclesInProtocol < this.TRIAL_PERIOD) {
            successMessages = Collections.unmodifiableList(
                    new ArrayList<String>() {{
                        add("Oops, you ended your time-restricted eating [SHORT] too short. Try planning ahead for how you will end your TRE.");
                        add("Oops, you ended your time-restricted eating too short! Have small snacks or meals ready so you can stay on time.");
                        add("Oops, [NAME], you ended your time-restricted eating too short! If you need help, get a family member to help you end your fasting time!");
                        add("Oops, you ended your time-restricted eating too short. Try putting Post-Its on your fridge and cupboards to help you remember your target End Calories time!");
                    }}
            );
        } else {
            successMessages = Collections.unmodifiableList(
                    new ArrayList<String>() {{
                        add("Oops, you ended your time-restricted eating [SHORT] too short. Your success rate is now [SUCCESS]. Try planning ahead for how you will end your TRE.");
                        add("Oops, you ended your time-restricted eating too short! [NAME], your success rate is now [SUCCESS]. Have small snacks or meals ready so you can stay on time.");
                        add("Oops, [NAME], you ended your time-restricted eating too short! Your success rate is now [SUCCESS]. If you need help, get a family member to help you end your fasting time!");
                        add("Oops, you ended your time-restricted eating too short. Your success rate is now [SUCCESS]. Try putting Post-Its on your fridge and cupboards to help you remember your target End Calories time!");
                    }}
            );
        }
        int rnd = new Random().nextInt(successMessages.size());
        String message = successMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        if (message.contains("[SUCCESS]")) {
            String successRate = Launcher.dbEngine.getSuccessRate(participantMap.get("participant_uuid"));
            if (successRate.equals("")) {
                if (this.isDayOff){
                    successRate = "100.00%";
                } else {
                    successRate = "0.00%";
                }
            }
            message = message.replace("[SUCCESS]", successRate);

        }
        if (message.contains("[SHORT]")) {
            String shortTime = TZHelper.getHoursMinutesBefore(startTime, endTime, 36000L); // 36000s = 10 hours
            message = message.replace("[SHORT]", shortTime);
        }
        return message;
    }

    public String pickRandomGreater11TRE(){
        // this is a list of responses for when a participant sends endcal
        final List<String> successMessages;
        if (this.numberOfCyclesInProtocol < this.TRIAL_PERIOD) {
            successMessages = Collections.unmodifiableList(
                    new ArrayList<String>() {{
                        add("Oops, you ended your TRE too late. Try planning ahead for how you will end calories earlier.");
                        add("Oops, you slipped up and ended too late. Let's get back on track tomorrow!");
                        add("Oops, [NAME], you exceeded the target eating window! If you need help, get a family member to help you end your calories on time!");
                        add("Oops, you slipped up and consumed calories for too long. Let's get back on track tomorrow!");
                    }}
            );
        } else {
            successMessages = Collections.unmodifiableList(
                    new ArrayList<String>() {{
                        add("Oops, you ended your TRE too late. Your success rate is now [SUCCESS]. Try planning ahead for how you will end calories earlier.");
                        add("Oops, you slipped up and ended too late. Your success rate is now [SUCCESS]. Let's get back on track tomorrow!");
                        add("Oops, [NAME], you exceeded the target eating window! Your success rate is now [SUCCESS]. If you need help, get a family member to help you end your calories on time!");
                        add("Oops, you slipped up and consumed calories for too long. Your success rate is now [SUCCESS]. Let's get back on track tomorrow!");
                    }}
            );
        }
        int rnd = new Random().nextInt(successMessages.size());
        String message = successMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        if (message.contains("[SUCCESS]")) {
            String successRate = Launcher.dbEngine.getSuccessRate(participantMap.get("participant_uuid"));
            if (successRate.equals("")) {
                if (this.isDayOff){
                    successRate = "100.00%";
                } else {
                    successRate = "0.00%";
                }
            }
            message = message.replace("[SUCCESS]", successRate);
        }
        return message;
    }

    public String randomAfter8PMMessage(){
        // this is a list of responses for when a participant sends endcal
        final List<String> successMessages = Collections.unmodifiableList(
        new ArrayList<String>() {{
            add("Oops, you ended your time-restricted eating too late. Try to keep your calories before 8pm.");
            add("Oops, you ended your time-restricted eating after 8pm. Try planning ahead for how you will end your daily calories earlier.");
            add("Oops, you ended your time-restricted eating after 8pm. Try using a recurring alarm or a family member to help you end calories earlier.");
        }});
        int rnd = new Random().nextInt(successMessages.size());
        return successMessages.get(rnd);
    }

} // class
