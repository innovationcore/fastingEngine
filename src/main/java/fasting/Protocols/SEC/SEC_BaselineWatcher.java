package fasting.Protocols.SEC;

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

public class SEC_BaselineWatcher {
    private final Logger logger;
    private final ScheduledExecutorService checkTimer;
    private final AtomicBoolean lockSEC_Baseline = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private final Map<String, SEC_Baseline> SEC_baselineMap;

    public SEC_BaselineWatcher() {
        this.logger = LoggerFactory.getLogger(SEC_BaselineWatcher.class);
        this.SEC_baselineMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startSEC_Baseline(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void incomingText(String participantId, Map<String,String> incomingMap) {
        try {

            //From
            logger.info("Incoming number: " + incomingMap.get("From") + " parid: " + participantId);

            synchronized (lockSEC_Baseline) {
                if(SEC_baselineMap.containsKey(participantId)) {
                    SEC_baselineMap.get(participantId).incomingText(incomingMap);
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
                    validNextStates = "waitStart,warnStartCal,startcal,warnEndCal,endProtocol";
                    break;
                case "waitStart":
                    validNextStates = "warnStartCal,startcal,endProtocol";
                    break;
                case "warnStartCal":
                    validNextStates = "timeout24,startcal,endProtocol";
                    break;
                case "startcal":
                    validNextStates = "startcal,endcal,warnEndCal,endProtocol";
                    break;
                case "warnEndCal":
                    validNextStates = "missedEndCal,endcal,endProtocol";
                    break;
                case "endcal":
                    validNextStates = "endcal,waitStart,endProtocol";
                    break;
                case "timeout24":
                    validNextStates = "waitStart";
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
        SEC_Baseline removed = SEC_baselineMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }

        //restart at beginning
        List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Baseline", "SEC");
        //Create person
        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, participantId);
        SEC_Baseline p0 = new SEC_Baseline(addMap);

        p0.restoreSaveState(true);

        synchronized (lockSEC_Baseline) {
            SEC_baselineMap.put(participantId, p0);
        }
    }

    public void stopProtocolNow(String participantId) {
        SEC_Baseline removed = SEC_baselineMap.remove(participantId);
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
            SEC_Baseline participant = SEC_baselineMap.get(participantId);
            switch (participant.getState()){
                //waitStart,warnStartCal,startcal,warnEndCal,endProtocol
                case initial:
                    if (moveToState.equals("waitStart")){
                        participant.receivedWaitStart();
                        newState = "waitStart";
                    } else if (moveToState.equals("warnStartCal")) {
                        participant.receivedWarnStart();
                        newState = "warnStartCal";
                    } else if (moveToState.equals("startcal")) {
                        participant.receivedStartCal();
                        newState = "startcal";
                    } else if (moveToState.equals("warnEndCal")) {
                        participant.recievedWarnEndCal();
                        newState = "warnEndCal";
                    } else if (moveToState.equals("endProtocol")) {
                        //stopProtocolNow(participantId);
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "initial invalid";
                    }
                    break;
                //warnStartCal,startcal,endProtocol
                case waitStart:
                    if (moveToState.equals("startcal")) {
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveStartCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedStartCal();
                        Launcher.dbEngine.removeTempStartCal(participantId);
                        newState = "startcal";
                    } else if (moveToState.equals("warnStartCal")) {
                        participant.timeoutwaitStartTowarnStartCal();
                        newState = "warnStartCal";
                    } else if (moveToState.equals("endProtocol")) {
                        //stopProtocolNow(participantId);
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "waitstart invalid";
                    }
                    break;
                // timeout24,startcal,endProtocol
                case warnStartCal:
                    if(moveToState.equals("timeout24")){
                        participant.timeoutwarnStartCalTotimeout24();
                        newState = "timeout24";
                    } else if (moveToState.equals("startcal")) {
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveStartCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedStartCal();
                        Launcher.dbEngine.removeTempStartCal(participantId);
                        newState = "startcal";
                    } else if (moveToState.equals("endProtocol")){
                        //stopProtocolNow(participantId);
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        newState = "warnStartCal invalid";
                        // invalid state
                    }
                    break;
                // startcal,endcal,warnEndCal,endProtocol
                case startcal:
                    if (moveToState.equals("startcal")){
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveStartCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedStartCal();
                        Launcher.dbEngine.removeTempStartCal(participantId);
                        newState = "startcal";
                    } else if (moveToState.equals("endcal")) {
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveEndCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedEndCal();
                        Launcher.dbEngine.removeTempEndCal(participantId);
                        newState = "endcal";
                    } else if (moveToState.equals("warnEndCal")) {
                        participant.timeoutstartcalTowarnEndCal();
                        newState = "warnEndCal";
                    } else if (moveToState.equals("endProtocol")) {
                        //stopProtocolNow(participantId);
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        newState = "startcal invalid";
                        // invalid state
                    }
                    break;
                // missedEndCal,endcal,endProtocol
                case warnEndCal:
                    if (moveToState.equals("missedEndCal")){
                        participant.timeoutwarnEndCalTomissedEndCal();
                        newState = "missedEndCal";
                    } else if (moveToState.equals("endcal")){
                        participant.receivedEndCal();
                        newState = "endcal";
                    } else if (moveToState.equals("endProtocol")){
                        //stopProtocolNow(participantId);
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "warnEndCal invalid";
                    }
                    break;
                // endcal,waitStart,endProtocol
                case endcal:
                    if (moveToState.equals("endcal")){
                        long timestamp = participant.TZHelper.parseTimeWebsite(time);
                        Launcher.dbEngine.saveEndCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedEndCal();
                        Launcher.dbEngine.removeTempEndCal(participantId);
                        newState = "endcal";
                    } else if (moveToState.equals("waitStart")) {
                        participant.timeoutendcalTowaitStart();
                        newState = "waitStart";
                    } else if (moveToState.equals("endProtocol")) {
                        //stopProtocolNow(participantId);
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "endcal invalid";
                    }
                    break;
                case timeout24:
                    if (moveToState.equals("waitStart")) {
                        newState = "waitStart";
                    } else {
                        newState = "timeout24 invalid";
                    }
                    break;
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
        SEC_Baseline toUpdate = SEC_baselineMap.get(participantId);
        logger.warn(participantId + ": changed TZ from " + toUpdate.TZHelper.getUserTimezone() + " to " + tz);
        toUpdate.TZHelper.setUserTimezone(tz);
    }

    class startSEC_Baseline extends TimerTask {
        private final Logger logger;
        public startSEC_Baseline() {
            logger = LoggerFactory.getLogger(startSEC_Baseline.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Baseline", "SEC");
                    Map<String, String> SEC_baselineUUIDs = new HashMap<>(); // this only stores the uuids from partMapList
                    List<String> participantsToAdd = new ArrayList<>();
                    List<String> participantsToRemove = new ArrayList<>();

                    for (Map<String, String> participantMap : participantMapList) {
                        SEC_baselineUUIDs.put(participantMap.get("participant_uuid"), "participant_uuid");
                    }

                    if (SEC_baselineUUIDs.size() > SEC_baselineMap.size()) {
                        participantsToAdd = getMissingKeys(SEC_baselineMap, SEC_baselineUUIDs);
                    } else if (SEC_baselineUUIDs.size() < SEC_baselineMap.size()) {
                        participantsToRemove = getMissingKeys(SEC_baselineMap, SEC_baselineUUIDs);
                    } else {
                        // otherwise check if participant needs to be added
                        if (!SEC_baselineUUIDs.keySet().equals(SEC_baselineMap.keySet())){
                            for (String key: SEC_baselineMap.keySet()){
                                if (!SEC_baselineUUIDs.containsKey(key)){
                                    participantsToRemove.add(key);
                                }
                            }
                            for (String key: SEC_baselineUUIDs.keySet()) {
                                if (!SEC_baselineMap.containsKey(key)) {
                                    participantsToAdd.add(key);
                                }
                            }
                        }
                    }

                    for (String toRemove: participantsToRemove) {
                        SEC_Baseline removed = SEC_baselineMap.remove(toRemove);
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
                        SEC_Baseline p0 = new SEC_Baseline(addMap);

                        logger.info("Restoring State for participant_uuid=" + toAdd);
                        p0.restoreSaveState(false);

                        synchronized (lockSEC_Baseline) {
                            SEC_baselineMap.put(toAdd, p0);
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
                    synchronized (lockSEC_Baseline) {
                        SEC_baselineMap.clear();
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

    public Map<String, SEC_Baseline> getSEC_BaselineMap(){
        return this.SEC_baselineMap;
    }

    public List<String> getMissingKeys(Map<String, SEC_Baseline> map1, Map<String, String> map2) {
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
