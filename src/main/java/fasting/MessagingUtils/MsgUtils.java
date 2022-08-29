package fasting.MessagingUtils;

// Install the Java helper library from twilio.com/docs/java/install

import com.google.gson.Gson;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import fasting.Launcher;
import fasting.Webapi.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Handler;

public class MsgUtils {
    // Find your Account SID and Auth Token at twilio.com/console
    // and set the environment variables. See http://twil.io/secure
    private String account_sid;
    private String auth_token;
    private String textFrom;
    private Logger logger;
    private Gson gson;

    public MsgUtils() {
        logger = LoggerFactory.getLogger(MsgUtils.class);
        textFrom = Launcher.config.getStringParam("twilio_from_number");
        gson = new Gson();
        //account_sid = Launcher.config.getStringParam("twilio_account_sid");
        //auth_token = Launcher.config.getStringParam("twilio_auth_token");
        Twilio.init(Launcher.config.getStringParam("twilio_account_sid"), Launcher.config.getStringParam("twilio_auth_token"));
    }

    public void sendMessage(String textTo, String body) {
        // Message message = Message.creator(
        //                 new com.twilio.type.PhoneNumber(textTo),
        //                 new com.twilio.type.PhoneNumber(textFrom),
        //                 body)
        //         .create();

        String messageId = UUID.randomUUID().toString();
        String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(textTo);
        String messageDirection = "outgoing";

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = format.format(date);

        // for some reason this was uploading as EDT
        // String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        // SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, new Locale("en", "USA"));
        // String date = simpleDateFormat.format(new Date());

        Map<String,String> messageMap = new HashMap<>();
        messageMap.put("Body",body);
        String json_string = gson.toJson(messageMap);

        String insertQuery = "INSERT INTO messages " +
                "(message_uuid, participant_uuid, TS, message_direction, message_json)" +
                " VALUES ('" + messageId + "', '" +
                participantId + "' ,'" + timestamp + "', '" +
                messageDirection + "', '" + json_string +
                "')";

        logger.info(insertQuery);

        Launcher.dbEngine.executeUpdate(insertQuery);

    }
}
