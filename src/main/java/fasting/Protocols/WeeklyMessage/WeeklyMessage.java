package fasting.Protocols.WeeklyMessage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fasting.Launcher;
import fasting.TimeUtils.TimezoneHelper;
import fasting.Protocols.Control.Control;
import fasting.Protocols.Baseline.Baseline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

public class WeeklyMessage extends WeeklyMessageBase {
    private Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    private Map<String, String> participantMap;
    private Map<String,Long> stateMap;
    private long startTimestamp = 0;
    private TimezoneHelper TZHelper;
    private boolean isRestoring;
    private boolean isDayOff;
    private boolean isFromYesterday = false;

    public String stateJSON;

    private Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(WeeklyMessage.class.getName());

    public WeeklyMessage(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();
        this.isRestoring = false;
        this.isDayOff = false;

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());
        // restoreSaveState();

        new Thread(){
            public void run(){
                try {
                    while (!getState().toString().equals("endProtocol")) {

                        String currentTimezone = Launcher.dbEngine.getParticipantTimezone(participantMap.get("participant_uuid"));
                        if (!participantMap.get("time_zone").equals(currentTimezone) && !currentTimezone.equals("")){
                            participantMap.put("time_zone", currentTimezone);
                            TZHelper.setUserTimezone(currentTimezone);
                        }

                        Thread.sleep(900000); // 900000 = 15 mins

                    }
                } catch (Exception ex) {
                    logger.error("protocols.Baseline Thread");
                    logger.error(ex.getMessage());
                }
            }
        }.start();

    }

//    public String saveStateJSON() {
//        String stateJSON = null;
//        try {
//            Map<String,Long> timerMap = new HashMap<>();
//            timerMap.put("stateIndex", Long.valueOf(getState().ordinal()));
//            timerMap.put("startTime", startTimestamp);
//            timerMap.put("currentTime", System.currentTimeMillis() / 1000); //unix seconds
//            timerMap.put("timeout1Week", Long.valueOf(getTimeout1Week()));
//
//            Map<String,Map<String,Long>> stateSaveMap = new HashMap<>();
//            stateSaveMap.put("history",stateMap);
//            stateSaveMap.put("timers", timerMap);
//
//            stateJSON = gson.toJson(stateSaveMap);
//
//        } catch (Exception ex) {
//            logger.error("saveStateJSON");
//            logger.error(ex.getMessage());
//
//        }
//        return stateJSON;
//    }

    @Override
    public boolean stateNotify(String state){

        logState(state);
    
        if(stateMap != null) {
            stateMap.put(state, System.currentTimeMillis() / 1000);
        }
        if(startTimestamp == 0) {
            startTimestamp = System.currentTimeMillis() / 1000;
        }

        switch (State.valueOf(state)) {
            case initial:
                //no timers
                break;
            case waitWeek:
                // 1 week timer set
                int seconds = TZHelper.getSecondsToFridayNoon();
                setTimeout1Week(seconds);
                String waitWeekMessage = participantMap.get("participant_uuid") + " created state machine: waitWeek timeout " + TZHelper.getDateFromAddingSeconds(seconds);
                logger.info(waitWeekMessage);
                break;
            case sendWeeklyMessage:
                String weeklyMessage = "Thank you for continuing to send us your STARTCAL and ENDCAL each day. Keep up the great work!";
                logger.info(weeklyMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), weeklyMessage);
                // wait 5 seconds, so multiple messages don't get sent at the same time
                try { Thread.sleep(5000); } catch (InterruptedException e) { /* do nothing */ }
                break;
            case endProtocol:
                logger.warn(participantMap.get("participant_uuid") + " is not longer in protocol.");
                break;
            default:
                logger.error("stateNotify: Invalid state: " + state);
        }

        return true;
    }

    public void restoreSaveState() {
        try {
            this.isRestoring = true;
            // get current protocol
            String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantMap.get("participant_uuid"));

            if (protocolNameDB.equals("Baseline")) {
                Map<String, Baseline> baselineMap = Launcher.baselineWatcher.getBaselineMap();
                while (!baselineMap.containsKey(participantMap.get("participant_uuid"))) {
                    Thread.sleep(500);
                    baselineMap = Launcher.baselineWatcher.getBaselineMap();
                }

                String currentState = baselineMap.get(participantMap.get("participant_uuid")).getState().toString();
                if (!currentState.equals("endProtocol")) {
                    int seconds = TZHelper.getSecondsToFridayNoon();
                    setTimeout1Week(seconds);
                    receivedWaitWeek();
                }

            } else if (protocolNameDB.equals("Control")){
                Map<String, Control> controlMap = Launcher.controlWatcher.getControlMap();
                while (!controlMap.containsKey(participantMap.get("participant_uuid"))) {
                    Thread.sleep(500);
                    controlMap = Launcher.controlWatcher.getControlMap();
                }

                String currentState = controlMap.get(participantMap.get("participant_uuid")).getState().toString();
                if (!currentState.equals("endProtocol")) {
                    int seconds = TZHelper.getSecondsToFridayNoon();
                    setTimeout1Week(seconds);
                    receivedWaitWeek();
                }

            }
            this.isRestoring = false;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logState(String state) {
        if(gson != null) {
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            messageMap.put("protocol", "WeeklyMessage");
            if (this.isRestoring) {
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
