package fasting.Protocols;

import fasting.TimeUtils.TimezoneHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.twilio.rest.api.v2010.account.Message;
import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class Restricted extends RestrictedBase {
    private Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    //id, participant_uuid, phone_number, participant_type
    private Map<String, String> participantMap;
    private Map<String,Long> stateMap;
    private long startTimestamp = 0;
    private TimezoneHelper TZHelper;

    public String stateJSON;

    private Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(Restricted.class.getName());

    public Restricted(String participant_uuid) {

    }

    public Restricted(Map<String, String> participantMap) {
        this.gson = new Gson();
        this.participantMap = participantMap;
        this.stateMap = new HashMap<>();

        // this initializes the user's and machine's timezone
        this.TZHelper = new TimezoneHelper(participantMap.get("time_zone"), TimeZone.getDefault().getID());
        restoreSaveState();

        new Thread(){
            public void run(){
                try {
                    while (!getState().toString().equals("endOfEpisode")) {

                        if(startTimestamp > 0) {
                            stateJSON = saveStateJSON();
                            Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                            //logger.info(stateJSON);
                        }

                        Thread.sleep(900000); // 900000 = 15 mins
                    }
                } catch (Exception ex) {
                    logger.error("protocols.Restricted Thread");
                    logger.error(ex.getMessage());
                }
            }
        }.start();

    }

    public void incomingText(Map<String,String> incomingMap) {
        try {
            State state = getState();
            switch (state) {
                case initial:
                    //no timers
                    break;
                case waitStart:
                    if (isDayoff(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"STARTCAL\" and \"ENDCAL\" today.");
                    } else if (isEndCal(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), TZHelper.yesterdaysDate()+ ": " + pickRandomEndCalMessage());
                        //Launcher.msgUtils.sendMessage(participantMap.get("number"), "No \"STARTCAL\" received yet today. Please send us your \"STARTCAL\" first, followed by your \"ENDCAL\".");
                    } else if(isStartCal(incomingMap.get("Body"))) {
                        receivedStartCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for" +
                                                    " the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case warnStartCal:
                    if (isDayoff(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"STARTCAL\" and \"ENDCAL\" today.");
                    } else if (isEndCal(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), TZHelper.yesterdaysDate()+ ": " + pickRandomEndCalMessage());
                        //Launcher.msgUtils.sendMessage(participantMap.get("number"), "No \"STARTCAL\" received yet today. Please send us your \"STARTCAL\" first, followed by your \"ENDCAL\".");
                    } else if(isStartCal(incomingMap.get("Body"))) {
                        receivedStartCal();
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for" +
                                                    " the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case startcal:
                    if (isDayoff(incomingMap.get("Body"))) {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Got it, no TRE today! Thank you for telling us. Please still let us know your \"ENDCAL\" today. ");
                    } else if(isStartCal(incomingMap.get("Body"))){
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "You've already started consuming calories for the day. Text \"ENDCAL\" when you finish your TRE today.");
                    } else if(isEndCal(incomingMap.get("Body"))) {
                        if (TZHelper.isBetween3AMand3PM()){
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. Try again in the evening. Text 270-402-2214 if you want to receive a call about how to manage TRE safely.");
                        } else {
                            receivedEndCal();
                        }
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for" +
                                                    " the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case missedStartCal:
                    String missedStartCalMessage =  participantMap.get("participant_uuid") + " missedStartCal unexpected message";
                    logger.warn(missedStartCalMessage);
                    break;
                case warnEndCal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        if (TZHelper.isBetween3AMand3PM()){
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. Try again in the evening. Text 270-402-2214 if you want to receive a call about how to manage TRE safely.");
                        } else {
                            receivedEndCal();
                        }
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for" +
                                                    " the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case endcal:
                    if(isEndCal(incomingMap.get("Body"))) {
                        if (TZHelper.isBetween3AMand3PM()){
                            Launcher.msgUtils.sendMessage(participantMap.get("number"), "We don't recommend that you end calories this early in the day. Try again in the evening. Text 270-402-2214 if you want to receive a call about how to manage TRE safely.");
                        } else {
                            receivedEndCal();
                        }
                    } else {
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), "Your text was not understood. Please send \"STARTCAL\" when you begin calories for" +
                                                    " the day; \"ENDCAL\" when you are done with calories for the day.");
                    }
                    break;
                case missedEndCal:
                    String missedEndCalMessage = participantMap.get("participant_uuid") + " missedEndCal unexpected message";
                    logger.warn(missedEndCalMessage);
                    break;
                case endOfEpisode:
                    String endOfEpisodeMessage = participantMap.get("participant_uuid") + " endOfEpisode unexpected message";
                    logger.warn(endOfEpisodeMessage);
                    break;
                default:
                    logger.error("stateNotify: Invalid state: " + getState());
            }


        } catch (Exception ex) {
            logger.error("incomingMessage");
            logger.error(ex.getMessage());
        }
    }

    private boolean isDayoff(String messageBody) {
        boolean isDayoff = false;
        try {
            if(messageBody.toLowerCase().contains("dayoff")) {
                isDayoff = true;
            } else {
                isDayoff = false;
            }

        } catch (Exception ex) {
            logger.error("isDayoff()");
            logger.error(ex.getMessage());
        }
        return isDayoff;
    }

    private boolean isStartCal(String messageBody) {
        boolean isStart = false;
        try {

            if(messageBody.toLowerCase().contains("startcal")) {
                //Launcher.msgUtils.sendMessage(participantMap.get("number"), "startcal accepted");
                isStart = true;
            } else {
                isStart = false;
            }

        } catch (Exception ex) {
            logger.error("isStartCal()");
            logger.error(ex.getMessage());
        }
        return isStart;
    }

    private boolean isEndCal(String messageBody) {
        boolean isEnd = false;
        try {

            if(messageBody.toLowerCase().contains("endcal")) {
                isEnd = true;
            } else {
                isEnd = false;
            }

        } catch (Exception ex) {
            logger.error("isStartCal()");
            logger.error(ex.getMessage());
        }
        return isEnd;
    }

    public String saveStateJSON() {
        String stateJSON = null;
        try {
            Map<String,Long> timerMap = new HashMap<>();
            timerMap.put("stateIndex", Long.valueOf(getState().ordinal()));
            timerMap.put("startTime", startTimestamp);
            timerMap.put("currentTime", System.currentTimeMillis() / 1000); //unix seconds
            timerMap.put("startDeadline", Long.valueOf(getStartDeadline()));
            timerMap.put("startWarnDeadline", Long.valueOf(getStartWarnDeadline()));
            timerMap.put("endDeadline", Long.valueOf(getEndDeadline()));
            timerMap.put("endWarnDeadline", Long.valueOf(getEndWarnDeadline()));
            timerMap.put("endOfEpisodeDeadline", Long.valueOf(getEndOfEpisodeDeadline()));

            Map<String,Map<String,Long>> stateSaveMap = new HashMap<>();
            stateSaveMap.put("history",stateMap);
            stateSaveMap.put("timers", timerMap);

            stateJSON = gson.toJson(stateSaveMap);

        } catch (Exception ex) {
            logger.error("saveStateJSON");
            logger.error(ex.getMessage());

        }
        return stateJSON;
    }

    @Override
    public boolean stateNotify(String state){

        //save change to state log
        logState(state);


        if(stateMap != null) {
            stateMap.put(state, System.currentTimeMillis() / 1000);
        }
        if(startTimestamp == 0) {
            startTimestamp = System.currentTimeMillis() / 1000;
        } else {
            stateJSON = saveStateJSON();
        }


        switch (State.valueOf(state)) {
            case initial:
                //no timers
                break;
            case waitStart:
                //setting warn timer
                int startWarnDiff =  TZHelper.getSecondsTo1159am();  //timeToD1T1159am();
                if(startWarnDiff <= 0) {
                    startWarnDiff = 300;
                }
                setStartWarnDeadline(startWarnDiff);
                String waitStartMessage = participantMap.get("participant_uuid") + " created state machine: warnStart timeout " + TZHelper.getDateFromAddingSeconds(startWarnDiff);
                logger.warn(waitStartMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case warnStartCal:
                //set start fail timer
                setStartDeadline(TZHelper.getSecondsTo359am()); // timeToD2359am());
                String warnStartCalMessage = participantMap.get("participant_uuid") + " please submit startcal: startdeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo359am());
                logger.warn(warnStartCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case startcal:
                //set warn and end
                // In the CSV with responses they don't have anything for sending startcal
                int secondsTo2059pm = TZHelper.getSecondsTo2059pm();
                // if after 9pm, don't immediately send warnEndCal message. Wait some time so user has time to respond
                if (secondsTo2059pm <= 0) {
                    secondsTo2059pm += 300; // add 5 minutes
                }
                setEndWarnDeadline(secondsTo2059pm); //timeToD19pm());
                String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: endwarndeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo2059pm());
                logger.info(startCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case missedStartCal:
                String missedStartCalMessage = "We haven't heard from you in a while. Remember to text \"STARTCAL\" when your calories start " +
                                                "in the morning and \"ENDCAL\" when your calories finish at night! Let us know if you need help.";
                                                //participantMap.get("participant_uuid") + " no startcal was recorded for today.";
                logger.warn(missedStartCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), missedStartCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case warnEndCal:
                //set end for end
                // maybe this should be sent 9 hours after startcal
                setEndDeadline(TZHelper.getSecondsTo359am());
                String warnEndCalMessage = participantMap.get("first_name") +  ", we haven't heard from you. Remember to text \"ENDCAL\" when you go calorie free.";//participantMap.get("participant_uuid") + " please submit endcal: enddeadline timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo359am()); // timeToD2359am());
                Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage);
                logger.warn(warnEndCalMessage);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case endcal:
                String endCalMessage = pickRandomEndCalMessage();
                logger.info(endCalMessage);
                Launcher.msgUtils.sendMessage(participantMap.get("number"), endCalMessage);
                resetNoEndCal();
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case missedEndCal:
            // TRF ended too late messages should be sent after 11pm, maybe reminder
                String missedEndCalMessage = participantMap.get("participant_uuid") + " no endcal was recorded for today.";
                logger.warn(missedEndCalMessage);
                logNoEndCal();
                if (getDaysWithoutEndCal() >= 2){
                    String missed2EndCals = "We haven't heard from you in a while. Text our study team at 270-402-2214 if you're struggling to stick with the time-restricted eating.";
                    Launcher.msgUtils.sendMessage(participantMap.get("number"), missed2EndCals);
                }
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            case endOfEpisode:
                //set restart one minute after other timeouts
                setEndOfEpisodeDeadline(TZHelper.getSecondsTo4am());// timeToD2359am() + 60);
                String endOfEpisode = participantMap.get("participant_uuid") + " end of episode timeout " + TZHelper.getDateFromAddingSeconds(TZHelper.getSecondsTo4am());
                logger.info(endOfEpisode);
                //save state info
                stateJSON = saveStateJSON();
                Launcher.dbEngine.uploadSaveState(stateJSON, participantMap.get("participant_uuid"));
                break;
            default:
                logger.error("stateNotify: Invalid state: " + state);
        }

        return true;
    }

    public Map<String, Map<String,Long>> getSaveStateMap() {
        Map<String, Map<String,Long>> saveStateMap = gson.fromJson(stateJSON,typeOfHashMap);
        return saveStateMap;
    }

    // returns true if state was found, false if not
    public boolean restoreSaveState() {
        boolean didFindState = false;
        try{
            String saveStateJSON = Launcher.dbEngine.getSaveState(participantMap.get("participant_uuid"));

            if (!saveStateJSON.equals("")){
                Map<String, Map<String,Long>> saveStateMap = gson.fromJson(saveStateJSON,typeOfHashMap);

                Map<String,Long> historyMap = saveStateMap.get("history");
                Map<String,Long> timerMap = saveStateMap.get("timers");

                int stateIndex = (int) timerMap.get("stateIndex").longValue();

                String stateName = State.values()[stateIndex].toString();

                // List<String> sortedHistoryList = saveStateMap.get("history").entrySet().stream()
                //         .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                //         .map(Map.Entry::getKey)
                //         .collect(Collectors.toList());

                // String lastState = sortedHistoryList.get(0);

                //long lastStateStartTime = historyMap.get(stateName);
                // long saveStartTime = timerMap.get("startTime");
                long saveCurrentTime = timerMap.get("currentTime");
                // long diffStateTimer = saveCurrentTime - lastStateStartTime;

                // long saveStartWarnDeadline = timerMap.get("startWarnDeadline");
                // long saveStartDeadline = timerMap.get("startDeadline");
                // long saveEndWarnDeadline = timerMap.get("endWarnDeadline");
                // long saveEndDeadline = timerMap.get("endDeadline");

                // //set all timers
                // setStartWarnDeadline((int)saveStartWarnDeadline);
                // setStartDeadline((int)saveStartDeadline);
                // setEndWarnDeadline((int)saveEndWarnDeadline);
                // setEndDeadline((int)saveEndDeadline);

                // if same day (<4am) below is correct
                // if next day (>=4am) need to reset to waitStart

                boolean isSameDay = TZHelper.isSameDay(saveCurrentTime);
                System.out.println("\nisSameDay: " + isSameDay);

                switch (State.valueOf(stateName)) {
                    case initial:
                        //no timers
                        break;
                    case waitStart:
                        //resetting warn timer
                        int startWarnDiff =  TZHelper.getSecondsTo1159am();  //timeToD1T1159am();
                        if(startWarnDiff <= 0) {
                            startWarnDiff = 300;
                        }
                        setStartWarnDeadline(startWarnDiff);
                        receivedWaitStart(); // initial to waitStart
                        String waitStartMessage = participantMap.get("participant_uuid") + " created state machine: warnStart timeout " + TZHelper.getDateFromAddingSeconds(startWarnDiff);
                        logger.warn(waitStartMessage);
                        break;
                    case warnStartCal:
                        //reset startDeadline
                        int secondsTo359am0 = TZHelper.getSecondsTo359am();
                        if (secondsTo359am0 < 0) {
                            secondsTo359am0 = 0;
                        }
                        setStartDeadline(secondsTo359am0); // timeToD2359am());
                        receivedWarnStartCal(); // initial to warnStartCal
                        String warnStartCalMessage = participantMap.get("participant_uuid") + " please submit startcal: startdeadline timeout " + TZHelper.getDateFromAddingSeconds(secondsTo359am0);
                        logger.warn(warnStartCalMessage);
                        break;
                    case startcal:
                        //reset endWarnDeadline
                        int secondsTo2059pm = TZHelper.getSecondsTo2059pm();
                        if (secondsTo2059pm < 0) {
                            secondsTo2059pm = 0;
                        }
                        setEndWarnDeadline(secondsTo2059pm); //timeToD19pm());
                        String startCalMessage = participantMap.get("participant_uuid") + " thanks for sending startcal: endwarndeadline timeout " + TZHelper.getDateFromAddingSeconds(secondsTo2059pm);
                        logger.info(startCalMessage);
                        receivedStartCal();
                        break;
                    case missedStartCal:
                        //no timers
                        break;
                    case warnEndCal:
                        //reset endDeadline
                        int secondsTo359am1 = TZHelper.getSecondsTo359am();
                        if (secondsTo359am1 < 0) {
                            secondsTo359am1 = 0;
                        }
                        setEndDeadline(secondsTo359am1);
                        String warnEndCalMessage = participantMap.get("first_name") +  ", we haven't heard from you. Remember to text \"ENDCAL\" when you go calorie free.";
                        Launcher.msgUtils.sendMessage(participantMap.get("number"), warnEndCalMessage);
                        logger.warn(warnEndCalMessage);
                        recievedWarnEndCal();
                        break;
                    case missedEndCal:
                        break;
                    case endOfEpisode:
                        // reset endOfEpisodeDeadline
                        int secondsTo4am = TZHelper.getSecondsTo4am();
                        if (secondsTo4am < 0) {
                            secondsTo4am = 0;
                        }
                        setEndOfEpisodeDeadline(secondsTo4am);
                        // quickest path to endOfEpisode, move it but don't save it
                        receivedStartCal();
                        receivedEndCal();
                        String endOfEpisode = participantMap.get("participant_uuid") + " end of episode timeout " + TZHelper.getDateFromAddingSeconds(secondsTo4am);
                        logger.info(endOfEpisode);
                        break;
                    default:
                        logger.error("restoreSaveState: Invalid state: " + stateName);
                }
                didFindState = true;
            }
            else {
            }
        // else no save state found

        } catch (Exception ex) {
            logger.error("restoreSaveState");
            logger.error(ex.getMessage());
        }
        return didFindState;
    }

    private void logNoEndCal(){
        Launcher.dbEngine.updateDaysWithoutEndCal(participantMap.get("participant_uuid"));
    }

    private void resetNoEndCal(){
        Launcher.dbEngine.resetDaysWithoutEndCal(participantMap.get("participant_uuid"));
    }

    private int getDaysWithoutEndCal(){
        return Launcher.dbEngine.getDaysWithoutEndCal(participantMap.get("participant_uuid"));
    }

    public void logState(String state) {
        if(gson != null) {
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("state",state);
            String json_string = gson.toJson(messageMap);

            String insertQuery = "INSERT INTO state_log " +
                    "(participant_uuid, TS, log_json)" +
                    " VALUES ('" + participantMap.get("participant_uuid") + "', " +
                    "GETUTCDATE(), '" + json_string +
                    "')";

            Launcher.dbEngine.executeUpdate(insertQuery);
        }
    }

    public String pickRandomEndCalMessage() {
         // this is a list of responses for when a participant sends endcal
        final List<String> endCalMessages = Collections.unmodifiableList(
        new ArrayList<String>() {{
            add("Thanks! You are now running calorie free!");
            add("Thanks for your response. You can do it!");
            add("Thanks, [NAME]! Here we go! Make sure your kitchen is in lock down!");
            add("Message received. Good luck!");
            add("Great. Now lock those kitchen cabinets!");
            add("Thanks. You are now nibble free.");
            add("Message received. Now stay out of the kitchen. Doctor's orders!");
            add("Thanks. Now go bolt the refrigerator door shut.");
            add("Got it. Now put your tummy to sleep until the morning!");
            add("Muchas gracias. Now go put your feet up. You've earned it!");
        }});
        Random rand = new Random();
        int rnd = new Random().nextInt(endCalMessages.size());
        String message = endCalMessages.get(rnd);
        if (message.contains("[NAME]")) {
            message = message.replace("[NAME]", participantMap.get("first_name"));
        }
        return message;
    }

}
