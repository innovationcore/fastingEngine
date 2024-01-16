package fasting.Protocols.CCW.CCW_WeeklyMessage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fasting.Launcher;
import fasting.Protocols.CCW.CCW_Baseline.CCW_Baseline;
import fasting.Protocols.CCW.CCW_Control.CCW_Control;
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

public class CCW_WeeklyMessage extends CCW_WeeklyMessageBase {
    private Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    private Map<String, String> participantMap;
    private Map<String,Long> stateMap;
    private long startTimestamp = 0;
    public TimezoneHelper TZHelper;
    private boolean isRestoring;
    private boolean isReset;

    private Gson gson;
    public ScheduledExecutorService uploadSave;
    private static final Logger logger = LoggerFactory.getLogger(CCW_WeeklyMessage.class.getName());

    public CCW_WeeklyMessage(Map<String, String> participantMap) {
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

                    String currentTimezone = Launcher.dbEngine.getParticipantTimezone(participantMap.get("participant_uuid"));
                    if (!participantMap.get("time_zone").equals(currentTimezone) && !currentTimezone.equals("")){
                        participantMap.put("time_zone", currentTimezone);
                        TZHelper.setUserTimezone(currentTimezone);
                    }

                    Thread.sleep(900000); // 900000 = 15 mins

                }
            } catch (Exception ex) {
                logger.error("protocols.CCW_WeeklyMessage Thread");
                logger.error(ex.getMessage());
            }
        }, 30, 900, TimeUnit.SECONDS); //900 sec is 15 mins

    }

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
                int seconds = TZHelper.getSecondsToFriday5pm();
                setTimeout1Week(seconds);
                String waitWeekMessage = participantMap.get("participant_uuid") + " created state machine: waitWeek timeout " + TZHelper.getDateFromAddingSeconds(seconds);
                logger.info(waitWeekMessage);
                break;
            case sendWeeklyMessage:
                String weeklyMessage = "Thank you for continuing to send us your \"STARTCAL\" and \"ENDCAL\" each day. Keep up the great work!";
                logger.info(weeklyMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), weeklyMessage, false);
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

    public void restoreSaveState(boolean isReset) {
        try {
            this.isRestoring = true;
            // get current protocol
            String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantMap.get("participant_uuid"));

            if (protocolNameDB.equals("Baseline")) {
                Map<String, CCW_Baseline> baselineMap = Launcher.CCW_BaselineWatcher.getCCW_BaselineMap();
                while (!baselineMap.containsKey(participantMap.get("participant_uuid"))) {
                    Thread.sleep(500);
                    baselineMap = Launcher.CCW_BaselineWatcher.getCCW_BaselineMap();
                }

                String currentState = baselineMap.get(participantMap.get("participant_uuid")).getState().toString();
                if (isReset) {
                    this.isReset = true;
                    int seconds = TZHelper.getSecondsToFriday5pm();
                    setTimeout1Week(seconds);
                    receivedWaitWeek();
                    this.isReset = false;
                }
                else {
                    if (!currentState.equals("endProtocol")) {
                        int seconds = TZHelper.getSecondsToFriday5pm();
                        setTimeout1Week(seconds);
                        receivedWaitWeek();
                    }
                }

            } else if (protocolNameDB.equals("Control")){
                Map<String, CCW_Control> controlMap = Launcher.CCW_ControlWatcher.getCCW_ControlMap();
                while (!controlMap.containsKey(participantMap.get("participant_uuid"))) {
                    Thread.sleep(500);
                    controlMap = Launcher.CCW_ControlWatcher.getCCW_ControlMap();
                }

                String currentState = controlMap.get(participantMap.get("participant_uuid")).getState().toString();
                if (isReset) {
                    this.isReset = true;
                    int seconds = TZHelper.getSecondsToFriday5pm();
                    setTimeout1Week(seconds);
                    receivedWaitWeek();
                    this.isReset = false;
                }
                else {
                    if (!currentState.equals("endProtocol")) {
                        int seconds = TZHelper.getSecondsToFriday5pm();
                        setTimeout1Week(seconds);
                        receivedWaitWeek();
                    }
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
                if(this.isReset) {
                    messageMap.put("RESET", "true");
                } else {
                    messageMap.put("restored", "true");
                }
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
