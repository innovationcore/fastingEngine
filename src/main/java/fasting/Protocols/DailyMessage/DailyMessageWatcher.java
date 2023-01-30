package fasting.Protocols.DailyMessage;

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

public class DailyMessageWatcher {

    private final AtomicBoolean lockDailyMessage = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();
    private final Map<String, DailyMessage> dailyMessageMap;

    public DailyMessageWatcher() {
        Logger logger = LoggerFactory.getLogger(DailyMessageWatcher.class);
        this.dailyMessageMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay",5000l);
        long checktimer = Launcher.config.getLongParam("checktimer",30000l);

        //create timer
        ScheduledExecutorService checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startDailyMessage(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    class startDailyMessage extends TimerTask {
        private Logger logger;
        private List<Map<String,String>> previousMapList;
        public startDailyMessage() {
            logger = LoggerFactory.getLogger(startDailyMessage.class);
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
                        for (Map<String,String> previousMap: previousMapList) {
                            String participantUUID = previousMap.get("participant_uuid");
                            String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantUUID);
                            if (!protocolNameDB.equals("TRE")) {
                                DailyMessage toRemove = dailyMessageMap.remove(participantUUID);
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
                        synchronized (lockDailyMessage) {
                            if(!dailyMessageMap.containsKey(participantMap.get("participant_uuid"))) {
                                isActive = true;
                            } else if (!previousMapList.equals(participantMapList)) {
                                // figure out who go removed
                                // find which participant is in previousMapList but not in participantMapList
                                for (Map<String, String> previousMap : previousMapList) {
                                    if (!participantMapList.contains(previousMap)) {
                                        //check if participant is still enrolled in protocol
                                        String participantUUID = previousMap.get("participant_uuid");
                                        String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantUUID);
                                        if (!protocolNameDB.equals("TRE")) {
                                            // removing participant
                                            DailyMessage toRemove = dailyMessageMap.remove(participantUUID);
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
                            DailyMessage p0 = new DailyMessage(participantMap);

                            logger.info("Restoring State for participant_uuid=" + participantMap.get("participant_uuid"));
                            p0.restoreSaveState();

                            synchronized (lockDailyMessage) {
                                dailyMessageMap.put(participantMap.get("participant_uuid"), p0);
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
                    synchronized (lockDailyMessage) {
                        dailyMessageMap.clear();
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
