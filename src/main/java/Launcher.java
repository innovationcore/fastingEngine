
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Launcher {

    private static Logger logger;


    public static void main(String[] argv) {

        logger = LoggerFactory.getLogger(Launcher.class);

        try {

            Testing testing = new Testing();
            //testing.testWorking();
            //testing.testNoStart();
            //testing.testNoEnd();
            //testing.saveStateDemo();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}
