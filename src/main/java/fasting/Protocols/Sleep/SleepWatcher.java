package fasting.Protocols.Sleep;

import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SleepWatcher {
    private final Logger logger;
    private final ScheduledExecutorService checkTimer;
    private final AtomicBoolean lockSleep = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private final Map<String, Sleep> SleepMap;

    public SleepWatcher() {
        this.logger = LoggerFactory.getLogger(SleepWatcher.class);
        this.SleepMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startSleep(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void incomingText(String participantId, Map<String,String> incomingMap) {
        try {

            //From
            logger.info("Incoming number: " + incomingMap.get("From") + " parid: " + participantId);

            synchronized (lockSleep) {
                if(SleepMap.containsKey(participantId)) {
                    SleepMap.get(participantId).incomingText(incomingMap);
                }

            }

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("incomingText");
            logger.error(exceptionAsString);
        }
    }

    public String getValidNextStates(String partUUID){
        String validNextStates = "";

        try {
            String currentState = Launcher.dbEngine.getParticipantCurrentState(partUUID);

            switch (currentState){
                case "initial":
                    validNextStates = "waitSleep,warnSleep,sleep,warnWake,endProtocol";
                    break;
                case "waitSleep":
                    validNextStates = "warnSleep,sleep,endProtocol";
                    break;
                case "warnSleep":
                    validNextStates = "timeout24,sleep,endProtocol";
                    break;
                case "sleep":
                    validNextStates = "sleep,wake,warnWake,endProtocol";
                    break;
                case "warnWake":
                    validNextStates = "waitSleep,wake,endProtocol";
                    break;
                case "wake":
                    validNextStates = "wake,waitSleep,endProtocol";
                    break;
                case "timeout24":
                    validNextStates = "waitSleep";
                    break;
                case "endProtocol":
                    validNextStates = "";
                    break;
                default:
                    // not in any state?
                    break;
            }
        } catch (Exception ex){
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("getValidNextStates");
            logger.error(exceptionAsString);
        }

        return validNextStates;
    }



    public void resetStateMachine(String participantId){
        // Remove participant from protocol
        Sleep removed = SleepMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }

        //restart at beginning
        List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Baseline", "HPM");
        //Create person
        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, participantId);
        Sleep p0 = new Sleep(addMap);

        p0.restoreSaveState(true);

        synchronized (lockSleep) {
            SleepMap.put(participantId, p0);
        }
    }

    public void stopProtocolNow(String participantId) {
        Sleep removed = SleepMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }
    }

    public String moveToState(String participantId, String moveToState, String time) {
        String newState = "";
        try {
            Sleep participant = SleepMap.get(participantId);
            switch (participant.getState()){
                //waitSleep,warnSleep,sleep,warnWake,endProtocol
                case initial:
                    if (moveToState.equals("waitSleep")){
                        participant.receivedWaitSleep();
                        newState = "waitSleep";
                    } else if (moveToState.equals("warnSleep")) {
                        participant.receivedWarnSleep();
                        newState = "warnSleep";
                    } else if (moveToState.equals("sleep")) {
                        participant.receivedSleep();
                        newState = "sleep";
                    } else if (moveToState.equals("warnWake")) {
                        participant.recievedWarnWake();
                        newState = "warnWake";
                    } else if (moveToState.equals("endProtocol")) {
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "initial invalid";
                        break;
                    }
                    break;
                //warnSleep,sleep,endProtocol
                case waitSleep:
                    if (moveToState.equals("sleep")) {
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveSleepTimeCreateTemp(participantId, timestamp);
                        participant.receivedSleep();
                        Launcher.dbEngine.removeTempSleep(participantId);
                        newState = "sleep";
                    } else if (moveToState.equals("warnSleep")) {
                        participant.timeoutwaitSleepTowarnSleep();
                        newState = "warnSleep";
                    } else if (moveToState.equals("endProtocol")) {
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "waitSleep invalid";
                        break;
                    }
                    break;
                // timeout24,sleep,endProtocol
                case warnSleep:
                    if(moveToState.equals("timeout24")){
                        participant.timeoutwarnSleepTotimeout24();
                        newState = "timeout24";
                    } else if (moveToState.equals("sleep")) {
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveSleepTimeCreateTemp(participantId, timestamp);
                        participant.receivedSleep();
                        Launcher.dbEngine.removeTempSleep(participantId);
                        newState = "sleep";
                    } else if (moveToState.equals("endProtocol")){
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        newState = "warnSleep invalid";
                        // invalid state
                        break;
                    }
                    break;
                // sleep,wake,warnWake,endProtocol
                case sleep:
                    if (moveToState.equals("sleep")){
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveSleepTimeCreateTemp(participantId, timestamp);
                        participant.receivedSleep();
                        Launcher.dbEngine.removeTempSleep(participantId);
                        newState = "sleep";
                    } else if (moveToState.equals("wake")) {
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveWakeTimeCreateTemp(participantId, timestamp);
                        participant.receivedWake();
                        Launcher.dbEngine.removeTempWake(participantId);
                        newState = "wake";
                    } else if (moveToState.equals("warnWake")) {
                        participant.timeoutsleepTowarnWake();
                        newState = "warnWake";
                    } else if (moveToState.equals("endProtocol")) {
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        newState = "sleep invalid";
                        // invalid state
                        break;
                    }
                    break;
                // waitSleep,wake,endProtocol
                case warnWake:
                    if (moveToState.equals("waitSleep")){
                        participant.timeoutwarnWakeTowaitSleep();
                        newState = "waitSleep";
                    } else if (moveToState.equals("wake")){
                        participant.receivedWake();
                        newState = "wake";
                    } else if (moveToState.equals("endProtocol")){
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "warnWake invalid";
                        break;
                    }
                    // wake,waitSleep,endProtocol
                case wake:
                    if (moveToState.equals("wake")){
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveWakeTimeCreateTemp(participantId, timestamp);
                        participant.receivedWake();
                        Launcher.dbEngine.removeTempWake(participantId);
                        newState = "wake";
                    } else if (moveToState.equals("waitSleep")) {
                        participant.timeoutwakeTowaitSleep();
                        newState = "waitSleep";
                    } else if (moveToState.equals("endProtocol")) {
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "wake invalid";
                        break;
                    }
                    break;
                case timeout24:
                    if (moveToState.equals("waitSleep")) {
                        newState = "waitSleep";
                    } else {
                        newState = "timeout24 invalid";
                        break;
                    }
                case endProtocol:
                    // no states to move to
                    newState = "endProtocol invalid";
                    break;
                default:
                    // invalid currentState
                    newState = "default invalid";
                    break;
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("moveToState");
            logger.error(exceptionAsString);
        }
        return newState;
    }

    public void updateTimeZone(String participantId, String tz) {
        Sleep toUpdate = SleepMap.get(participantId);
        logger.warn(participantId + ": changed TZ from " + toUpdate.TZHelper.getUserTimezone() + " to " + tz);
        toUpdate.TZHelper.setUserTimezone(tz);
    }

    class startSleep extends TimerTask {
        private final Logger logger;
        public startSleep() {
            logger = LoggerFactory.getLogger(startSleep.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Baseline", "HPM");
                    Map<String, String> SleepUUIDs = new HashMap<>(); // this only stores the uuids from partMapList
                    List<String> participantsToAdd = new ArrayList<>();
                    List<String> participantsToRemove = new ArrayList<>();

                    for (Map<String, String> participantMap : participantMapList) {
                        SleepUUIDs.put(participantMap.get("participant_uuid"), "participant_uuid");
                    }

                    if (SleepUUIDs.size() > SleepMap.size()) {
                        participantsToAdd = getMissingKeys(SleepMap, SleepUUIDs);
                    } else if (SleepUUIDs.size() < SleepMap.size()) {
                        participantsToRemove = getMissingKeys(SleepMap, SleepUUIDs);
                    } else {
                        // otherwise check if participant needs to be added
                        if (!SleepUUIDs.keySet().equals(SleepMap.keySet())){
                            for (String key: SleepMap.keySet()){
                                if (!SleepUUIDs.containsKey(key)){
                                    participantsToRemove.add(key);
                                }
                            }
                            for (String key: SleepUUIDs.keySet()) {
                                if (!SleepMap.containsKey(key)) {
                                    participantsToAdd.add(key);
                                }
                            }
                        }
                    }

                    for (String toRemove: participantsToRemove) {
                        Sleep removed = SleepMap.remove(toRemove);
                        if (removed != null) {
                            removed.receivedEndProtocol();
                            removed.uploadSave.shutdownNow();
                            removed = null;
                            System.gc();
                        }
                    }

                    for (String toAdd: participantsToAdd) {
                        logger.info("Creating state machine for participant_uuid=" + toAdd);
                        //Create person
                        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, toAdd);
                        if (addMap.isEmpty()) { continue; }
                        Sleep p0 = new Sleep(addMap);

                        logger.info("Restoring State for participant_uuid=" + toAdd);
                        p0.restoreSaveState(false);

                        synchronized (lockSleep) {
                            SleepMap.put(toAdd, p0);
                        }
                    }
                }

            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                ex.printStackTrace();
                logger.error("startProtocols");
                logger.error(exceptionAsString);
            }

        }

    }

    class episodeReset extends TimerTask {
        private final Logger logger;
        public episodeReset() {
            logger = LoggerFactory.getLogger(episodeReset.class);
        }

        public void run() {
            try {
                logger.error("RESET!!");
                synchronized (lockEpisodeReset) {
                    synchronized (lockSleep) {
                        SleepMap.clear();
                    }
                }

            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                ex.printStackTrace();
                logger.error("episodeReset");
                logger.error(exceptionAsString);
            }

        }

    }

    public Map<String, Sleep> getSleepMap(){
        return this.SleepMap;
    }

    public List<String> getMissingKeys(Map<String, Sleep> map1, Map<String, String> map2) {
        List<String> keysNotInBoth = new ArrayList<>();
        for (String key : map1.keySet()) {
            if (!map2.containsKey(key)) {
                keysNotInBoth.add(key);
            }
        }
        for (String key : map2.keySet()) {
            if (!map1.containsKey(key)) {
                keysNotInBoth.add(key);
            }
        }
        return keysNotInBoth;
    }

    public Map<String, String> getHashMapByParticipantUUID(List<Map<String, String>> list, String participantUUID) {
        for (Map<String, String> map : list) {
            if (map.containsKey("participant_uuid") && map.get("participant_uuid").equals(participantUUID)) {
                return map;
            }
        }
        return new HashMap<>();
    }

} //class
