package fasting.Protocols;

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

public class RestrictedWatcher {
    private final Logger logger;

    private final AtomicBoolean lockRestricted = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private final Map<String,Restricted> restrictedMap;

    public RestrictedWatcher() {
        this.logger = LoggerFactory.getLogger(RestrictedWatcher.class);
        this.restrictedMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        ScheduledExecutorService checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startRestricted(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void incomingText(String participantId, Map<String,String> incomingMap) {
        try {

            //From
            logger.info("Incoming number: " + incomingMap.get("From") + " parid: " + participantId);

            synchronized (lockRestricted) {
                if(restrictedMap.containsKey(participantId)) {
                    restrictedMap.get(participantId).incomingText(incomingMap);
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
                    validNextStates = "warnStartCal,startcal,dayOffWait,endProtocol";
                    break;
                case "warnStartCal":
                    validNextStates = "startcal,missedStartCal,dayOffWarn,endProtocol";
                    break;
                case "startcal":
                    validNextStates = "startcal,endcal,warnEndCal,dayOffStartCal,endProtocol";
                    break;
                case "missedStartCal":
                case "endcal":
                case "missedEndCal":
                    validNextStates = "endOfEpisode";
                    break;
                case "warnEndCal":
                    validNextStates = "endcal,missedEndCal,dayOffWarnEndCal,endProtocol";
                    break;
                case "endOfEpisode":
                    validNextStates = "resetEpisodeVariables,endcal,endProtocol";
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

    public String moveToState(String participantId, String moveToState, long timestamp) {
        String newState = "";
        try {
            Restricted participant = restrictedMap.get(participantId);
            switch (participant.getState()){
                case initial:
                    if (moveToState.equals("waitStart")){
                        participant.receivedWaitStart();
                        newState = "waitStart";
                    } else if (moveToState.equals("warnStartCal")) {
                        participant.receivedWarnStartCal();
                        newState = "warnStartCal";
                    } else if (moveToState.equals("startcal")) {
                        participant.receivedStartCal();
                        newState = "startcal";
                    } else if (moveToState.equals("warnEndCal")) {
                        participant.recievedWarnEndCal();
                        newState = "warnEndCal";
                    } else {
                        // invalid state
                        newState = "initial invalid";
                    }
                    break;
                case waitStart:
                    if (moveToState.equals("warnStartCal")){
                        participant.timeoutwaitStartTowarnStartCal();
                        newState = "warnStartCal";
                    } else if (moveToState.equals("startcal")) {
                        participant.receivedStartCal();
                        Launcher.dbEngine.saveStartCalTime(participantId, timestamp);
                        newState = "startcal";
                    } else if (moveToState.equals("dayOffWait")) {
                    participant.receivedDayOff();
                    newState = "dayOffWait";
                    } else {
                        // invalid state
                        newState = "waitstart invalid";
                    }
                    break;
                case warnStartCal:
                    if (moveToState.equals("startcal")) {
                        participant.receivedStartCal();
                        Launcher.dbEngine.saveStartCalTime(participantId, timestamp);
                        newState = "startcal";
                    } else if (moveToState.equals("missedStartCal")) {
                        participant.timeoutwarnStartCalTomissedStartCal();
                        newState = "missedStartCal";
                    } else if (moveToState.equals("dayOffWarn")) {
                        participant.receivedDayOff();
                        newState = "dayOffWarn";
                    } else {
                        newState = "warnstart invalid";
                        // invalid state
                    }
                    break;
                case startcal:
                    if (moveToState.equals("startcal")){ 
                        participant.receivedStartCal();
                        Launcher.dbEngine.saveStartCalTime(participantId, timestamp);
                        newState = "startcal";
                    } else if (moveToState.equals("endcal")) {
                        Launcher.dbEngine.saveEndCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedEndCal();
                        Launcher.dbEngine.removeTempEndCal(participantId);
                        newState = "endcal";
                    } else if (moveToState.equals("warnEndCal")){
                        participant.timeoutstartcalTowarnEndCal();
                        newState = "warnEndCal";
                    } else if (moveToState.equals("dayOffStartCal")) {
                        participant.receivedDayOff();
                        newState = "dayOffStartCal";
                    } else {
                        newState = "startcal invalid";
                        // invalid state
                    }
                    break;
                case missedStartCal:
                    if (moveToState.equals("endOfEpisode")){ 
                        // nothing needs to happen here because it will move to next state immediately
                        newState = "endOfEpisode";
                    } else {
                        // invalid state
                        newState = "missedstartcal invalid";
                    }
                    break;
                case warnEndCal:
                    if (moveToState.equals("endcal")) {
                        Launcher.dbEngine.saveEndCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedEndCal(); // needs to create endcal state before saving time
                        Launcher.dbEngine.removeTempEndCal(participantId);
                        newState = "endcal";
                    } else if (moveToState.equals("missedEndCal")){ 
                        participant.timeoutwarnEndCalTomissedEndCal();
                        newState = "missedEndCal";
                    } else if (moveToState.equals("dayOffWarnEndCal")) {
                        participant.receivedDayOff();
                        newState = "dayOffWarnEndCal";
                    } else {
                        // invalid state
                        newState = "warnEndCal invalid";
                    }
                    break;
                case endcal:
                    if (moveToState.equals("endOfEpisode")) {
                        // nothing needs to happen here because it will move to next state immediately
                        newState = "endOfEpisode";
                    } else {
                        // invalid state
                        newState = "endcal invalid";
                    }
                    break;
                case missedEndCal:
                    if (moveToState.equals("endOfEpisode")){ 
                        // nothing needs to happen here because it will move to next state immediately
                        newState = "endOfEpisode";
                    } else {
                        // invalid state
                        newState = "missedEndCal invalid";

                    }
                    break;
                case endOfEpisode:
                    if (moveToState.equals("resetEpisodeVariables")){
                        participant.timeoutendOfEpisodeToresetEpisodeVariables();
                        newState = "resetEpisodeVariables";
                    } else if (moveToState.equals("endcal")){
                        Launcher.dbEngine.saveEndCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedEndCal();
                        Launcher.dbEngine.removeTempEndCal(participantId);
                        newState = "endcal";
                    } else {
                        // invalid state
                        newState = "endOfEpisode invalid";
                    }
                    break;
                case resetEpisodeVariables:
                    if(moveToState.equals("waitStart")){
                        // nothing needs to happen, moves immediately to waitstart
                        newState = "waitStart";
                    } else {
                        // invalid state
                        newState = "resetEpisodeVariables invalid";
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

    class startRestricted extends TimerTask {
        private final Logger logger;
        private List<Map<String,String>> previousMapList;
        public startRestricted() {
            logger = LoggerFactory.getLogger(startRestricted.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("TRE");
                    if (previousMapList == null){
                        //first run
                        previousMapList = participantMapList;
                    }

                    if (previousMapList.size() > 0 && participantMapList.size() == 0){
                        // clear anyone in previousMapList
                        for (Map<String,String> previousMap: previousMapList){
                            String participantUUID = previousMap.get("participant_uuid");
                            String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantUUID);
                            if (!protocolNameDB.equals("TRE")) {
                                Restricted toRemove = restrictedMap.remove(participantUUID);
                                if (toRemove != null) {
                                    toRemove.receivedEndProtocol();
                                    toRemove = null;
                                    System.gc();
                                }
                            }
                        }
                    }

                    for (Map<String, String> participantMap : participantMapList) {
                        boolean isActive = false;
                        synchronized (lockRestricted) {
                            if(!restrictedMap.containsKey(participantMap.get("participant_uuid"))) {
                                isActive = true;
                            } else if (!previousMapList.equals(participantMapList)) {
                                // figure out who go removed
                                // find which participant is in previousMapList but not in participantMapList
                                for (Map<String, String> previousMap : previousMapList) {
                                    if (!participantMapList.contains(previousMap)) {
                                        String participantUUID = previousMap.get("participant_uuid");
                                        String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantUUID);
                                        if (!protocolNameDB.equals("TRE")) {
                                            // removing participant
                                            Restricted toRemove = restrictedMap.remove(participantUUID);
                                            if (toRemove != null) {
                                                toRemove.receivedEndProtocol();
                                                toRemove = null;
                                                System.gc();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if(isActive) {
                            logger.info("Creating state machine for participant_uuid=" + participantMap.get("participant_uuid"));
                            //Create person
                            Restricted p0 = new Restricted(participantMap);

                            logger.info("Restoring State for participant_uuid=" + participantMap.get("participant_uuid"));
                            p0.restoreSaveState();

                            synchronized (lockRestricted) {
                                restrictedMap.put(participantMap.get("participant_uuid"), p0);
                            }
                        }
                    }
                    previousMapList = participantMapList;
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
                    synchronized (lockRestricted) {
                        restrictedMap.clear();
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

} //class 
