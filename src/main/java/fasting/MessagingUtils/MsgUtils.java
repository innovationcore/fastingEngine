package fasting.MessagingUtils;

// Install the Java helper library from twilio.com/docs/java/install

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import fasting.Launcher;
import fasting.Webapi.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgUtils {
    // Find your Account SID and Auth Token at twilio.com/console
    // and set the environment variables. See http://twil.io/secure
    private String account_sid;
    private String auth_token;
    private String textFrom;
    private Logger logger;

    public MsgUtils() {
        logger = LoggerFactory.getLogger(MsgUtils.class);
        textFrom = Launcher.config.getStringParam("twilio_from_number");
        //account_sid = Launcher.config.getStringParam("twilio_account_sid");
        //auth_token = Launcher.config.getStringParam("twilio_auth_token");
        Twilio.init(Launcher.config.getStringParam("twilio_account_sid"), Launcher.config.getStringParam("twilio_auth_token"));
    }

    public void sendMessage(String textTo, String body) {

        Message message = Message.creator(
                        new com.twilio.type.PhoneNumber(textTo),
                        new com.twilio.type.PhoneNumber(textFrom),
                        body)
                .create();

        long timestamp = System.currentTimeMillis() / 1000;
        String insertQuery = "INSERT INTO messages " +
                "(`To`, `From`, `Body`, `received_at`)" +
                " VALUES ('" + textTo + "', '" +  textFrom + "', " + "'" + body + "', '" + timestamp + "')";

        logger.info(insertQuery);

        Launcher.dbEngine.executeUpdate(insertQuery);

    }
}
