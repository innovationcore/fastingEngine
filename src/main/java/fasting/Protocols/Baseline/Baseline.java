package fasting.Protocols.Baseline;

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

public class Baseline extends BaselineBase {
    private final Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    //id, participant_uuid, phone_number, participant_type
    private final Map<String, String> participantMap;
    private final Map<String,Long> stateMap;
    private long startTimestamp = 0;
    public final TimezoneHelper TZHelper;
    private boolean isRestoring;
    private Map<String,String> incomingMap;
    public String stateJSON;
    private final Gson gson;
    public ScheduledExecutorService uploadSave;
    private static final Logger logger = LoggerFactory.getLogger(Baseline.class.getName());

    public Baseline(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();
        this.isRestoring = false;

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

    } //Baseline

    public void incomingText(Map<String,String> incomingMap) {
        this.incomingMap = incomingMap;
        try {
            State state = getState();
            switch (state) {
                case initial:
                    //no timers
                    break;
                case waitStart:
                    if(isStartCal(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] startCalSplit = textBody.split(" ", 2);
                        if (startCalSplit.length >= 2) {
                            long parsedTime = TZHelper.parseTime(startCalSplit[1]);
                            if (parsedTime == -1L) {
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
                    if (isStartCal(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] startCalSplit = textBody.split(" ", 2);
                        if (startCalSplit.length >= 2) {
                            long parsedTime = TZHelper.parseTime(startCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your STARTCAL time was not understood. Please send \"STARTCAL\" again with your starting time. For example, \"STARTCAL 7:30 am\".");
                                break;
                            }
                        }
                        receivedStartCal();
                    } else if(isEndCal(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        if (endCalSplit.length >= 2) {
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                        }
                        receivedEndCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case warnEndCal:
                    if (isEndCal(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        if (endCalSplit.length >= 2) {
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                        }
                        receivedEndCal();
                    } else {
                        String endCalMessage = participantMap.get("participant_uuid") + " warnEndCal unexpected message";
                        logger.warn(endCalMessage);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    }
                    break;
                case endcal:
                    if (isEndCal(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] endCalSplit = textBody.split(" ", 2);
                        if (endCalSplit.length >= 2) {
                            long parsedTime = TZHelper.parseTime(endCalSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your ENDCAL time was not understood. Please send \"ENDCAL\" again with your ending time. For example, \"ENDCAL 7:30 pm\".");
                                break;
                            }
                        }
                        receivedEndCal();
                    } else {
                        String endCalMessage = participantMap.get("participant_uuid") + " endCal unexpected message";
                        logger.warn(endCalMessage);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    }
                    break;
                case timeout24:
                    String timeoutMessage = participantMap.get("participant_uuid") + " timeout unexpected message";
                    logger.warn(timeoutMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
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
                //24 hour timer set
                int seconds = TZHelper.getSecondsTo359am();
                setTimeout24Hours(seconds);
                String waitStartMessage = participantMap.get("participant_uuid") + " created state machine: timeout24 timeout " + TZHelper.getDateFromAddingSeconds(seconds);
                logger.warn(waitStartMessage);
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
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage);
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
            case timeout24:
                logger.warn(participantMap.get("participant_uuid") + " did not send startcal/endcal in time.");
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "We haven't heard from you in a " +
                        "while. Remember to text \"STARTCAL\" when your calories start in the morning and \"ENDCAL\" " +
                        "when your calories finish at night! Let us know if you need help.");
                break;
            case endProtocol:
                Launcher.dbEngine.addProtocolNameToLog("Baseline", participantMap.get("participant_uuid"));
                logger.warn(participantMap.get("participant_uuid") + " is not longer in protocol.");
                break;
            default:
                logger.error("stateNotify: Invalid state: " + state);
        }

        return true;
    }

    public void restoreSaveState() {
        try{
            String saveStateJSON = Launcher.dbEngine.getSaveState(participantMap.get("participant_uuid"));

            if (!saveStateJSON.equals("")){
                Map<String, Map<String,Long>> saveStateMap = gson.fromJson(saveStateJSON,typeOfHashMap);

                // historyMap not needed currently, but available
                //Map<String,Long> historyMap = saveStateMap.get("history");
                Map<String,Long> timerMap = saveStateMap.get("timers");

                int stateIndex = (int) timerMap.get("stateIndex").longValue();
                String stateName = State.values()[stateIndex].toString(); // out of bounds

                long saveCurrentTime = timerMap.get("currentTime");

                // if same day (<4am) below is correct
                // if next day (>=4am) need to reset to waitStart

                // if past 4am, reset everything to beginning
                boolean isSameDay = TZHelper.isSameDay(saveCurrentTime);
                if (!isSameDay) {
                    // if state is endProtocol, do not restart cycle
                    if(!stateName.equals("endProtocol")) {
                        stateName = "waitStart";
                    }
                }

                switch (State.valueOf(stateName)) {
                    case initial:
                    case timeout24:
                    case endProtocol:
                        // no timers
                        break;
                    case waitStart:
                        this.isRestoring = true;
                        //resetting warn timer
                        setTimeout24Hours(TZHelper.getSecondsTo359am());
                        receivedWaitStart(); // initial to waitStart
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
            }
            else {
                logger.info("restoreSaveState: no save state found for " + participantMap.get("participant_uuid"));
                int timeout24 =  TZHelper.getSecondsTo359am();
                setTimeout24Hours(timeout24);
                receivedWaitStart(); // initial to waitStart
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
