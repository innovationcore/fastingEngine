import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Restricted extends RestrictedBase {
    private Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();

    private String person_id;
    private Map<String,Long> stateMap;

    private long startTimestamp = 0;

    public String stateJSON;

    private Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(Restricted.class.getName());

    public Restricted(String person_id) {
        this.person_id = person_id;
        this.stateMap = new HashMap<>();
        this.gson = new Gson();

        new Thread(){
            public void run(){
                try {
                    while (!getState().toString().equals("endOfEpisode")) {

                        if(startTimestamp > 0) {
                            stateJSON = saveStateJSON();
                            //logger.info(stateJSON);
                        }

                        Thread.sleep(1000);
                    }
                } catch (Exception ex) {
                    logger.error("Restricted Thread: " + ex.toString());
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    logger.error(pw.toString());
                }
            }
        }.start();

    }

    public String saveStateJSON() {
        String stateJSON = null;
        try {

            Map<String,Long> timerMap = new HashMap<>();
            timerMap.put("stateIndex", Long.valueOf(getState().ordinal()));
            timerMap.put("startTime", startTimestamp);
            timerMap.put("currentTime", System.currentTimeMillis() / 1000);
            timerMap.put("startDeadline", Long.valueOf(getStartDeadline()));
            timerMap.put("startWarnDeadline", Long.valueOf(getStartWarnDeadline()));
            timerMap.put("endDeadline", Long.valueOf(getEndDeadline()));
            timerMap.put("endWarnDeadline", Long.valueOf(getEndWarnDeadline()));

            Map<String,Map<String,Long>> stateSaveMap = new HashMap<>();
            stateSaveMap.put("history",stateMap);
            stateSaveMap.put("timers", timerMap);

            stateJSON = gson.toJson(stateSaveMap);


        } catch (Exception ex) {
            logger.error("saveStateJSON: " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(pw.toString());

        }
        return stateJSON;
    }

    @Override
    public boolean stateNotify(String state){

        logger.info("\t\t\t\t person_id: " + person_id + " state change: " + state);

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
                break;
            case warnStartCal:
                logger.warn("\t\t SEND STARTCAL WARNING TEXT");
                break;
            case startcal:
                logger.info("\t\t SEND STARTCAL THANK YOU TEXT");
                break;
            case missedStartCal:
                logger.error("\t\t SEND STARTCAL FAILURE TEXT");
                break;
            case warnEndCal:
                logger.warn("\t\t SEND ENDCAL WARNING TEXT");
                break;
            case endcal:
                logger.info("\t\t SEND ENDCAL THANK YOU TEXT");
                break;
            case missedEndCal:
                logger.error("\t\t SEND ENDCAL FAILURE TEXT");
                break;
            case endOfEpisode:
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

    public void restoreSaveState(String saveStateJSON) {

        try{

            //Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();
            Map<String, Map<String,Long>> saveStateMap = gson.fromJson(saveStateJSON,typeOfHashMap);

            Map<String,Long> historyMap = saveStateMap.get("history");
            Map<String,Long> timerMap = saveStateMap.get("timers");

            List<String> sortedHistoryList = saveStateMap.get("history").entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            String lastState = sortedHistoryList.get(0);

            long lastStateStartTime = historyMap.get(lastState);
            long saveStartTime = timerMap.get("startTime");
            long saveCurrentTime = timerMap.get("currentTime");
            long diffStateTimer = saveCurrentTime - lastStateStartTime;

            long saveStartWarnDeadline = timerMap.get("startWarnDeadline");
            long saveStartDeadline = timerMap.get("startDeadline");
            long saveEndWarnDeadline = timerMap.get("endWarnDeadline");
            long saveEndDeadline = timerMap.get("endDeadline");

            //set all timers
            setStartWarnDeadline((int)saveStartWarnDeadline);
            setStartDeadline((int)saveStartDeadline);
            setEndWarnDeadline((int)saveEndWarnDeadline);
            setEndDeadline((int)saveEndDeadline);

            switch (State.valueOf(lastState)) {
                case initial:
                    //no timers
                    break;
                case waitStart:
                    //change startWarnDeadline
                    //startTimeoutwaitStartTowarnStartCalHandler();
                    long newStartWarnDeadline = saveStartWarnDeadline - diffStateTimer;
                    setStartWarnDeadline((int)newStartWarnDeadline);
                    receivedWaitStart();
                    break;
                case warnStartCal:
                    //change startDeadline
                    //startTimeoutwarnStartCalTomissedStartCalHandler();
                    long newsStartDeadline = saveStartDeadline - diffStateTimer;
                    setStartDeadline((int)newsStartDeadline);
                    receivedWarnStartCal();
                    break;
                case startcal:
                    //change endWarnDeadline
                    //startTimeoutstartcalTowarnEndCalHandler();
                    long newEndWarnDeadline = saveEndWarnDeadline - diffStateTimer;
                    setEndWarnDeadline((int)newEndWarnDeadline);
                    receivedStartcal();
                    break;
                case missedStartCal:
                    //no timers
                    break;
                case warnEndCal:
                    //change endDeadline
                    //startTimeoutwarnEndCalTomissedEndCalHandler();
                    long newEndDeadline = saveEndDeadline - diffStateTimer;
                    setEndDeadline((int)newEndDeadline);
                    recievedWarnEndCal();
                    break;
                case missedEndCal:
                    break;
                case endOfEpisode:
                    break;
                default:
                    logger.error("restoreSaveState: Invalid state: " + lastState);
            }

            //logger.error("save json: " + saveStateMap.toString());

        } catch (Exception ex) {
            logger.error("restoreSaveState");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(pw.toString());
        }

    }

}
