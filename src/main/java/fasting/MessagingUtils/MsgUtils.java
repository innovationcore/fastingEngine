package fasting.MessagingUtils;

// Install the Java helper library from twilio.com/docs/java/install

import com.google.gson.Gson;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MsgUtils {
    // Find your Account SID and Auth Token at twilio.com/console
    // and set the environment variables. See http://twil.io/secure
    private final String textFromHPM;
    private final String textFromCCW;
    private final Logger logger;
    private final Gson gson;

    public MsgUtils() {
        logger = LoggerFactory.getLogger(MsgUtils.class);
        textFromHPM = Launcher.config.getStringParam("twilio_from_number_HPM");
        textFromCCW = Launcher.config.getStringParam("twilio_from_number_CCW");
        gson = new Gson();
        Twilio.init(Launcher.config.getStringParam("twilio_account_sid"), Launcher.config.getStringParam("twilio_auth_token"));
    }

    public void sendMessage(String textTo, String body, Boolean toAdmin) {
        String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(textTo);
        String study = Launcher.dbEngine.getStudyFromParticipantId(participantId);
        Boolean isMessagingDisabled = Launcher.config.getBooleanParam("disable_messaging");

        if (isMessagingDisabled) {
            logger.warn("Messaging is disabled. Messages will be saved, but not sent.");
        } else {
            String toNumber;
            if (study.equals("HPM")) {
                // you can set the below equal to a Message object for later use
                if (toAdmin) { toNumber = Launcher.adminPhoneNumber; }
                else {toNumber = textTo; }
                Message.creator(
                                new PhoneNumber(toNumber),
                                new PhoneNumber(textFromHPM),
                                body)
                        .create();
            } else if (study.equals("CCW")) {
                if (toAdmin) { toNumber = Launcher.adminPhoneNumber; }
                else {toNumber = textTo; }
                Message.creator(
                                new PhoneNumber(toNumber),
                                new PhoneNumber(textFromCCW),
                                body)
                        .create();
            }
        }

        String messageId = UUID.randomUUID().toString();
        String messageDirection = "outgoing";

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
                messageDirection + "', '" + json_string + "', '" + study + "')";

        if (toAdmin) {
            insertQuery = "INSERT INTO messages " +
                    "(message_uuid, participant_uuid, TS, message_direction, message_json, study)" +
                    " VALUES ('" + messageId + "', '00000000-0000-0000-0000-000000000000' ,'" + timestamp + "', '" +
                    messageDirection + "', '" + json_string + "', '" + study + "')";
        }
        Launcher.dbEngine.executeUpdate(insertQuery);

    }

    public void sendScheduledMessage(String textTo, String body, ZonedDateTime dateTime, Boolean toAdmin) {
        String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(textTo);
        String study = Launcher.dbEngine.getStudyFromParticipantId(participantId);
        String fromNumber = null;

        if (study.equals("HPM")) {
            fromNumber = textFromHPM;
        } else if (study.equals("CCW")) {
            fromNumber = textFromCCW;
        }

        String messageId = UUID.randomUUID().toString();
        String scheduledFor = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(dateTime);

        Map<String, String> messageMap = new HashMap<>();
        String stripped_body = body.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "");
        messageMap.put("Body", stripped_body);
        String json_string = gson.toJson(messageMap);
        logger.info("Message queued for: " + scheduledFor + ", Message: " + json_string);

        // if message to Matt don't insert into database
        String insertQuery = "INSERT INTO queued_messages " +
                "(message_uuid, participant_uuid, toNumber, fromNumber, scheduledFor, message_json, study)" +
                " VALUES ('" + messageId + "', '" + participantId + "','" + textTo + "','" + fromNumber + "','" + scheduledFor + "','" + json_string + "','" + study + "')";

        if (toAdmin) {
            insertQuery = "INSERT INTO queued_messages " +
                    "(message_uuid, participant_uuid, toNumber, fromNumber, scheduledFor, message_json, study)" +
                    " VALUES ('" + messageId + "', '00000000-0000-0000-0000-000000000000','" + Launcher.adminPhoneNumber + "','" + fromNumber + "','" + scheduledFor + "','" + json_string + "','"+study+"')";
        }
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

                Map<String,String> response = new HashMap<>();
                response.put("status","ok");
                responseString = gson.toJson(response);
                return responseString;

            } else {
                Map<String,String> response = new HashMap<>();
                response.put("status","error");
                response.put("status_desc","participant not found");
                responseString = gson.toJson(response);
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
