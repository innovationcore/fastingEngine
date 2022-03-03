
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Launcher {

    private static Logger logger;

    public static void main(String[] argv) {

        logger = LoggerFactory.getLogger(Launcher.class);

        try {

            //Create dummy person
            Restricted p0 = new Restricted("0");
            //set short deadline for cal end
            p0.setDeadline(3);
            //send start cal
            p0.receivedStartCal();

            //loop until end
            while(!(p0.getState() == RestrictedBase.State.endOfEpisode)) {
                logger.info(p0.getState().toString());
                Thread.sleep(1000);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}
