package fasting.Protocols.WeeklyMessage;

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

public class WeeklyMessageWatcher {

    private final AtomicBoolean lockWeeklyMessage = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();

    private final Map<String, WeeklyMessage> weeklyMessageMap;

    public WeeklyMessageWatcher() {
        Logger logger = LoggerFactory.getLogger(WeeklyMessageWatcher.class);
        this.weeklyMessageMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay",5000l);
        long checktimer = Launcher.config.getLongParam("checktimer",30000l);

        //create timer
        ScheduledExecutorService checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startWeeklyMessage(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    class startWeeklyMessage extends TimerTask {
        private Logger logger;
        private List<Map<String,String>> previousMapList;
        public startWeeklyMessage() {
            logger = LoggerFactory.getLogger(startWeeklyMessage.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Baseline");
                    participantMapList.addAll(Launcher.dbEngine.getParticipantMapByGroup("Control")); // if baseline or control
                    if (previousMapList == null){
                        //first run
                        previousMapList = participantMapList;
                    }

                    if (previousMapList.size() > 0 && participantMapList.size() == 0){
                        // clear anyone in previousMapList
                        for (Map<String,String> previousMap: previousMapList) {
                            String participantUUID = previousMap.get("participant_uuid");
                            String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantUUID);
                            if (!protocolNameDB.equals("Baseline") || !protocolNameDB.equals("Control")) {
                                WeeklyMessage toRemove = weeklyMessageMap.remove(participantUUID);
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
                        synchronized (lockWeeklyMessage) {
                            if(!weeklyMessageMap.containsKey(participantMap.get("participant_uuid"))) {
                                isActive = true;
                            } else if (!previousMapList.equals(participantMapList)) {
                                // figure out who go removed
                                // find which participant is in previousMapList but not in participantMapList
                                for (Map<String, String> previousMap : previousMapList) {
                                    if (!participantMapList.contains(previousMap)) {
                                        //check if participant is still enrolled in protocol
                                        String participantUUID = previousMap.get("participant_uuid");
                                        String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantUUID);
                                        if (!protocolNameDB.equals("Baseline") || !protocolNameDB.equals("Control")) {
                                            // removing participant
                                            WeeklyMessage toRemove = weeklyMessageMap.remove(participantUUID);
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
                            WeeklyMessage p0 = new WeeklyMessage(participantMap);

                            logger.info("Restoring State for participant_uuid=" + participantMap.get("participant_uuid"));
                            p0.restoreSaveState();

                            synchronized (lockWeeklyMessage) {
                                weeklyMessageMap.put(participantMap.get("participant_uuid"), p0);
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
                    synchronized (lockWeeklyMessage) {
                        weeklyMessageMap.clear();
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
