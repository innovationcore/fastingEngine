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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MsgUtils {
    // Find your Account SID and Auth Token at twilio.com/console
    // and set the environment variables. See http://twil.io/secure
    private final String textFromHPM;
    private final String textFromCCW;
    private final String textFromSleep;
    private final String textFromSEC;
    private final Logger logger;
    private final Gson gson;

    public MsgUtils() {
        logger = LoggerFactory.getLogger(MsgUtils.class);
        textFromHPM = Launcher.config.getStringParam("twilio_from_number_HPM");
        textFromCCW = Launcher.config.getStringParam("twilio_from_number_CCW");
        textFromSleep = Launcher.config.getStringParam("twilio_from_number_Sleep");
        textFromSEC = Launcher.config.getStringParam("twilio_from_number_SEC");

        gson = new Gson();
        Twilio.init(Launcher.config.getStringParam("twilio_account_sid"), Launcher.config.getStringParam("twilio_auth_token"));
    }

    public void sendMessage(String textTo, String body, Boolean toAdmin, String study) {
        Message message = null;
        try {
            Map<String, String> participantIds = Launcher.dbEngine.getParticipantIdFromPhoneNumber(textTo);
            String participantId = participantIds.get(study);
            Boolean isMessagingDisabled = Launcher.config.getBooleanParam("disable_messaging");

            if (isMessagingDisabled) {
                logger.warn("Messaging is disabled. Messages will be saved, but not sent.");
            } else {
                String toNumber;
                switch (study) {
                    case "HPM":
                        // you can set the below equal to a Message object for later use
                        if (toAdmin) {
                            toNumber = Launcher.adminPhoneNumber;
                        } else {
                            toNumber = textTo;
                        }
                        Message.creator(
                                        new PhoneNumber(toNumber),
                                        new PhoneNumber(textFromHPM),
                                        body)
                                .create();
                        break;
                    case "CCW":
                        if (toAdmin) {
                            toNumber = Launcher.adminPhoneNumber;
                        } else {
                            toNumber = textTo;
                        }
                        message = Message.creator(
                                        new PhoneNumber(toNumber),
                                        new PhoneNumber(textFromCCW),
                                        body)
                                .create();
                        break;
                    case "Sleep":
                        if (toAdmin) {
                            toNumber = Launcher.adminPhoneNumber;
                        } else {
                            toNumber = textTo;
                        }
                        message = Message.creator(
                                        new PhoneNumber(toNumber),
                                        new PhoneNumber(textFromSleep),
                                        body)
                                .create();
                        break;
                    case "SEC":
                        if (toAdmin) {
                            toNumber = Launcher.adminPhoneNumber;
                        } else {
                            toNumber = textTo;
                        }
                        message = Message.creator(
                                        new PhoneNumber(toNumber),
                                        new PhoneNumber(textFromSEC),
                                        body)
                                .create();
                        break;
                    default:
                        logger.error("MsgUtils: Unknown study for participant");
                }
                Message.Status status = message.getStatus();
                if (status.toString().equals("failed")) {
                    logger.error("Message not sent..." + status);
                }
            }

            String messageId = UUID.randomUUID().toString();
            String messageDirection = "outgoing";

            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String timestamp = format.format(date);

            Map<String, String> messageMap = new HashMap<>();
            String stripped_body = body.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "");
            messageMap.put("Body", stripped_body);
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
        } catch (Exception e) {
            logger.error("Exception occurred trying to send a message...");
            e.printStackTrace();
        }

    }

    public void sendScheduledMessage(String textTo, String body, ZonedDateTime dateTime, Boolean toAdmin, String study) {
        try {
            Map<String, String> participantIds = Launcher.dbEngine.getParticipantIdFromPhoneNumber(textTo);
            String participantId = participantIds.get(study);
            String fromNumber = null;

            switch (study) {
                case "HPM":
                    fromNumber = textFromHPM;
                    break;
                case "CCW":
                    fromNumber = textFromCCW;
                    break;
                case "Sleep":
                    fromNumber = textFromSleep;
                    break;
                case "SEC":
                    fromNumber = textFromSEC;
                    break;
                default:
                    break;
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
                        " VALUES ('" + messageId + "', '00000000-0000-0000-0000-000000000000','" + Launcher.adminPhoneNumber + "','" + fromNumber + "','" + scheduledFor + "','" + json_string + "','" + study + "')";
            }
            Launcher.dbEngine.executeUpdate(insertQuery);
        } catch (Exception e) {
            logger.error("Exception occurred trying to send scheduled message...");
            e.printStackTrace();
        }
    }
}
