package fasting.Protocols;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fasting.Launcher;
import fasting.TimeUtils.TimezoneHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

public class Control extends ControlBase {
    private Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    //id, participant_uuid, phone_number, participant_type
    private Map<String, String> participantMap;
    private Map<String,Long> stateMap;
    private long startTimestamp = 0;
    private TimezoneHelper TZHelper;
    private boolean pauseMessages;
    private boolean isDayOff;
    private boolean isFromYesterday = false;
    private Map<String,String> incomingMap;

    public String stateJSON;

    private Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(Control.class.getName());

    public Control(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();
        this.pauseMessages = false;
        this.isDayOff = false;

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());
        // restoreSaveState();

        new Thread(){
            public void run(){
                try {
                    while (!getState().toString().equals("endProtocol")) {

                        if(startTimestamp > 0) {
                            stateJSON = saveStateJSON();
                            boolean didUpload = Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                            if(!didUpload){
                                break;
                            }
                        }

                        Thread.sleep(900000); // 900000 = 15 mins

                    }
                } catch (Exception ex) {
                    logger.error("protocols.Control Thread");
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
                    if(isStartCal(incomingMap.get("Body"))) {
                        receivedStartCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case startcal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        receivedEndCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for " +
                                                                                    "the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case endcal:
                    String endCalMessage = participantMap.get("participant_uuid") + " endCal unexpected message";
                    logger.warn(endCalMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    break;
                case timeout24:
                    String timeoutMessage = participantMap.get("participant_uuid") + " timeout unexpected message";
                    logger.warn(timeoutMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
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
            timerMap.put("timeout24Hours", Long.valueOf(getTimeout24Hours()));

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
        long recentStartCalTime;

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
                int secondsStart = TZHelper.getSecondsTo359am();
                setTimeout24Hours(secondsStart);
                String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: timeout24 timeout " + TZHelper.getDateFromAddingSeconds(secondsStart);
                logger.info(startCalMessage);
                
                // update startcal time in state_log
                if (incomingMap == null) {
                    unixTS = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                    if (unixTS == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                } else {
                    String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                    String[] startCalSplit = textBody.split(" ");
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
            case endcal:
                resetNoEndCal();

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
                    String[] endCalSplit = textBody.split(" ");
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
                Launcher.dbEngine.addProtocolNameToLog("Control", participantMap.get("participant_uuid"));
                logger.warn(participantMap.get("participant_uuid") + " is not longer in protocol.");
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

                // historyMap not needed currently, but available
                //Map<String,Long> historyMap = saveStateMap.get("history");
                Map<String,Long> timerMap = saveStateMap.get("timers");

                int stateIndex = (int) timerMap.get("stateIndex").longValue();
                String stateName = State.values()[stateIndex].toString();

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
                    this.isDayOff = false;
                }

                switch (State.valueOf(stateName)) {
                    case initial:
                        //no timers
                        break;
                    case waitStart:
                        //resetting warn timer
                        int timeout24 =  TZHelper.getSecondsTo359am();  //timeToD1T1159am();
                        setTimeout24Hours(timeout24);
                        receivedWaitStart(); // initial to waitStart
                        break;
                    case startcal:
                        //reset endWarnDeadline

                        long unixTS = Launcher.dbEngine.getStartCalTime(participantMap.get("participant_uuid"));
                        if (unixTS == 0) {
                            unixTS = TZHelper.getUnixTimestampNow();
                        }
                        Launcher.dbEngine.saveStartCalTime(participantMap.get("participant_uuid"), unixTS);
                        receivedStartCal();
                        break;
                    case endcal:
                        //no timers
                        break;
                    case timeout24:
                        break;
                    case endProtocol:
                        //no timers
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
        }
    }

    private void resetNoEndCal(){
        Launcher.dbEngine.resetDaysWithoutEndCal(participantMap.get("participant_uuid"));
    }

    public void logState(String state) {
        if(gson != null) {
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            if (this.pauseMessages) {
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

} // class
