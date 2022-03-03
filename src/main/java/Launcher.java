
import Protocols.Restricted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;


public class Launcher {


    private static Logger logger;
    private static ConcurrentLinkedQueue<String> incomingQueue = new ConcurrentLinkedQueue<>();


    public static void main(String[] argv) {

        logger = LoggerFactory.getLogger(Launcher.class);

        try {

            Restricted p0 = new Restricted("0");
            logger.info(p0.getState().toString());
            p0.receivedStartCal();
            logger.info(p0.getState().toString());
            p0.receivedEndCal();
            logger.info(p0.getState().toString());

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}
