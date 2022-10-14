package fasting.Protocols;

import fasting.Launcher;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestrictedWatcher {
    private Logger logger;
    private Timer checkTimer;
    private Timer episodeResetTimer;

    private AtomicBoolean lockRestricted = new AtomicBoolean();
    private AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private Map<String,Restricted> restrictedMap;

    public RestrictedWatcher() {
        this.logger = LoggerFactory.getLogger(RestrictedWatcher.class);
        this.restrictedMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay",5000l);
        long checktimer = Launcher.config.getLongParam("checktimer",30000l);

        //create timer
        checkTimer = new Timer();
        //set timer
        checkTimer.scheduleAtFixedRate(new startRestricted(), checkdelay, checktimer);
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
                    validNextStates = "waitStart,warnStartCal,startcal,warnEndCal";
                    break;
                case "waitStart":
                    validNextStates = "warnStartCal,startcal";
                    break;
                case "warnStartCal":
                    validNextStates = "startcal,missedStartCal";
                    break;
                case "startcal":
                    validNextStates = "startcal,endcal,warnEndCal";
                    break;
                case "missedStartCal":
                    validNextStates = "endOfEpisode";
                    break;
                case "warnEndCal":
                    validNextStates = "endcal,missedEndCal";
                    break;
                case "endcal":
                    validNextStates = "endOfEpisode";
                    break;
                case "missedEndCal":
                    validNextStates = "endOfEpisode";
                    break;
                case "endOfEpisode":
                    validNextStates = "waitStart";
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

    public String moveToState(String participantId, String moveToState) {
        String newState = "";
        try {
            Restricted participant = restrictedMap.get(participantId);
            switch (participant.getState()){
                case initial:
                    if (moveToState == "waitStart"){
                        participant.receivedWaitStart();
                        newState = "waitStart";
                    } else if (moveToState == "warnStartCal") {
                        participant.receivedWarnStartCal();
                        newState = "warnStartCal";
                    } else if (moveToState == "startcal") {
                        participant.receivedStartCal();
                        newState = "startcal";
                    } else if (moveToState == "warnEndCal") {
                        participant.recievedWarnEndCal();
                        newState = "warnEndCal";
                    } else
                        // invalid state
                        break;
                    
                    break;
                case waitStart:
                    if (moveToState == "warnStartCal"){
                        participant.timeoutwaitStartTowarnStartCal();
                        newState = "warnStartCal";
                    } else if (moveToState == "startcal") {
                        participant.receivedStartCal();
                        newState = "startcal";
                    } else 
                        // invalid state
                        break;
                    break;
                case warnStartCal:
                    if (moveToState == "startcal") {
                        participant.receivedStartCal();
                        newState = "startcal";
                    } else if (moveToState == "missedStartCal") {
                        participant.timeoutwarnStartCalTomissedStartCal();
                        newState = "missedStartCal";
                    } else
                        // invalid state
                        break;
                    break;
                case startcal:
                    if (moveToState == "startcal"){ 
                        participant.receivedStartCal();
                        newState = "startcal";
                    } else if (moveToState == "endcal") {
                        participant.receivedEndCal();
                        newState = "endcal";
                    } else if (moveToState == "warnEndCal"){
                        participant.timeoutstartcalTowarnEndCal();
                        newState = "warnEndCal";
                    } else
                        // invalid state
                        break;
                    break;
                case missedStartCal:
                    if (moveToState == "endOfEpisode"){ 
                        // nothing needs to happen here because it will move to next state immediately
                        newState = "endOfEpisode";
                    } else
                        // invalid state
                        break;
                    break;
                case warnEndCal:
                    if (moveToState == "endcal") {
                        participant.receivedEndCal();
                        newState = "endcal";
                    } else if (moveToState == "missedEndCal"){ 
                        participant.timeoutwarnEndCalTomissedEndCal();
                        newState = "missedEndCal";
                    } else
                        // invalid state
                        break;
                    break;
                case endcal:
                    if (moveToState == "endOfEpisode") {
                        // nothing needs to happen here because it will move to next state immediately
                        newState = "endOfEpisode";
                    } else
                        // invalid state
                        break;
                    break;
                case missedEndCal:
                    if (moveToState == "endOfEpisode"){ 
                        // nothing needs to happen here because it will move to next state immediately
                        newState = "endOfEpisode";
                    } else
                        // invalid state
                        break;
                    break;
                case endOfEpisode:
                    if (moveToState == "waitStart"){
                        participant.timeoutendOfEpisodeTowaitStart();
                        newState = "waitStart";
                    } else
                        // invalid state
                        break;
                    break;
                default:
                    // invalid currentState
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
        private Logger logger;
        public startRestricted() {
            logger = LoggerFactory.getLogger(startRestricted.class);
        }

        public void run() {
            try {
               synchronized (lockEpisodeReset) {
                   List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("TRE");
                   for (Map<String, String> participantMap : participantMapList) {

                       boolean isActive = false;
                       synchronized (lockRestricted) {
                           if(!restrictedMap.containsKey(participantMap.get("participant_uuid"))) {
                               isActive = true;
                           }
                       }

                       if(isActive) {
                           logger.info("Creating state machine for participant_uuid=" + participantMap.get("participant_uuid"));
                           //Create person
                           Restricted p0 = new Restricted(participantMap);

                           logger.info("Set WaitStart for participant_uuid=" + participantMap.get("participant_uuid"));
                           p0.receivedWaitStart();

                           synchronized (lockRestricted) {
                               restrictedMap.put(participantMap.get("participant_uuid"), p0);
                           }
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
        private Logger logger;
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
