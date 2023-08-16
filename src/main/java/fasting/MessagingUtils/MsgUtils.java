package fasting.MessagingUtils;

// Install the Java helper library from twilio.com/docs/java/install

import com.google.gson.Gson;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

public class MsgUtils {
    // Find your Account SID and Auth Token at twilio.com/console
    // and set the environment variables. See http://twil.io/secure
    private String textFrom;
    private Logger logger;
    private Gson gson;

    public MsgUtils() {
        logger = LoggerFactory.getLogger(MsgUtils.class);
        textFrom = Launcher.config.getStringParam("twilio_from_number");
        gson = new Gson();
        Twilio.init(Launcher.config.getStringParam("twilio_account_sid"), Launcher.config.getStringParam("twilio_auth_token"));
    }

    public void sendMessage(String textTo, String body) {
        Boolean isMessagingDisabled = Launcher.config.getBooleanParam("disable_messaging");
        if (isMessagingDisabled) {
            logger.warn("Messaging is disabled. Messages will be saved, but not sent.");
        } else {
            Message message = Message.creator(
                        new PhoneNumber(textTo),
                        new PhoneNumber(textFrom),
                        body)
                .create();
        }

        String messageId = UUID.randomUUID().toString();
        String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(textTo);
        String messageDirection = "outgoing";
        String study = Launcher.dbEngine.getStudyFromParticipantId(participantId);

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = format.format(date);

        Map<String,String> messageMap = new HashMap<>();
        String stripped_body = body.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "");
        messageMap.put("Body",stripped_body);
        String json_string = gson.toJson(messageMap);
        logger.info(json_string);

        String insertQuery = "INSERT INTO messages " +
                "(message_uuid, participant_uuid, TS, message_direction, message_json, study)" +
                " VALUES ('" + messageId + "', '" +
                participantId + "' ,'" + timestamp + "', '" +
                messageDirection + "', '" + json_string + "', '"+ study +"')";

        Launcher.dbEngine.executeUpdate(insertQuery);

    }

    public String fakeIncomingMessage(Map<String, String> formsMap, String phone_number) {
        String responseString = "Error";
        try {

            String messageId = UUID.randomUUID().toString();
            String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(phone_number);

            if (participantId != null) {
                String messageDirection = "incoming";

                String json_string = gson.toJson(formsMap);

                String insertQuery = "INSERT INTO messages " +
                        "(message_uuid, participant_uuid, TS, message_direction, message_json)" +
                        " VALUES ('" + messageId + "', '" +
                        participantId + "' , GETUTCDATE(), '" +
                        messageDirection + "', '" + json_string +
                        "')";

                //record incoming
                Launcher.dbEngine.executeUpdate(insertQuery);

                //send to state machine
                Launcher.HPM_RestrictedWatcher.incomingText(participantId, formsMap);

                Map<String,String> responce = new HashMap<>();
                responce.put("status","ok");
                responseString = gson.toJson(responce);
                return responseString;

            } else {
                Map<String,String> responce = new HashMap<>();
                responce.put("status","error");
                responce.put("status_desc","participant not found");
                responseString = gson.toJson(responce);
                return responseString;
            }

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("fakeIncomingText");
            logger.error(exceptionAsString);

        }
        return responseString;
    }
}
