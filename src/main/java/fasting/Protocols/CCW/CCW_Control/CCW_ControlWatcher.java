package fasting.Protocols.CCW.CCW_Control;

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

public class CCW_ControlWatcher {
    private final Logger logger;
    private final ScheduledExecutorService checkTimer;
    private final AtomicBoolean lockCCW_Control = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private final Map<String, CCW_Control> CCW_controlMap;

    public CCW_ControlWatcher() {
        this.logger = LoggerFactory.getLogger(CCW_ControlWatcher.class);
        this.CCW_controlMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startCCW_Control(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void incomingText(String participantId, Map<String,String> incomingMap) {
        try {

            //From
            logger.info("Incoming number: " + incomingMap.get("From") + " parid: " + participantId);

            synchronized (lockCCW_Control) {
                if(CCW_controlMap.containsKey(participantId)) {
                    CCW_controlMap.get(participantId).incomingText(incomingMap);
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
                    validNextStates = "waitStart,endcal,endProtocol";
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
        CCW_Control removed = CCW_controlMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }

        //restart at beginning
        List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Control", "CCW");
        //Create person
        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, participantId);
        CCW_Control p0 = new CCW_Control(addMap);

        p0.restoreSaveState(true);

        synchronized (lockCCW_Control) {
            CCW_controlMap.put(participantId, p0);
        }
    }

    public String moveToState(String participantId, String moveToState, String time) {
        String newState = "";
        try {
            CCW_Control participant = CCW_controlMap.get(participantId);
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
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "initial invalid";
                        break;
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
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "waitstart invalid";
                        break;
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
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        newState = "warnStartCal invalid";
                        // invalid state
                        break;
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
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        newState = "startcal invalid";
                        // invalid state
                        break;
                    }
                    break;
                // waitStart,endcal,endProtocol
                case warnEndCal:
                    if (moveToState.equals("waitStart")){
                        participant.timeoutwarnEndCalTowaitStart();
                        newState = "waitStart";
                    } else if (moveToState.equals("endcal")){
                        participant.receivedEndCal();
                        newState = "endcal";
                    } else if (moveToState.equals("endProtocol")){
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "warnEndCal invalid";
                        break;
                    }
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
                    } else {
                        // invalid state
                        newState = "endcal invalid";
                        break;
                    }
                    break;
                case timeout24:
                    if (moveToState.equals("waitStart")) {
                        newState = "waitStart";
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
        CCW_Control toUpdate = CCW_controlMap.get(participantId);
        logger.warn(participantId + ": changed TZ from " + toUpdate.TZHelper.getUserTimezone() + " to " + tz);
        toUpdate.TZHelper.setUserTimezone(tz);
    }

    class startCCW_Control extends TimerTask {
        private final Logger logger;
        public startCCW_Control() {
            logger = LoggerFactory.getLogger(startCCW_Control.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Control", "CCW");
                    Map<String, String> CCW_controlUUIDs = new HashMap<>(); // this only stores the uuids from partMapList
                    List<String> participantsToAdd = new ArrayList<>();
                    List<String> participantsToRemove = new ArrayList<>();

                    for (Map<String, String> participantMap : participantMapList) {
                        CCW_controlUUIDs.put(participantMap.get("participant_uuid"), "participant_uuid");
                    }

                    if (CCW_controlUUIDs.size() > CCW_controlMap.size()) {
                        participantsToAdd = getMissingKeys(CCW_controlMap, CCW_controlUUIDs);
                    } else if (CCW_controlUUIDs.size() < CCW_controlMap.size()) {
                        participantsToRemove = getMissingKeys(CCW_controlMap, CCW_controlUUIDs);
                    } else {
                        // otherwise check if participant needs to be added
                        if (!CCW_controlUUIDs.keySet().equals(CCW_controlMap.keySet())){
                            for (String key: CCW_controlMap.keySet()){
                                if (!CCW_controlUUIDs.containsKey(key)){
                                    participantsToRemove.add(key);
                                }
                            }
                            for (String key: CCW_controlUUIDs.keySet()) {
                                if (!CCW_controlMap.containsKey(key)) {
                                    participantsToAdd.add(key);
                                }
                            }
                        }
                    }

                    for (String toRemove: participantsToRemove) {
                        CCW_Control removed = CCW_controlMap.remove(toRemove);
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
                        CCW_Control p0 = new CCW_Control(addMap);

                        logger.info("Restoring State for participant_uuid=" + toAdd);
                        p0.restoreSaveState(false);

                        synchronized (lockCCW_Control) {
                            CCW_controlMap.put(toAdd, p0);
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
                    synchronized (lockCCW_Control) {
                        CCW_controlMap.clear();
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

    public Map<String, CCW_Control> getCCW_ControlMap(){
        return this.CCW_controlMap;
    }


    public List<String> getMissingKeys(Map<String, CCW_Control> map1, Map<String, String> map2) {
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
