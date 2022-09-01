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
                   List<Map<String,String>> participantMapList = Launcher.dbEngine.getParticipantMapByGroup("Baseline");
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

    // this func not called anywhere
    // private Restricted setRestrictedTimers(Restricted p0) {
    //     try {
    //         Date date = new Date();   // given date

    //         Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    //         calendar.setTime(date);   // assigns calendar to given date
    //         long currentTime = calendar.getTime().getTime()/1000;

    //         calendar.set(Calendar.HOUR_OF_DAY, 4);
    //         calendar.set(Calendar.MINUTE, 0);
    //         long d1t400am = calendar.getTime().getTime()/1000;

    //         calendar.set(Calendar.HOUR_OF_DAY, 11);
    //         calendar.set(Calendar.MINUTE, 59);
    //         long d1t1159am = calendar.getTime().getTime()/1000;

    //         calendar.set(Calendar.HOUR_OF_DAY, 12);
    //         calendar.set(Calendar.MINUTE, 0);
    //         long d1t1200pm = calendar.getTime().getTime()/1000;

    //         calendar.set(Calendar.HOUR_OF_DAY, 21);
    //         calendar.set(Calendar.MINUTE, 0);
    //         long d1t900pm = calendar.getTime().getTime()/1000;

    //         long d2t359am = d1t400am + 86400 - 60; //add full day of seconds and subtract a minute

        
    //         int startWarnDiff =  (int)(d1t1159am-currentTime);
    //         if(startWarnDiff <= 0) {
    //             startWarnDiff = (int)currentTime + 300;
    //             p0.setStartWarnDeadline(startWarnDiff);
    //         } else {
    //             p0.setStartWarnDeadline(startWarnDiff);
    //         }
    //         p0.receivedWaitStart();


    //         //p0.setStartDeadline((int)(d2t359am - currentTime));
    //         //p0.setEndWarnDeadline((int)(d1t900pm - currentTime));
    //         //p0.setEndDeadline((int)(d2t359am - currentTime));

    //         //p0.receivedWaitStart();


    //         /*
    //         String routeString = GM + RM + GC + RC + TXp + RXp + TXa + RXa + TXr + RXr + TXpe + RXpe + TXae + RXae + TXre + RXre;
    //         routePath = Integer.parseInt(routeString, 2);
    //         //System.out.println("desc:" + rm.getParam("desc") + "\nroutePath:" + routePath + " RouteString:\n" + routeString + "\n" + rm.getParams());
    //          */

    //     } catch (Exception ex) {
    //         logger.error("setRestrictedTimers Error");
    //         logger.error(ex.toString());
    //     }

    //     return p0;
    // }

}
