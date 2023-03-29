package fasting.Protocols.DailyMessage;

import fasting.Launcher;
import fasting.Protocols.Restricted.Restricted;
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
    private final Logger logger;
    private final AtomicBoolean lockDailyMessage = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();
    private final Map<String, DailyMessage> dailyMessageMap;

    public DailyMessageWatcher() {
        this.logger = LoggerFactory.getLogger(DailyMessageWatcher.class);
        this.dailyMessageMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        ScheduledExecutorService checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startDailyMessage(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void resetStateMachine(String participantId){
        // Remove participant from protocol
        DailyMessage removed = dailyMessageMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }

        //restart at beginning
        List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("TRE");
        //Create person
        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, participantId);
        DailyMessage p0 = new DailyMessage(addMap);
        p0.restoreSaveState(true);

        synchronized (lockDailyMessage) {
            dailyMessageMap.put(participantId, p0);
        }
    }

    public void updateTimeZone(String participantId, String tz) {
        DailyMessage toUpdate = dailyMessageMap.get(participantId);
        logger.warn(participantId + ": changed TZ from " + toUpdate.TZHelper.getUserTimezone() + " to " + tz);
        toUpdate.TZHelper.setUserTimezone(tz);
    }

    class startDailyMessage extends TimerTask {
        private final Logger logger;
        public startDailyMessage() {
            logger = LoggerFactory.getLogger(startDailyMessage.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String, String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("TRE");
                    Map<String, String> dailyUUIDs = new HashMap<>(); // this only stores the uuids from partMapList
                    List<String> participantsToAdd = new ArrayList<>();
                    List<String> participantsToRemove = new ArrayList<>();

                    for (Map<String, String> participantMap : participantMapList) {
                        dailyUUIDs.put(participantMap.get("participant_uuid"), "participant_uuid");
                    }

                    if (dailyUUIDs.size() > dailyMessageMap.size()) {
                        participantsToAdd = getMissingKeys(dailyMessageMap, dailyUUIDs);
                    } else if (dailyUUIDs.size() < dailyMessageMap.size()) {
                        participantsToRemove = getMissingKeys(dailyMessageMap, dailyUUIDs);
                    } else {
                        // otherwise check if participant needs to be added
                        if (!dailyUUIDs.keySet().equals(dailyMessageMap.keySet())) {
                            for (String key : dailyMessageMap.keySet()) {
                                if (!dailyUUIDs.containsKey(key)) {
                                    participantsToRemove.add(key);
                                }
                            }
                            for (String key : dailyUUIDs.keySet()) {
                                if (!dailyMessageMap.containsKey(key)) {
                                    participantsToAdd.add(key);
                                }
                            }
                        }
                    }

                    for (String toRemove : participantsToRemove) {
                        DailyMessage removed = dailyMessageMap.remove(toRemove);
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
                        DailyMessage p0 = new DailyMessage(addMap);

                        logger.info("Restoring State for participant_uuid=" + toAdd);
                        p0.restoreSaveState(false);

                        synchronized (lockDailyMessage) {
                            dailyMessageMap.put(toAdd, p0);
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

    public List<String> getMissingKeys(Map<String, DailyMessage> map1, Map<String, String> map2) {
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
