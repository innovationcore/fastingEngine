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

    class startRestricted extends TimerTask {
        private Logger logger;
        public startRestricted() {
            logger = LoggerFactory.getLogger(startRestricted.class);
        }

        public void run() {
            try {
               synchronized (lockEpisodeReset) {
                   List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("TRF");
                   for (Map<String, String> participantMap : participantMapList) {

                       boolean isActive = false;
                       synchronized (lockRestricted) {
                           if(!restrictedMap.containsKey(participantMap.get("participant_uuid"))) {
                               isActive = true;
                           }
                       }

                       if(isActive) {
                           logger.info("Creating state machine for participant_uuid=" + participantMap.get("participant_uuid"));
                           //Create dummy person
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
