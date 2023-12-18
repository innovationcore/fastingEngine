package fasting.Protocols.Sleep;

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

public class Sleep extends SleepBase {
    private final Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();
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
    private static final Logger logger = LoggerFactory.getLogger(fasting.Protocols.Sleep.Sleep.class.getName());

    public Sleep(Map<String, String> participantMap) {
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
                logger.error("protocols.Sleep Thread");
                logger.error(ex.getMessage());
            }
        }, 30, 900, TimeUnit.SECONDS); //900 sec is 15 mins

    } //Sleep

    public void incomingText(Map<String,String> incomingMap) {
        this.incomingMap = incomingMap;
        try {
            State state = getState();
            switch (state) {
                case initial:
                    //no timers
                    break;
                case waitSleep:
                case warnSleep:
                    if(isSleep(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] sleepSplit = textBody.split(" ", 2);
                        if (sleepSplit.length >= 2) {
                            if (!(sleepSplit[1].toLowerCase().contains("a") || sleepSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep including \"am\" or \"pm\". For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                            if (!isSleep(sleepSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep. For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(sleepSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep. For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                        } else {
                            // if just sent sleep make sure its not sleep9:45 or something similar
                            if (sleepSplit[0].length() > 5){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep. For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                        }
                        receivedSleep();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"SLEEP\" before you begin trying to fall asleep;" +
                                " \"WAKE\" soon after you wake up in the morning.");
                    }
                    break;
                case sleep:
                    if (isSleep(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] sleepSplit = textBody.split(" ", 2);
                        if (sleepSplit.length >= 2) {
                            if (!(sleepSplit[1].toLowerCase().contains("a") || sleepSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep including \"am\" or \"pm\". For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                            if (!isSleep(sleepSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep. For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(sleepSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep. For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                        } else {
                            // if just sent sleep make sure its not sleep9:45 or something similar
                            if (sleepSplit[0].length() > 5){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your SLEEP time was not understood. Please send \"SLEEP\" again with the time you started falling asleep. For example, \"SLEEP 9:30 pm\".");
                                break;
                            }
                        }
                        receivedSleep();
                    } else if(isWake(incomingMap.get("Body"))) {
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] wakeSplit = textBody.split(" ", 2);
                        if (wakeSplit.length >= 2) {
                            if (!(wakeSplit[1].toLowerCase().contains("a") || wakeSplit[1].toLowerCase().contains("p"))){
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up including \"am\" or \"pm\". For example, \"WAKE 7:30 am\".");
                                break;
                            }
                            if (!isWake(wakeSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(wakeSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                break;
                            }
                        } else {
                            // if just sent wake make sure it's not wake9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("wake")){
                                if (wakeSplit[0].length() > 4){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                    break;
                                }
                            }
                        }
                        receivedWake();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"SLEEP\" before you begin trying to fall asleep;" +
                                " \"WAKE\" soon after you wake up in the morning.");
                    }
                    break;
                case warnWake:
                    if (isWake(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] wakeSplit = textBody.split(" ", 2);
                        if (wakeSplit.length >= 2) {
                            if (!(wakeSplit[1].toLowerCase().contains("a") || wakeSplit[1].toLowerCase().contains("p"))) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up including \"am\" or \"pm\". For example, \"WAKE 7:30 am\".");
                                break;
                            }
                            if (!isWake(wakeSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(wakeSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                break;
                            }
                        } else {
                            // if just sent wake make sure its not wake9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("wake")){
                                if (wakeSplit[0].length() > 4){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                    break;
                                }
                            }
                        }
                        receivedWake();
                    } else {
                        String wakeMessage = participantMap.get("participant_uuid") + " warnWake unexpected message";
                        logger.warn(wakeMessage);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    }
                    break;
                case wake:
                    if (isWake(incomingMap.get("Body"))){
                        String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                        String[] wakeSplit = textBody.split(" ", 2);
                        if (wakeSplit.length >= 2) {
                            if (!(wakeSplit[1].toLowerCase().contains("a") || wakeSplit[1].toLowerCase().contains("p"))) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up including \"am\" or \"pm\". For example, \"WAKE 7:30 am\".");
                                break;
                            }
                            if (!isWake(wakeSplit[0])) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                break;
                            }
                            long parsedTime = TZHelper.parseTime(wakeSplit[1]);
                            if (parsedTime == -1L) {
                                Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                break;
                            }
                        } else {
                            // if just sent wake make sure its not wake9:45 or something similar
                            if (incomingMap.get("Body").toLowerCase().contains("wake")){
                                if (wakeSplit[0].length() > 4){
                                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your WAKE time was not understood. Please send \"WAKE\" again with the time you woke up. For example, \"WAKE 7:30 am\".");
                                    break;
                                }
                            }
                        }
                        receivedWake();
                    } else {
                        String wakeMessage = participantMap.get("participant_uuid") + " wake unexpected message";
                        logger.warn(wakeMessage);
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    }
                    break;
                case timeout24:
                    String timeoutMessage = participantMap.get("participant_uuid") + " timeout24 unexpected message";
                    logger.warn(timeoutMessage);
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Text 270-402-2214 if you need help.");
                    break;
                case endProtocol:
                    Launcher.dbEngine.addProtocolNameToLog("Sleep", participantMap.get("participant_uuid"));
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

    private boolean isSleep(String messageBody) {
        boolean isSleep = false;
        try {
            isSleep = messageBody.toLowerCase().contains("sleep");

        } catch (Exception ex) {
            logger.error("isSleep()");
            logger.error(ex.getMessage());
        }
        return isSleep;
    }

    private boolean isWake(String messageBody) {
        boolean isWake = false;
        try {
            if(messageBody.toLowerCase().contains("wake")) {
                isWake = true;
            }
        } catch (Exception ex) {
            logger.error("isWake()");
            logger.error(ex.getMessage());
        }
        return isWake;
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
            case waitSleep:
                int seconds = TZHelper.getSecondsTo1159am();
                if (seconds <= 0) {
                    seconds = 300;
                }
                setSleepWarnDeadline(seconds);
                String waitStartMessage = participantMap.get("participant_uuid") + " created state machine: warnSleep timeout " + TZHelper.getDateFromAddingSeconds(seconds);
                logger.warn(waitStartMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case warnSleep:
                //set end for sleep
                setTimeout24Hours(TZHelper.getSecondsTo1pm());
                String warnStartMessage = "Remember to text \"SLEEP\" when your begin trying to fall asleep and \"WAKE\" when wake up in the morning. Thank you!";
                if (!this.isRestoring){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnStartMessage);
                }
                logger.warn(warnStartMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case sleep:
                int secondsStart = TZHelper.getSecondsTo1159am(); //noon
                if (secondsStart < 0){
                    secondsStart = 300;
                }
                setWakeWarnDeadline(secondsStart);
                String sleepMessage = participantMap.get("participant_uuid") + " thanks for sending sleep: warnWake timeout " + TZHelper.getDateFromAddingSeconds(secondsStart);
                logger.info(sleepMessage);

                // update sleep time in state_log
                if (incomingMap == null) {
                    unixTS = Launcher.dbEngine.getSleepTime(participantMap.get("participant_uuid"));
                    if (unixTS == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                } else {
                    String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                    String[] sleepSplit = textBody.split(" ", 2);
                    if (sleepSplit.length >= 2){
                        unixTS = TZHelper.parseTime(sleepSplit[1]);
                    } else {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                }

                Launcher.dbEngine.saveSleepTime(participantMap.get("participant_uuid"), unixTS);

                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case warnWake:
                //set end for wake
                setTimeout24Hours(TZHelper.getSecondsTo1pm());
                String warnWakeMessage = "Remember to let us know when you woke up today.";
                if (!this.isRestoring){
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), warnWakeMessage);
                }
                logger.warn(warnWakeMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case wake:
                int secondsEnd = TZHelper.getSecondsTo1pm();
                setTimeout24Hours(secondsEnd);
                String wakeMessage = participantMap.get("participant_uuid") + " thanks for sending wake: timeout24 timeout " + TZHelper.getDateFromAddingSeconds(secondsEnd);
                logger.info(wakeMessage);

                // update wake time in state_log
                if (incomingMap == null) {
                    unixTS = Launcher.dbEngine.getWakeTime(participantMap.get("participant_uuid"));
                    if (unixTS == 0) {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                } else {
                    String textBody = incomingMap.get("Body").trim(); // removes whitespace before and after
                    String[] wakeSplit = textBody.split(" ", 2);
                    if (wakeSplit.length >= 2){
                        unixTS = TZHelper.parseTime(wakeSplit[1]);
                    } else {
                        unixTS = TZHelper.getUnixTimestampNow();
                    }
                }
                // save wake time to state_log
                Launcher.dbEngine.saveWakeTime(participantMap.get("participant_uuid"), unixTS);

                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case timeout24:
                logger.warn(participantMap.get("participant_uuid") + " did not send sleep/wake in time.");
                Launcher.msgUtils.sendMessage(participantMap.get("number"), "We haven't heard from you in a " +
                        "while. Remember to text \"STARTCAL\" when your calories start in the morning and \"ENDCAL\" " +
                        "when your calories finish at night! Let us know if you need help.");
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
                receivedWaitSleep(); // initial to waitStart
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
                        case endProtocol:
                            // no timers
                            break;
                        case waitSleep:
                            this.isRestoring = true;
                            //resetting warn timer
                            setSleepWarnDeadline(TZHelper.getSecondsTo1159am());
                            receivedWaitSleep(); // initial to waitStart
                            this.isRestoring = false;
                            break;
                        case warnSleep:
                            this.isRestoring = true;
                            //resetting warn timer
                            setTimeout24Hours(TZHelper.getSecondsTo1pm());
                            receivedWarnSleep(); // initial to warnStart
                            this.isRestoring = false;
                            break;
                        case sleep:
                            this.isRestoring = true;
                            long unixTS = Launcher.dbEngine.getSleepTime(participantMap.get("participant_uuid"));
                            if (unixTS == 0) {
                                unixTS = TZHelper.getUnixTimestampNow();
                            }
                            Launcher.dbEngine.saveSleepTime(participantMap.get("participant_uuid"), unixTS);
                            receivedSleep();
                            this.isRestoring = false;
                            break;
                        case warnWake:
                            this.isRestoring = true;
                            //resetting warnEnd time
                            setTimeout24Hours(TZHelper.getSecondsTo1pm());
                            recievedWarnWake(); // initial to warnWake
                            this.isRestoring = false;
                            break;
                        case wake:
                            this.isRestoring = true;
                            // setting timeout24
                            setTimeout24Hours(TZHelper.getSecondsTo1pm());
                            receivedSleep();
                            receivedWake();
                            this.isRestoring = false;
                            break;
                        default:
                            logger.error("restoreSaveState: Invalid state: " + stateName);
                    }
                } else {
                    logger.info("restoreSaveState: no save state found for " + participantMap.get("participant_uuid"));
                    int timeout24 = TZHelper.getSecondsTo1pm();
                    setTimeout24Hours(timeout24);
                    receivedWaitSleep(); // initial to waitStart
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
            messageMap.put("protocol", "Sleep");
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

