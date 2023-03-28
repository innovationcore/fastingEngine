package fasting.Protocols.Control;

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

public class ControlWatcher {
    private final Logger logger;
    private final ScheduledExecutorService checkTimer;

    private final AtomicBoolean lockControl = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private final Map<String,Control> controlMap;

    public ControlWatcher() {
        this.logger = LoggerFactory.getLogger(ControlWatcher.class);
        this.controlMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startControl(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
        
    }

    public void incomingText(String participantId, Map<String,String> incomingMap) {
        try {

            //From
            logger.info("Incoming number: " + incomingMap.get("From") + " parid: " + participantId);

            synchronized (lockControl) {
                if(controlMap.containsKey(participantId)) {
                    controlMap.get(participantId).incomingText(incomingMap);
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
                    validNextStates = "waitStart,startcal,warnEndCal";
                    break;
                case "waitStart":
                    validNextStates = "startcal,timeout24,endProtocol";
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
        Control removed = controlMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }

        //restart at beginning
        List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Control");
        //Create person
        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, participantId);
        Control p0 = new Control(addMap);

        p0.restoreSaveState(true);

        synchronized (lockControl) {
            controlMap.put(participantId, p0);
        }
    }

    public String moveToState(String participantId, String moveToState, String time) {
        String newState = "";
        try {
            Control participant = controlMap.get(participantId);
            switch (participant.getState()){
                case initial:
                    if (moveToState.equals("waitStart")){
                        participant.receivedWaitStart();
                        newState = "waitStart";
                    } else if (moveToState.equals("startcal")) {
                        participant.receivedStartCal();
                        newState = "startcal";
                    } else {
                        // invalid state
                        newState = "initial invalid";
                        break;
                    }
                    break;
                case waitStart:
                    if (moveToState.equals("startcal")) {
                        participant.receivedStartCal();
                        long timestamp = participant.TZHelper.parseTime(time);
                        Launcher.dbEngine.saveStartCalTime(participantId, timestamp);
                        newState = "startcal";
                    } else if (moveToState.equals("timeout24")) {
                        participant.timeoutwaitStartTotimeout24();
                        newState = "timeout24";
                    } else if (moveToState.equals("endProtocol")) {
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        // invalid state
                        newState = "waitstart invalid";
                        break;
                    }
                    break;
                case startcal:
                    if (moveToState.equals("startcal")){
                        participant.receivedStartCal();
                        long timestamp = participant.TZHelper.parseTime(time);
                        Launcher.dbEngine.saveStartCalTime(participantId, timestamp);
                        newState = "startcal";
                    } else if (moveToState.equals("endcal")) {
                        long timestamp = participant.TZHelper.parseTime(time);
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
                case endcal:
                    if (moveToState.equals("endcal")){
                        long timestamp = participant.TZHelper.parseTime(time);
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

    class startControl extends TimerTask {
        private final Logger logger;
        public startControl() {
            logger = LoggerFactory.getLogger(startControl.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Control");
                    Map<String, String> controlUUIDs = new HashMap<>(); // this only stores the uuids from partMapList
                    List<String> participantsToAdd = new ArrayList<>();
                    List<String> participantsToRemove = new ArrayList<>();

                    for (Map<String, String> participantMap : participantMapList) {
                        controlUUIDs.put(participantMap.get("participant_uuid"), "participant_uuid");
                    }

                    if (controlUUIDs.size() > controlMap.size()) {
                        participantsToAdd = getMissingKeys(controlMap, controlUUIDs);
                    } else if (controlUUIDs.size() < controlMap.size()) {
                        participantsToRemove = getMissingKeys(controlMap, controlUUIDs);
                    } else {
                        // otherwise check if participant needs to be added
                        if (!controlUUIDs.keySet().equals(controlMap.keySet())){
                            for (String key: controlMap.keySet()){
                                if (!controlUUIDs.containsKey(key)){
                                    participantsToRemove.add(key);
                                }
                            }
                            for (String key: controlUUIDs.keySet()) {
                                if (!controlMap.containsKey(key)) {
                                    participantsToAdd.add(key);
                                }
                            }
                        }
                    }

                    for (String toRemove: participantsToRemove) {
                        Control removed = controlMap.remove(toRemove);
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
                        Control p0 = new Control(addMap);

                        logger.info("Restoring State for participant_uuid=" + toAdd);
                        p0.restoreSaveState(false);

                        synchronized (lockControl) {
                            controlMap.put(toAdd, p0);
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
                    synchronized (lockControl) {
                        controlMap.clear();
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

    public Map<String, Control> getControlMap(){
        return this.controlMap;
    }


    public List<String> getMissingKeys(Map<String, Control> map1, Map<String, String> map2) {
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
