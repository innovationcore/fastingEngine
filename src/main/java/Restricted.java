import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Restricted extends RestrictedBase {

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
        }
        return stateJSON;
    }

    @Override
    public boolean stateNotify(String node){
        //logger.info("person_id: " + person_id + " state change: " + node);
        if(stateMap != null) {
            stateMap.put(node, System.currentTimeMillis() / 1000);
        }
        if(startTimestamp == 0) {
            startTimestamp = System.currentTimeMillis() / 1000;
        } else {
            stateJSON = saveStateJSON();
        }

        return true;
    }

    public void restoreSaveState(String saveStateJSON) {

        try{

            Type typeOfHashMap = new TypeToken<Map<String, Map<String,Long>>>() { }.getType();
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

            long saveStartWarnDeadline = timerMap.get("startWarnDeadline");
            long saveStartDeadline = timerMap.get("startDeadline");
            long saveEndWarnDeadline = timerMap.get("endWarnDeadline");
            long saveEndDeadline = timerMap.get("endDeadline");



            switch (lastState) {
                case "initial":
                    //no timers
                    break;
                case "waitStart":
                    //startTimeoutwaitStartTowarnStartCalHandler();
                    break;
                case "warnStartCal":
                    //startTimeoutwarnStartCalTomissedStartCalHandler();
                    break;
                case "startcal":
                    //startTimeoutstartcalTowarnEndCalHandler();
                    break;
                case "missedStartCal":
                    //no timers
                    break;
                case "warnEndCal":
                    //startTimeoutwarnEndCalTomissedEndCalHandler();
                    break;
                case "missedEndCal":
                    break;
                case "endOfEpisode":
                    break;
                default:
                    logger.error("restoreSaveState: Invalid state: " + lastState);
            }

            logger.error("save json: " + saveStateMap.toString());

        } catch (Exception ex) {
            logger.error("restoreSaveState");
        }

    }



}
