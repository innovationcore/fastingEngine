package fasting.Protocols.DailyMessage;

import com.google.gson.Gson;
import fasting.Launcher;
import fasting.Protocols.Restricted.Restricted;
import fasting.TimeUtils.TimezoneHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DailyMessage extends DailyMessageBase {
    private final Map<String, String> participantMap;
    private long startTimestamp = 0;
    private final TimezoneHelper TZHelper;
    private boolean isRestoring;
    private final Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(DailyMessage.class.getName());

    public DailyMessage(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.isRestoring = false;

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());

        new Thread(){
            public void run(){
                try {
                    while (!getState().toString().equals("endProtocol")) {

                        String currentTimezone = Launcher.dbEngine.getParticipantTimezone(participantMap.get("participant_uuid"));
                        if (!participantMap.get("time_zone").equals(currentTimezone) && !currentTimezone.equals("")){
                            participantMap.put("time_zone", currentTimezone);
                            TZHelper.setUserTimezone(currentTimezone);
                        }

                        Thread.sleep(900000); // 900000 = 15 mins

                    }
                } catch (Exception ex) {
                    logger.error("protocols.Baseline Thread");
                    logger.error(ex.getMessage());
                }
            }
        }.start();

    }

    @Override
    public boolean stateNotify(String state){

        logState(state);

        if(startTimestamp == 0) {
            startTimestamp = System.currentTimeMillis() / 1000;
        }

        switch (State.valueOf(state)) {
            case initial:
                //no timers
                break;
            case waitDay:
                // 24 Hour timer set
                int seconds = TZHelper.getSecondsTo5pm();
                setTimeout24Hours(seconds);
                String waitDayMessage = participantMap.get("participant_uuid") + " created state machine: timeout24Hours " + TZHelper.getDateFromAddingSeconds(seconds);
                logger.info(waitDayMessage);
                break;
            case sendDailyMessage:
                String dailyMessage = getRandomDailyMessage();
                logger.info(dailyMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), dailyMessage);
                break;
            case endProtocol:
                logger.warn(participantMap.get("participant_uuid") + " is not longer in protocol.");
                break;
            default:
                logger.error("stateNotify: Invalid state: " + state);
        }

        return true;
    }

    public void restoreSaveState() {
        try {
            this.isRestoring = true;
            // get current protocol
            String protocolNameDB = Launcher.dbEngine.getProtocolFromParticipantId(participantMap.get("participant_uuid"));

            if (protocolNameDB.equals("TRE")) {
                Map<String, Restricted> restrictedMap = Launcher.restrictedWatcher.getRestrictedMap();
                while (restrictedMap.isEmpty()) {
                    Thread.sleep(500);
                    restrictedMap = Launcher.restrictedWatcher.getRestrictedMap();
                }
                String currentState = restrictedMap.get(participantMap.get("participant_uuid")).getState().toString();
                if (!currentState.equals("endProtocol")) {
                    int seconds = TZHelper.getSecondsToFridayNoon();
                    setTimeout24Hours(seconds);
                    receivedWaitDay();
                }

            } else {
                // move to endProtocol
                receivedWaitDay();
                receivedEndProtocol();
            }
            this.isRestoring = false;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logState(String state) {
        if(gson != null) {
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            messageMap.put("protocol", "DailyMessage");
            if (this.isRestoring) {
                messageMap.put("restored","true");
            }

            String json_string = gson.toJson(messageMap);

            String insertQuery = "INSERT INTO state_log " +
                    "(participant_uuid, TS, log_json)" +
                    " VALUES ('" + participantMap.get("participant_uuid") + "', " +
                    "GETUTCDATE(), '" + json_string +
                    "')";

            Launcher.dbEngine.executeUpdate(insertQuery);
        }
    }

    public String getRandomDailyMessage(){
        final List<String> dailyMessages = Collections.unmodifiableList(
                new ArrayList<String>() {{
                    add("Remember the golden rule: Once you end calories today, you need to be calorie free until tomorrow!");
                    add("Develop a plan to help you stay calorie free overnight.");
                    add("Hide snacks to curb late night nibbles.");
                    add("If you struggle sticking with it, distract yourself with a hobby or a good book!");
                    add("Drink fizzy water to help with the cravings.");
                    add("Alcohol is cheating!  Calorie free means alcohol free too.");
                    add("Try drinking herbal tea for a tasty way to be calorie free.");
                    add("Kiss your partner to help you stay calorie free.  It won't help you, but it will make them feel yummy!");
                    add("Enjoy black coffee or tea with no sugar and stay calorie free. Phew!");
                    add("Top tip: drink fizzy water from a wine glass if you are craving a glass of vino.");
                    add("[NAME], is it a bummer to go calorie free?  Tip: Cheer yourself up by remembering the last time you laughed so hard you almost peed in your pants.");
                    add("Try knitting to help you stay calorie free tonight. You may feel hungry but you'll get a scarf along the way!");
                    add("Need a distraction to help you stay calorie free tonight? Do some household chores after you put your calories to bed.");
                    add("Your own mind is your greatest motivator. You CAN do this!");
                    add("Hey TRE Queen, need a distraction? Try singing your favorite song. Now who's having the time of her life?");
                    add("It's not just on game shows where it helps to phone a friend. Chatting with friends helps distract you AND relax at the end of the day.");
                    add("Remember to text ENDCAL when you have your last calorie tonight!");
                }});
        int rnd = new Random().nextInt(dailyMessages.size());
        String message = dailyMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        return message;
    }

} // class
