package fasting.Protocols.CCW.CCW_DailyMessage;

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

public class CCW_DailyMessageWatcher {
    private final Logger logger;
    private final AtomicBoolean lockCCW_DailyMessage = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();
    private final Map<String, CCW_DailyMessage> CCW_dailyMessageMap;

    public CCW_DailyMessageWatcher() {
        this.logger = LoggerFactory.getLogger(CCW_DailyMessageWatcher.class);
        this.CCW_dailyMessageMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        ScheduledExecutorService checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startCCW_DailyMessage(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void resetStateMachine(String participantId){
        // Remove participant from protocol
        CCW_DailyMessage removed = CCW_dailyMessageMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }

        //restart at beginning
        List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("TRE", "CCW");
        //Create person
        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, participantId);
        CCW_DailyMessage p0 = new CCW_DailyMessage(addMap);
        p0.restoreSaveState(true);

        synchronized (lockCCW_DailyMessage) {
            CCW_dailyMessageMap.put(participantId, p0);
        }
    }

    public void updateTimeZone(String participantId, String tz) {
        CCW_DailyMessage toUpdate = CCW_dailyMessageMap.get(participantId);
        logger.warn(participantId + ": changed TZ from " + toUpdate.TZHelper.getUserTimezone() + " to " + tz);
        toUpdate.TZHelper.setUserTimezone(tz);
    }

    class startCCW_DailyMessage extends TimerTask {
        private final Logger logger;
        public startCCW_DailyMessage() {
            logger = LoggerFactory.getLogger(startCCW_DailyMessage.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String, String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("TRE", "CCW");
                    Map<String, String> dailyUUIDs = new HashMap<>(); // this only stores the uuids from partMapList
                    List<String> participantsToAdd = new ArrayList<>();
                    List<String> participantsToRemove = new ArrayList<>();

                    for (Map<String, String> participantMap : participantMapList) {
                        dailyUUIDs.put(participantMap.get("participant_uuid"), "participant_uuid");
                    }

                    if (dailyUUIDs.size() > CCW_dailyMessageMap.size()) {
                        participantsToAdd = getMissingKeys(CCW_dailyMessageMap, dailyUUIDs);
                    } else if (dailyUUIDs.size() < CCW_dailyMessageMap.size()) {
                        participantsToRemove = getMissingKeys(CCW_dailyMessageMap, dailyUUIDs);
                    } else {
                        // otherwise check if participant needs to be added
                        if (!dailyUUIDs.keySet().equals(CCW_dailyMessageMap.keySet())) {
                            for (String key : CCW_dailyMessageMap.keySet()) {
                                if (!dailyUUIDs.containsKey(key)) {
                                    participantsToRemove.add(key);
                                }
                            }
                            for (String key : dailyUUIDs.keySet()) {
                                if (!CCW_dailyMessageMap.containsKey(key)) {
                                    participantsToAdd.add(key);
                                }
                            }
                        }
                    }

                    for (String toRemove : participantsToRemove) {
                        CCW_DailyMessage removed = CCW_dailyMessageMap.remove(toRemove);
                        if (removed != null) {
                            removed.receivedEndProtocol();
                            removed.uploadSave.shutdownNow();
                            removed = null;
                            System.gc();
                        }
                    }

                    for (String toAdd : participantsToAdd) {
                        logger.info("Creating state machine for participant_uuid=" + toAdd);
                        //Create person
                        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, toAdd);
                        if (addMap.isEmpty()) {
                            continue;
                        }
                        CCW_DailyMessage p0 = new CCW_DailyMessage(addMap);

                        logger.info("Restoring State for participant_uuid=" + toAdd);
                        p0.restoreSaveState(false);

                        synchronized (lockCCW_DailyMessage) {
                            CCW_dailyMessageMap.put(toAdd, p0);
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
                    synchronized (lockCCW_DailyMessage) {
                        CCW_dailyMessageMap.clear();
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

    public List<String> getMissingKeys(Map<String, CCW_DailyMessage> map1, Map<String, String> map2) {
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
