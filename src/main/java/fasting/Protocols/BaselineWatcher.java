package fasting.Protocols;

import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BaselineWatcher {
    private Logger logger;
    private ScheduledExecutorService checkTimer;

    private AtomicBoolean lockBaseline = new AtomicBoolean();
    private AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private Map<String,Baseline> baselineMap;

    public BaselineWatcher() {
        this.logger = LoggerFactory.getLogger(BaselineWatcher.class);
        this.baselineMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay",5000l);
        long checktimer = Launcher.config.getLongParam("checktimer",30000l);

        //create timer
        checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startBaseline(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void incomingText(String participantId, Map<String,String> incomingMap) {
        try {

            //From
            logger.info("Incoming number: " + incomingMap.get("From") + " parid: " + participantId);

            synchronized (lockBaseline) {
                if(baselineMap.containsKey(participantId)) {
                    baselineMap.get(participantId).incomingText(incomingMap);
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
                    validNextStates = "waitStart,startcal";
                    break;
                case "waitStart":
                    validNextStates = "startcal,timeout24,endProtocol";
                    break;
                case "startcal":
                    validNextStates = "startcal,endcal,timeout24,endProtocol";
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

    public String moveToState(String participantId, String moveToState, long timestamp) {
        String newState = "";
        try {
            Baseline participant = baselineMap.get(participantId);
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
                        Launcher.dbEngine.saveStartCalTime(participantId, timestamp);
                        newState = "startcal";
                    } else if (moveToState.equals("endcal")) {
                        Launcher.dbEngine.saveEndCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedEndCal();
                        Launcher.dbEngine.removeTempEndCal(participantId);
                        newState = "endcal";
                    } else if (moveToState.equals("timeout24")) {
                        participant.timeoutstartcalTotimeout24();
                        newState = "timeout24";
                    } else if (moveToState.equals("endProtocol")) {
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
                    } else {
                        newState = "startcal invalid";
                        // invalid state
                        break;
                    }
                    break;
                case endcal:
                    if (moveToState.equals("endcal")){
                        Launcher.dbEngine.saveEndCalTimeCreateTemp(participantId, timestamp);
                        participant.receivedEndCal();
                        Launcher.dbEngine.removeTempEndCal(participantId);
                        newState = "endcal";
                    } else if (moveToState.equals("waitStart")) {
                        // nothing needs to happen here because it will move to next state immediately
                        newState = "waitStart";
                    } else if (moveToState.equals("endProtocol")) {
                        participant.receivedEndProtocol();
                        newState = "endProtocol";
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

    class startBaseline extends TimerTask {
        private Logger logger;
        private List<Map<String,String>> previousMapList;
        public startBaseline() {
            logger = LoggerFactory.getLogger(startBaseline.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Baseline");
                    if (previousMapList == null){
                        //first run
                        previousMapList = participantMapList;
                    }

                    if (previousMapList.size() > 0 && participantMapList.size() == 0){
                        // clear anyone in previousMapList
                        for (Map<String,String> previousMap: previousMapList){
                            Baseline toRemove = baselineMap.remove(previousMap.get("participant_uuid"));
                            if(toRemove != null){
                                toRemove.receivedEndProtocol();
                                toRemove = null;
                                System.gc();
                            }
                        }
                    }

                    for (Map<String, String> participantMap : participantMapList) {
                        boolean isActive = false;
                        synchronized (lockBaseline) {
                            if(!baselineMap.containsKey(participantMap.get("participant_uuid"))) {
                                isActive = true;
                            } else if (!previousMapList.equals(participantMapList)) {
                                // figure out who go removed
                                // find which participant is in previousMapList but not in participantMapList
                                for (Map<String, String> previousMap : previousMapList) {
                                    if (!participantMapList.contains(previousMap)) {
                                        // removing participant
                                        Baseline toRemove = baselineMap.remove(previousMap.get("participant_uuid"));
                                        if(toRemove != null){
                                            toRemove.receivedEndProtocol();
                                            toRemove = null;
                                            System.gc();
                                        }
                                    }
                                }
                            }
                        }

                        if(isActive) {
                            logger.info("Creating state machine for participant_uuid=" + participantMap.get("participant_uuid"));
                            //Create person
                            Baseline p0 = new Baseline(participantMap);

                            logger.info("Restoring State for participant_uuid=" + participantMap.get("participant_uuid"));
                            p0.restoreSaveState();

                            synchronized (lockBaseline) {
                                baselineMap.put(participantMap.get("participant_uuid"), p0);
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
        private Logger logger;
        public episodeReset() {
            logger = LoggerFactory.getLogger(episodeReset.class);
        }

        public void run() {
            try {
                logger.error("RESET!!");
                synchronized (lockEpisodeReset) {
                    synchronized (lockBaseline) {
                        baselineMap.clear();
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
