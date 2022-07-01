import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Testing {

    private Logger logger;

    public Testing() {
        logger = LoggerFactory.getLogger(Testing.class);
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
            while(!(p0.getState() == RestrictedBase.State.endOfEpisode)) {
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
