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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestrictedWatcher {

    private Logger logger;
    private Timer checkTimer;

    private AtomicBoolean lockRestricted = new AtomicBoolean();
    private Map<String,Restricted> restrictedMap;

    public RestrictedWatcher() {
        this.logger = LoggerFactory.getLogger(RestrictedWatcher.class);
        this.restrictedMap = Collections.synchronizedMap(new HashMap<>());

        //how long to wait before checking protocols
        long checkdelay = Launcher.config.getLongParam("checkdelay",0l);
        long checktimer = Launcher.config.getLongParam("checktimer",30000l);

        //create timer
        checkTimer = new Timer();
        //set timer
        checkTimer.scheduleAtFixedRate(new startRestricted(),checkdelay, checktimer);//remote


    }

    public void incomingText(Map<String,String> incomingMap) {

        try {

            //From
            String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(incomingMap.get("From"));
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
                List<Map<String, String>> participantMapList = Launcher.dbEngine.getParticipant("other_participation_type");
                for (Map<String, String> participantMap : participantMapList) {

                    boolean isActive = false;
                    synchronized (lockRestricted) {
                        if(!restrictedMap.containsKey(participantMap.get("participant_id"))) {
                            isActive = true;
                        }

                    }

                    if(isActive) {
                        logger.info("Creating state machine for participant_id=" + participantMap.get("participant_id"));
                        //Create dummy person
                        Restricted p0 = new Restricted(participantMap);

                        //set short deadline for cal end
                        p0.setStartWarnDeadline(60);
                        p0.setStartDeadline(120);
                        p0.setEndWarnDeadline(240);
                        p0.setEndDeadline(300);

                        logger.info("Set WaitStart for participant_id=" + participantMap.get("participant_id"));
                        p0.receivedWaitStart();

                        synchronized (lockRestricted) {
                            restrictedMap.put(participantMap.get("participant_id"), p0);
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



    public void testWorking() {

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

}
