package fasting.Protocols;
import fasting.TimeUtils.TimezoneHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import java.util.*;

public class Testing {

    private Logger logger;
    private Gson gson;

    public Testing() {
        logger = LoggerFactory.getLogger(Testing.class);
        gson = new Gson();
    }

    // Simulating a happy user interaction
    public void testHappyWorking() {

        try {
            logger.info("Creating P0");
            //Create dummy person
            Restricted p0 = new Restricted(gson.fromJson("{'first_name':'Sam','last_name':'A','number':'+18596844789','group':'Baseline','time_zone':'America/Louisville','participant_uuid':'7C52EFDD-8E85-4D86-BCF3-C636242CF2F7'}", Map.class));
            //set short deadline for cal end

            // these first 4 are in state initial
            p0.setStartWarnDeadline(2); //in seconds
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
            logger.info("p0 state: " + p0.getState());

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
            Restricted p0 = new Restricted(gson.fromJson("{'first_name':'Sam','last_name':'A','number':'+18596844789','group':'Baseline','time_zone':'America/Louisville','participant_uuid':'7C52EFDD-8E85-4D86-BCF3-C636242CF2F7'}", Map.class));
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
            Restricted p0 = new Restricted(gson.fromJson("{'first_name':'Sam','last_name':'A','number':'+18596844789','group':'Baseline','time_zone':'America/Louisville','participant_uuid':'7C52EFDD-8E85-4D86-BCF3-C636242CF2F7'}", Map.class));
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

    public void testTiming() {
        Integer total;
        Integer hours;
        Integer mins;
        Integer seconds;

        //SAME TIMEZONE
        System.out.println("\n SAME ZONE");
        TimezoneHelper timezoneHelper = new TimezoneHelper("America/Louisville", "America/Louisville");
        total = timezoneHelper.getSecondsTo1159am();
        System.out.println("Total seconds to 11:59am: " + total);
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until Noon: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper.getSecondsTo2059pm();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 9pm: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper.getSecondsTo359am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 3:59:30am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper.getSecondsTo4am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 4am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        System.out.println("User LocalTime: " + timezoneHelper.getUserLocalTime());
        System.out.println("User time til 3:59:30am: " + timezoneHelper.getDateFromAddingSeconds(timezoneHelper.getSecondsTo359am()));
        

        //USER BEFORE TIMEZONE
        System.out.println("\n USER BEHIND MACHINE");
        TimezoneHelper timezoneHelper1 = new TimezoneHelper("America/Los_Angeles", "America/Louisville");
        total = timezoneHelper1.getSecondsTo1159am();
        System.out.println("Total seconds to 11:59am: " + total);
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until Noon: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper1.getSecondsTo2059pm();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 9pm: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper1.getSecondsTo359am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 4am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper1.getSecondsTo4am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 4am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));
        System.out.println("User LocalTime: " + timezoneHelper1.getUserLocalTime());
        System.out.println("User time til 4am: " + timezoneHelper1.getDateFromAddingSeconds(timezoneHelper1.getSecondsTo359am()));

        //USER AFTER TIMEZONE
        System.out.println("\n USER AHEAD MACHINE");
        TimezoneHelper timezoneHelper2 = new TimezoneHelper("America/Louisville", "America/Los_Angeles");
        total = timezoneHelper2.getSecondsTo1159am();
        System.out.println("Total seconds to 11:59am: " + total);
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until Noon: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper2.getSecondsTo2059pm();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 9pm: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper2.getSecondsTo359am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 4am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper2.getSecondsTo4am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 4am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));
        System.out.println("User LocalTime: " + timezoneHelper2.getUserLocalTime());
        System.out.println("User time til 4am: " + timezoneHelper2.getDateFromAddingSeconds(timezoneHelper2.getSecondsTo359am()));


        //USER BEFORE UTC TIMEZONE
        System.out.println("\n USER BEHIND MACHINE (in UTC)");
        TimezoneHelper timezoneHelper3 = new TimezoneHelper("America/Louisville", "Etc/UTC");
        total = timezoneHelper3.getSecondsTo1159am();
        System.out.println("Total seconds to 11:59am: " + total);
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until Noon: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper3.getSecondsTo2059pm();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 9pm: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper3.getSecondsTo359am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 4am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));

        total = timezoneHelper3.getSecondsTo4am();
        hours = total / 3600;
        mins = (total % 3600) / 60;
        seconds = total % 60;
        System.out.println("Time until 4am: " + String.format("%02d:%02d:%02d", hours, mins, seconds));
        System.out.println("User LocalTime: " + timezoneHelper3.getUserLocalTime());
        System.out.println("User time til 4am: " + timezoneHelper3.getDateFromAddingSeconds(timezoneHelper3.getSecondsTo359am()));
    }

}
