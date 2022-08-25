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

// REMOVE THIS
import com.google.gson.Gson;

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
        checkTimer.scheduleAtFixedRate(new startRestricted(),checkdelay, checktimer);//remote

        //Calendar today = Calendar.getInstance();
        //today.set(Calendar.HOUR_OF_DAY, 4);
        //today.set(Calendar.MINUTE, 0);
        //today.set(Calendar.SECOND, 0);

        // every night at 3:59am you run your task
        //episodeResetTimer = new Timer();
        //episodeResetTimer.schedule(new episodeReset(), today.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)); // period: 1 day

    }

    public void incomingText(String participantId, Map<String,String> incomingMap) {

        try {

            //From
            //String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(incomingMap.get("From"));
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

                   //List<Map<String, String>> participantMapList = Launcher.dbEngine.getParticipant("Baseline");
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
                           System.out.println(participantMap);
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


    public void testWorking() {
        Gson gson = new Gson();
        try {
            logger.info("Creating P0");
            //Create dummy person
            Restricted p0 = new Restricted(gson.fromJson("{'first_name':'Sam', 'last_name':'A', 'number':'+18596844789', 'group':'Baseline', 'time_zone':'America/Louisville', 'participant_uuid':'CA36CB66-BA36-42EB-8D6B-CCB7632D4A53'}", Map.class));
            //set short deadline for cal end

            p0.setStartWarnDeadline(2);
            p0.setStartDeadline(5);
            p0.setEndWarnDeadline(10);
            p0.setEndDeadline(15);
            logger.info("p0 state: " + p0.getState());
            logger.info("Set WaitStart P0");
            p0.receivedWaitStart();
            logger.info("p0 state: " + p0.getState());
            //send start cal
            logger.info("Sending received StartCal: simulate message in");
            p0.receivedStartCal();

            //loop until end

            while(!(p0.getState() == RestrictedBase.State.endOfEpisode)) {
                logger.info("p0 state: " + p0.getState());
                Thread.sleep(1000);
                //p0.receivedStartCal();
                logger.info("Sending received EndCal: simulate message in");
                p0.receivedEndCal();

            }

            logger.info("p0 state: " + p0.getState());

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void testNoStart() {

        try {

            logger.info("Creating P0");
            //Create dummy person
            Restricted p0 = new Restricted("0");
            //set short deadline for cal end

            p0.setStartWarnDeadline(2);
            p0.setStartDeadline(5);
            p0.setEndWarnDeadline(10);
            p0.setEndDeadline(15);
            logger.info("p0 state: " + p0.getState());
            logger.info("Set WaitStart P0");
            p0.receivedWaitStart();


            while(!(p0.getState() == RestrictedBase.State.endOfEpisode)) {
                //logger.info("p0 state: " + p0.getState());
                Thread.sleep(1000);
                //p0.receivedStartCal();
                logger.info("Waiting for startCal");
            }

            logger.info("p0 state: " + p0.getState());




        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void testNoEnd() {

        try {

            logger.info("Creating P0");
            //Create dummy person
            Restricted p0 = new Restricted("0");
            //set short deadline for cal end

            p0.setStartWarnDeadline(2);
            p0.setStartDeadline(5);
            p0.setEndWarnDeadline(10);
            p0.setEndDeadline(15);
            logger.info("p0 state: " + p0.getState());
            logger.info("Set WaitStart P0");
            p0.receivedWaitStart();
            logger.info("p0 state: " + p0.getState());
            logger.info("Sending StartCal P0");
            p0.receivedStartCal();
            logger.info("p0 state: " + p0.getState());

            while(!(p0.getState() == RestrictedBase.State.endOfEpisode)) {
                //logger.info("p0 state: " + p0.getState());
                Thread.sleep(1000);
                logger.info("Waiting for endCal");
            }

            logger.info("p0 state: " + p0.getState());




        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void saveStateDemo() {

        try {
            logger.info("Creating P0");
            //Create dummy person
            Restricted p0 = new Restricted("0");
            //set short deadline for cal end

            p0.setStartWarnDeadline(2);
            p0.setStartDeadline(5);
            p0.setEndWarnDeadline(10);
            p0.setEndDeadline(15);
            logger.info("Sending WaitStart P0, simulate message in");
            p0.receivedWaitStart();
            logger.info(" ");
            //send start cal
            //p0.receivedStartCal();

            //loop until end
            /*
            while(!(p0.getState() == protocols.RestrictedBase.State.endOfEpisode)) {
                logger.info(p0.getState().toString());
                Thread.sleep(1000);
                //p0.receivedStartCal();
                p0.receivedEndCal();
            }
             */
            logger.info("Dump of P0");
            logger.info(p0.saveStateJSON());
            logger.info(" ");
            Thread.sleep(1000);
            Thread.sleep(1000);
            Thread.sleep(1000);
            Thread.sleep(1000);
            logger.info("Dump of P0 after 4 seconds");
            logger.info(p0.saveStateJSON());
            logger.info(" ");

            logger.info("Creating P1");
            Restricted p1 = new Restricted("1");
            logger.info("Restoring P0 to P1");
            logger.info(" ");
            p1.restoreSaveState(p0.stateJSON);
            logger.info("p0 state: " + p0.getState());
            logger.info("p1 state: " + p1.getState());
            logger.info(" ");
            logger.info("Dump of P1");
            logger.info(p1.saveStateJSON());
            logger.info(" ");
            Thread.sleep(1000);
            Thread.sleep(1000);
            Thread.sleep(1000);
            Thread.sleep(1000);
            logger.info("p0 after 4: " + p0.saveStateJSON());
            logger.info(" ");
            logger.info("p1 after 4: " + p1.saveStateJSON());
            logger.info(" ");
            //logger.info(p0.getState().toString());
            //p0.restoreSaveState(p0.stateJSON);


        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private Restricted setRestrictedTimers(Restricted p0) {

        try {

            Date date = new Date();   // given date

            Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
            calendar.setTime(date);   // assigns calendar to given date
            long currentTime = calendar.getTime().getTime()/1000;

            calendar.set(Calendar.HOUR_OF_DAY, 4);
            calendar.set(Calendar.MINUTE, 0);
            long d1t400am = calendar.getTime().getTime()/1000;

            calendar.set(Calendar.HOUR_OF_DAY, 11);
            calendar.set(Calendar.MINUTE, 59);
            long d1t1159am = calendar.getTime().getTime()/1000;

            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            long d1t1200pm = calendar.getTime().getTime()/1000;

            calendar.set(Calendar.HOUR_OF_DAY, 21);
            calendar.set(Calendar.MINUTE, 0);
            long d1t900pm = calendar.getTime().getTime()/1000;

            long d2t359am = d1t400am + 86400 - 60; //add full day of seconds and subtract a minute

            /*
            String none = "0";
            none = "1";

            String WaitS = "0";

            String WarnS = "0";

            String d14am1159am = "0";
            if((currentTime >= d1t400am) && (currentTime <= d1t1159am)) {
                d14am1159am = "1";
            }

            String d12pmd2359am = "0";
            if((currentTime >= d1t1200pm) && (currentTime <= d2t359am)) {
                d14am1159am = "1";
            }

            */
            int startWarnDiff =  (int)(d1t1159am-currentTime);
            if(startWarnDiff <= 0) {
                startWarnDiff = (int)currentTime + 300;
                p0.setStartWarnDeadline(startWarnDiff);
            } else {
                p0.setStartWarnDeadline(startWarnDiff);
            }
            p0.receivedWaitStart();


            //p0.setStartDeadline((int)(d2t359am - currentTime));
            //p0.setEndWarnDeadline((int)(d1t900pm - currentTime));
            //p0.setEndDeadline((int)(d2t359am - currentTime));

            //p0.receivedWaitStart();


            /*
            String routeString = GM + RM + GC + RC + TXp + RXp + TXa + RXa + TXr + RXr + TXpe + RXpe + TXae + RXae + TXre + RXre;
            routePath = Integer.parseInt(routeString, 2);
            //System.out.println("desc:" + rm.getParam("desc") + "\nroutePath:" + routePath + " RouteString:\n" + routeString + "\n" + rm.getParams());
             */

        } catch (Exception ex) {
            logger.error("setRestrictedTimers Error");
            logger.error(ex.toString());
        }

        return p0;
    }

}
