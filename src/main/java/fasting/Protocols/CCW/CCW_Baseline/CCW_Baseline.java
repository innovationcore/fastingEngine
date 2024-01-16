package fasting.Protocols.CCW.CCW_Baseline;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fasting.Launcher;
import fasting.TimeUtils.TimezoneHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CCW_Baseline extends CCW_BaselineBase {
    private final Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    //id, participant_uuid, phone_number, participant_type
    private final Map<String, String> participantMap;
    private final Map<String,Long> stateMap;
    private long startTimestamp = 0;
    public final TimezoneHelper TZHelper;
    private boolean isRestoring;
    private boolean isReset;
    private Map<String,String> incomingMap;
    public String stateJSON;
    private final Gson gson;
    public ScheduledExecutorService uploadSave;
    private static final Logger logger = LoggerFactory.getLogger(CCW_Baseline.class.getName());

    public CCW_Baseline(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();
        this.isRestoring = false;
        this.isReset = false;

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());

        //create timer
        this.uploadSave = Executors.newScheduledThreadPool(1);
        //set timer
        this.uploadSave.scheduleAtFixedRate(() -> {
            try {
                if (!getState().toString().equals("endProtocol")) {

                    if(startTimestamp > 0) {
                        stateJSON = saveStateJSON();
                        boolean didUpload = Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                        if(!didUpload){
                            logger.error("saveState failed to upload for participant: " + participantMap.get("participant_uuid"));
                        }
                    }

                    String currentTimezone = Launcher.dbEngine.getParticipantTimezone(participantMap.get("participant_uuid"));
                    if (!participantMap.get("time_zone").equals(currentTimezone) && !currentTimezone.equals("")){
                        participantMap.put("time_zone", currentTimezone);
                        TZHelper.setUserTimezone(currentTimezone);
                    }
                }
            } catch (Exception ex) {
                logger.error("protocols.Baseline Thread");
                logger.error(ex.getMessage());
            }
        }, 30, 900, TimeUnit.SECONDS); //900 sec is 15 mins

    } //CCW_Baseline

    public void incomingText(Map<String,String> incomingMap) {
        this.incomingMap = incomingMap;
        try {
            State state = getState();
            switch (state) {
                case initial:
                    //no timers
                    break;
                case waitStart:
                case warnStartCal:
                    if(isStartCal(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] startCalSplit = textBody.split(" ", 2);
                        if (startCalSplit.length >= 2) {
                            if (!(startCalSplit[1].toLowerCase().contains("a") || startCalSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time including \"am\" or \"pm\". For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                            if (!isStartCal(startCalSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(startCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                        } else {
                            // if just sent startcal make sure its not startcal9:45 or something similar
                            if (startCalSplit[0].length() > 8){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                        }
                        receivedStartCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.", false);
                    }
                    break;
                case startcal:
                    if (isStartCal(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] startCalSplit = textBody.split(" ", 2);
                        if (startCalSplit.length >= 2) {
                            if (!(startCalSplit[1].toLowerCase().contains("a") || startCalSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                            if (!isStartCal(startCalSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(startCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                        } else {
                            // if just sent startcal make sure its not startcal9:45 or something similar
                            if (startCalSplit[0].length() > 8){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".", false);
                                break;
                            }
                        }
                        receivedStartCal();
                    } else if(isEndCal(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        if (endCalSplit.length >= 2) {
                            if (!(endCalSplit[1].toLowerCase().contains("a") || endCalSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time including \"am\" or \"pm\". For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                            if (!isEndCal(endCalSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                        } else {
                            // if just sent stopcal/endcal make sure its not endcal9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("stopcal")){
                                if (endCalSplit[0].length() > 7){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                    break;
                                }
                            } else if (incomingMap.get("Body").toLowerCase().contains("endcal")){
                                if (endCalSplit[0].length() > 6){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                    break;
                                }
                            }
                        }
                        receivedEndCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.", false);
                    }
                    break;
                case warnEndCal:
                    if (isEndCal(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        if (endCalSplit.length >= 2) {
                            if (!(endCalSplit[1].toLowerCase().contains("a") || endCalSplit[1].toLowerCase().contains("p"))) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time including \"am\" or \"pm\". For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                            if (!isEndCal(endCalSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                        } else {
                            // if just sent stopcal/endcal make sure its not endcal9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("stopcal")){
                                if (endCalSplit[0].length() > 7){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                    break;
                                }
                            } else if (incomingMap.get("Body").toLowerCase().contains("endcal")){
                                if (endCalSplit[0].length() > 6){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                    break;
                                }
                            }
                        }
                        receivedEndCal();
                    } else {
                        String endCalMessage = participantMap.get("participant_uuid") + " warnEndCal unexpected message";
                        logger.warn(endCalMessage);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.", false);
                    }
                    break;
                case endcal:
                    if (isEndCal(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        if (endCalSplit.length >= 2) {
                            if (!(endCalSplit[1].toLowerCase().contains("a") || endCalSplit[1].toLowerCase().contains("p"))) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time including \"am\" or \"pm\". For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                            if (!isEndCal(endCalSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                break;
                            }
                        } else {
                            // if just sent stopcal/endcal make sure its not endcal9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("stopcal")){
                                if (endCalSplit[0].length() > 7){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                    break;
                                }
                            } else if (incomingMap.get("Body").toLowerCase().contains("endcal")){
                                if (endCalSplit[0].length() > 6){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".", false);
                                    break;
                                }
                            }
                        }
                        receivedEndCal();
                    } else {
                        String endCalMessage = participantMap.get("participant_uuid") + " endCal unexpected message";
                        logger.warn(endCalMessage);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.", false);
                    }
                    break;
                case timeout24:
                case missedEndCal:
                    String timeoutMessage = participantMap.get("participant_uuid") + " timeout/missedEndCal unexpected message";
                    logger.warn(timeoutMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.", false);
                    break;
                case endProtocol:
                    Launcher.dbEngine.addProtocolNameToLog("Baseline", participantMap.get("participant_uuid"));
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

    private boolean isStartCal(String messageBody) {
        boolean isStart = false;
        try {
            isStart = messageBody.toLowerCase().contains("startcal");

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
            timerMap.put("timeout24Hours", (long) getTimeout24Hours());

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
                int seconds = TZHelper.getSecondsTo1159am();
                if (seconds <= 0) {
                    seconds = 300;
                }
                setStartWarnDeadline(seconds);
                String waitStartMessage = participantMap.get("participant_uuid") + " created state machine: warnStartCal timeout " + TZHelper.getDateFromAddingSeconds(seconds);
                logger.warn(waitStartMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case warnStartCal:
                //set end for startcal
                setTimeout24Hours(TZHelper.getSecondsTo359am());
                String warnStartMessage = "Remember to text \"STARTCAL\" when your calories start for the day and \"ENDCAL\" when your calories finish at night. Thank you!";
                if (!this.isRestoring){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnStartMessage, false);
                }
                logger.warn(warnStartMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case startcal:
                int secondsStart = TZHelper.getSecondsTo2059pm();
                if (secondsStart < 0){
                    secondsStart = 300;
                }
                setEndWarnDeadline(secondsStart);
                String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: warnEndCal timeout " + TZHelper.getDateFromAddingSeconds(secondsStart);
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
                break;
            case warnEndCal:
                //set end for endcal
                setTimeout24Hours(TZHelper.getSecondsTo359am());
                String warnEndCalMessage = "Remember to enter your \"ENDCAL\" tonight after your last calories. Thank you!";
                if (!this.isRestoring){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage, false);
                }
                logger.warn(warnEndCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case endcal:
                int secondsEnd = TZHelper.getSecondsTo359am();
                setTimeout24Hours(secondsEnd);
                String endCalMessage = participantMap.get("participant_uuid") + " thanks for sending endcal: timeout24 timeout " + TZHelper.getDateFromAddingSeconds(secondsEnd);
                logger.info(endCalMessage);

                // update endcal time in state_log
                if (incomingMap == null) {
                    unixTS = Launcher.dbEngine.getEndCalTime(participantMap.get("participant_uuid"));
                    if (unixTS == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
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

                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case missedEndCal:
                logger.warn(participantMap.get("participant_uuid") + " did not send endcal in time. (missedEndCal)");
                if (!isRestoring) {
                    Launcher.msgUtils.sendScheduledMessage(participantMap.get("number"), "[CCW Baseline] Participant " + participantMap.get("first_name") + " " + participantMap.get("last_name") + " ("+participantMap.get("number")+") missed their ENDCAL.", TZHelper.getZonedDateTime8am(true), true);
                }
            case timeout24:
                logger.warn(participantMap.get("participant_uuid") + " did not send startcal/endcal in time.");
                if(!isRestoring) {
                    String message = "We haven't heard from you in a while. Remember to text \"STARTCAL\" when your " +
                            "calories start in the morning and \"ENDCAL\" when your calories finish at night! Let us " +
                            "know if you need help.";
                    Launcher.msgUtils.sendScheduledMessage(participantMap.get("number"), message, TZHelper.getZonedDateTime8am(false), false);
                    Launcher.msgUtils.sendScheduledMessage(participantMap.get("number"), "[CCW Baseline] Participant " + participantMap.get("first_name") + " " + participantMap.get("last_name") + " ("+participantMap.get("number")+") did not send STARTCAL or ENDCAL yesterday.", TZHelper.getZonedDateTime8am(true), true);
                }
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
                receivedWaitStart(); // initial to waitStart
                this.isReset = false;
            }
            else {
                if (!saveStateJSON.equals("")) {
                    Map<String, Map<String, Long>> saveStateMap = gson.fromJson(saveStateJSON, typeOfHashMap);

                    // historyMap not needed currently, but available
                    //Map<String,Long> historyMap = saveStateMap.get("history");
                    Map<String, Long> timerMap = saveStateMap.get("timers");

                    int stateIndex = (int) timerMap.get("stateIndex").longValue();
                    String stateName = State.values()[stateIndex].toString(); // out of bounds

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
                    }

                    switch (State.valueOf(stateName)) {
                        case initial:
                        case timeout24:
                        case missedEndCal:
                        case endProtocol:
                            // no timers
                            break;
                        case waitStart:
                            this.isRestoring = true;
                            //resetting warn timer
                            setStartWarnDeadline(TZHelper.getSecondsTo1159am());
                            receivedWaitStart(); // initial to waitStart
                            this.isRestoring = false;
                            break;
                        case warnStartCal:
                            this.isRestoring = true;
                            //resetting warn timer
                            setTimeout24Hours(TZHelper.getSecondsTo359am());
                            receivedWarnStart(); // initial to warnStart
                            this.isRestoring = false;
                            break;
                        case startcal:
                            this.isRestoring = true;
                            long unixTS = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                            if (unixTS == 0) {
                                unixTS = TZHelper.getUnixTimestampNow();
                            }
                            Launcher.dbEngine.saveStartCalTime(participantMap.get("participant_uuid"), unixTS);
                            receivedStartCal();
                            setEndWarnDeadline(TZHelper.getSecondsTo2059pm());
                            this.isRestoring = false;
                            break;
                        case warnEndCal:
                            this.isRestoring = true;
                            //resetting warnEnd time
                            setTimeout24Hours(TZHelper.getSecondsTo359am());
                            recievedWarnEndCal(); // initial to warnEndCal
                            this.isRestoring = false;
                            break;
                        case endcal:
                            this.isRestoring = true;
                            // setting timeout24
                            setTimeout24Hours(TZHelper.getSecondsTo359am());
                            receivedStartCal();
                            receivedEndCal();
                            this.isRestoring = false;
                            break;
                        default:
                            logger.error("restoreSaveState: Invalid state: " + stateName);
                    }
                } else {
                    logger.info("restoreSaveState: no save state found for " + participantMap.get("participant_uuid"));
                    int timeout24 = TZHelper.getSecondsTo359am();
                    setTimeout24Hours(timeout24);
                    receivedWaitStart(); // initial to waitStart
                }
            }

        } catch (Exception ex) {
            logger.error("restoreSaveState");
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void logState(String state) {
        if(gson != null) {
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            messageMap.put("protocol", "Baseline");
            if (this.isRestoring) {
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

} // class
