
import Protocols.Fasting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;


public class Launcher {


    private static Logger logger;
    private static ConcurrentLinkedQueue<String> incomingQueue = new ConcurrentLinkedQueue<>();


    public static void main(String[] argv) {

        logger = LoggerFactory.getLogger(Launcher.class);

        try {

            Fasting p0 = new Fasting();
            System.out.println(p0.getState());
            p0.receivedStartCal();
            System.out.println(p0.getState());
            p0.receivedEndCal();
            System.out.println(p0.getState());

            p0.receivedStartCal();
            System.out.println(p0.getState());
            //test
            //test2

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}
