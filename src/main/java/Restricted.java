import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Restricted extends RestrictedBase {

private String person_id;
private static final Logger logger = LoggerFactory.getLogger(Restricted.class.getName());

    public Restricted(String person_id) {
        this.person_id = person_id;
    }

    @Override
    public boolean stateNotify(String node){
        logger.info("person_id: " + person_id + " state change: " + node);
        return true;
    }

}
