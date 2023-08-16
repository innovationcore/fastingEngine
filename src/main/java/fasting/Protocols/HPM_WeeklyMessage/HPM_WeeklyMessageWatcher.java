package fasting.Protocols.HPM_WeeklyMessage;

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

public class HPM_WeeklyMessageWatcher {
    private final Logger logger;
    private final AtomicBoolean lockHPM_WeeklyMessage = new AtomicBoolean();
    private final AtomicBoolean lockEpisodeReset = new AtomicBoolean();
    private final Map<String, HPM_WeeklyMessage> HPM_weeklyMessageMap;

    public HPM_WeeklyMessageWatcher() {
        this.logger = LoggerFactory.getLogger(HPM_WeeklyMessageWatcher.class);
        this.HPM_weeklyMessageMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay", 5000L);
        long checktimer = Launcher.config.getLongParam("checktimer", 30000L);

        //create timer
        ScheduledExecutorService checkTimer = Executors.newScheduledThreadPool(1);
        //set timer
        checkTimer.scheduleAtFixedRate(new startHPM_WeeklyMessage(), checkdelay, checktimer, TimeUnit.MILLISECONDS);
    }

    public void resetStateMachine(String participantId){
        // Remove participant from protocol
        HPM_WeeklyMessage removed = HPM_weeklyMessageMap.remove(participantId);
        if (removed != null) {
            removed.receivedEndProtocol();
            removed.uploadSave.shutdownNow();
            removed = null;
            System.gc();
        }

        //restart at beginning
        List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Control", "HPM");
        participantMapList.addAll(Launcher.dbEngine.getParticipantMapByGroup("Baseline", "HPM"));
        //Create person
        Map<String, String> addMap = getHashMapByParticipantUUID(participantMapList, participantId);
        HPM_WeeklyMessage p0 = new HPM_WeeklyMessage(addMap);
        p0.restoreSaveState(true);

        synchronized (lockHPM_WeeklyMessage) {
            HPM_weeklyMessageMap.put(participantId, p0);
        }
    }

    public void updateTimeZone(String participantId, String tz) {
        HPM_WeeklyMessage toUpdate = HPM_weeklyMessageMap.get(participantId);
        logger.warn(participantId + ": changed TZ from " + toUpdate.TZHelper.getUserTimezone() + " to " + tz);
        toUpdate.TZHelper.setUserTimezone(tz);
    }

    class startHPM_WeeklyMessage extends TimerTask {
        private Logger logger;
        public startHPM_WeeklyMessage() {
            logger = LoggerFactory.getLogger(startHPM_WeeklyMessage.class);
        }

        public void run() {
            try {
                synchronized (lockEpisodeReset) {
                    List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Control", "HPM");
                    participantMapList.addAll(Launcher.dbEngine.getParticipantMapByGroup("Baseline", "HPM"));
                    Map<String, String> weeklyUUIDs = new HashMap<>(); // this only stores the uuids from partMapList
                    List<String> participantsToAdd = new ArrayList<>();
                    List<String> participantsToRemove = new ArrayList<>();

                    for (Map<String, String> participantMap : participantMapList) {
                        weeklyUUIDs.put(participantMap.get("participant_uuid"), "participant_uuid");
                    }

                    if (weeklyUUIDs.size() > HPM_weeklyMessageMap.size()) {
                        participantsToAdd = getMissingKeys(HPM_weeklyMessageMap, weeklyUUIDs);
                    } else if (weeklyUUIDs.size() < HPM_weeklyMessageMap.size()) {
                        participantsToRemove = getMissingKeys(HPM_weeklyMessageMap, weeklyUUIDs);
                    } else {
                        // otherwise check if participant needs to be added
                        if (!weeklyUUIDs.keySet().equals(HPM_weeklyMessageMap.keySet())){
                            for (String key: HPM_weeklyMessageMap.keySet()){
                                if (!weeklyUUIDs.containsKey(key)){
                                    participantsToRemove.add(key);
                                }
                            }
                            for (String key: weeklyUUIDs.keySet()) {
                                if (!HPM_weeklyMessageMap.containsKey(key)) {
                                    participantsToAdd.add(key);
                                }
                            }
                        }
                    }

                    for (String toRemove: participantsToRemove) {
                        HPM_WeeklyMessage removed = HPM_weeklyMessageMap.remove(toRemove);
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
                        HPM_WeeklyMessage p0 = new HPM_WeeklyMessage(addMap);

                        logger.info("Restoring State for participant_uuid=" + toAdd);
                        p0.restoreSaveState(false);

                        synchronized (lockHPM_WeeklyMessage) {
                            HPM_weeklyMessageMap.put(toAdd, p0);
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
                    synchronized (lockHPM_WeeklyMessage) {
                        HPM_weeklyMessageMap.clear();
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

    public List<String> getMissingKeys(Map<String, HPM_WeeklyMessage> map1, Map<String, String> map2) {
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
