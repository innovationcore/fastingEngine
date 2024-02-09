package fasting.Webapi;

import com.google.gson.Gson;
import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


@Path("/sms")
public class API {

    @Inject
    private javax.inject.Provider<org.glassfish.grizzly.http.server.Request> request;

    private Gson gson;
    private Logger logger = LoggerFactory.getLogger(API.class);

    public API() {
        gson = new Gson();
    }

    @POST
    @Path("/incoming")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    public Response incomingText(MultivaluedMap<String, String> formParams) {

        String responseString;
        try {

            String messageId = UUID.randomUUID().toString();
            Map<String, String> participantIds = Launcher.dbEngine.getParticipantIdFromPhoneNumber(formParams.get("From").get(0));
            String textBody = formParams.get("Body").get(0);

            if (!participantIds.isEmpty()) {
                String messageDirection = "incoming";
                //logger.error(gson.toJson(convertMultiToRegularMap(formParams)));
                

                Map<String, String> formsMap = convertMultiToRegularMap(formParams);
                String json_string = gson.toJson(formsMap);

                // get HPM, CCW, Sleep or a combo
                List<String> studies = new ArrayList<>();
                for (Map.Entry<String, String> entry : participantIds.entrySet()) {
                    studies.add(Launcher.dbEngine.getStudyFromParticipantId(entry.getValue())); // this returns HPM or CCW (check for sleep below)
                }

                if (studies.contains("Sleep")) {
                    studies.remove("Sleep");
                }
                String study;
                if (textBody.toLowerCase().contains("sleep") || textBody.toLowerCase().contains("wake")){
                    study = "Sleep";
                } else {
                    study = studies.get(0);
                }

                String participantId = participantIds.get(study);

                String insertQuery = "INSERT INTO messages " +
                        "(message_uuid, participant_uuid, TS, message_direction, study, message_json)" +
                        " VALUES ('" + messageId + "', '" +
                        participantId + "' , GETUTCDATE(), '" +
                        messageDirection + "', '" + study + "', '" + json_string +
                        "')";

                //record incoming
                Launcher.dbEngine.executeUpdate(insertQuery);

                //send to state machine
                Map<String, String> protocol = Launcher.dbEngine.getProtocolFromParticipantId(participantId);
                if (study.equals("HPM")) {
                    if (protocol.get(study).equals("TRE")) {
                        Launcher.HPM_RestrictedWatcher.incomingText(participantId, formsMap);
                    } else if (protocol.get(study).equals("Baseline")) {
                        Launcher.HPM_BaselineWatcher.incomingText(participantId, formsMap);
                    } else if (protocol.get(study).equals("Control")) {
                        Launcher.HPM_ControlWatcher.incomingText(participantId, formsMap);
                    }
                } else if (study.equals("CCW")){
                    if (protocol.get(study).equals("TRE")) { // TODO problem with study key not there
                        Launcher.CCW_RestrictedWatcher.incomingText(participantId, formsMap);
                    } else if (protocol.get(study).equals("Baseline")) {
                        Launcher.CCW_BaselineWatcher.incomingText(participantId, formsMap);
                    } else if (protocol.get(study).equals("Control")) {
                        Launcher.CCW_ControlWatcher.incomingText(participantId, formsMap);
                    }
                } else if (study.equals("Sleep")) {
                    Launcher.sleepWatcher.incomingText(participantId, formsMap);
                } else {
                    logger.error("Text from participant not enrolled in any HPM, CCW, or Sleep protocol");
                }

                Map<String,String> responce = new HashMap<>();
                responce.put("status","ok");
                responseString = gson.toJson(responce);

            } else {
                Map<String,String> responce = new HashMap<>();
                responce.put("status","error");
                responce.put("status_desc","participant not found");
                responseString = gson.toJson(responce);
            }

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("incomingText");
            logger.error(exceptionAsString);

            return Response.status(500).entity(exceptionAsString).build();
        }
        //return accesslog data
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessLog() {
        String responseString = "{}";
        try {

            //get remote ip address from request
            String remoteIP = request.get().getRemoteAddr();
            //get the timestamp of the request
            long access_ts = System.currentTimeMillis();
            logger.info("IP: " + remoteIP + " Timestamp: " + access_ts);

            Map<String,String> responseMap = new HashMap<>();
            responseMap.put("ip", remoteIP);
            responseMap.put("timestamp", String.valueOf(access_ts));

            responseString = gson.toJson(responseMap);


        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        //return accesslog data
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("/get-valid-next-states/{participant_uuid}/{participant_study}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextStates(@PathParam("participant_uuid") String participantId,
                                  @PathParam("participant_study") String participantStudy) {
        String responseString;
        try {
            if (!participantId.equals("")) {
                // this returns a comma delimited list as a string
                String validNextStates = "";
                Map<String, String> protocol = Launcher.dbEngine.getProtocolFromParticipantId(participantId);
                if (participantStudy.equals("HPM")) {
                    if (protocol.get(participantStudy).equals("TRE")) {
                        validNextStates = Launcher.HPM_RestrictedWatcher.getValidNextStates(participantId);
                    } else if (protocol.get(participantStudy).equals("Control")) {
                        validNextStates = Launcher.HPM_ControlWatcher.getValidNextStates(participantId);
                    } else if (protocol.get(participantStudy).equals("Baseline")) {
                        validNextStates = Launcher.HPM_BaselineWatcher.getValidNextStates(participantId);
                    }
                } else if (participantStudy.equals("CCW")) {
                    if (protocol.get(participantStudy).equals("TRE")) {
                        validNextStates = Launcher.CCW_RestrictedWatcher.getValidNextStates(participantId);
                    } else if (protocol.get(participantStudy).equals("Control")) {
                        validNextStates = Launcher.CCW_ControlWatcher.getValidNextStates(participantId);
                    } else if (protocol.get(participantStudy).equals("Baseline")) {
                        validNextStates = Launcher.CCW_BaselineWatcher.getValidNextStates(participantId);
                    }
                } else if (participantStudy.equals("Sleep")){
                    validNextStates = Launcher.sleepWatcher.getValidNextStates(participantId);
                }

                Map<String,String> response = new HashMap<>();
                response.put("status","ok");
                response.put("valid_states", validNextStates);
                responseString = gson.toJson(response);

            } else {
                Map<String,String> response = new HashMap<>();
                response.put("status","error");
                response.put("status_desc","participant not found");
                responseString = gson.toJson(response);
            }

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("getNextStates");
            logger.error(exceptionAsString);
            return Response.status(500).entity(exceptionAsString).build();
        }
        //return state moved to
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/next-state")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveToNextState(MultivaluedMap<String, String> data) {

        String participantId = data.get("participantUUID").get(0);
        String nextState = data.get("toState").get(0);
        String time = data.get("time").get(0);
        String study = data.get("study").get(0);
        String responseString = "";
        try {

            if (participantId != null) {
                //send to state machine
                String newState = "";
                Map<String, String> protocol = Launcher.dbEngine.getProtocolFromParticipantId(participantId);
                if (study.equals("HPM")) {
                    if (protocol.get(study).equals("TRE")) {
                        newState = Launcher.HPM_RestrictedWatcher.moveToState(participantId, nextState, time);
                    } else if (protocol.get(study).equals("Control")) {
                        newState = Launcher.HPM_ControlWatcher.moveToState(participantId, nextState, time);
                    } else if (protocol.get(study).equals("Baseline")) {
                        newState = Launcher.HPM_BaselineWatcher.moveToState(participantId, nextState, time);
                    }
                } else if (study.equals("CCW")) {
                    if (protocol.get(study).equals("TRE")) {
                        newState = Launcher.CCW_RestrictedWatcher.moveToState(participantId, nextState, time);
                    }
                    else if (protocol.get(study).equals("Control")) {
                        newState = Launcher.CCW_ControlWatcher.moveToState(participantId, nextState, time);
                    }
                    else if (protocol.get(study).equals("Baseline")) {
                        newState = Launcher.CCW_BaselineWatcher.moveToState(participantId, nextState, time);
                    }
                } else if (study.equals("Sleep")) {
                    newState = Launcher.sleepWatcher.moveToState(participantId, nextState, time);
                }

                Map<String,String> response = new HashMap<>();
                response.put("status","ok");
                response.put("moved_to_state", newState);
                responseString = gson.toJson(response);

            } else {
                Map<String,String> response = new HashMap<>();
                response.put("status","error");
                response.put("status_desc","participant not found");
                responseString = gson.toJson(response);
            }

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("moveToNextState");
            logger.error(exceptionAsString);

            return Response.status(500).entity(exceptionAsString).build();
        }
        //return state moved to
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    @POST
    @Path("/reset-machine")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetStateMachine(MultivaluedMap<String, String> data) {

        String responseString;
        try {
            String participantId = data.get("uuid").get(0);
            String study = data.get("study").get(0);
            if (participantId != null) {
                //send to state machine
                Map<String, String> protocol = Launcher.dbEngine.getProtocolFromParticipantId(participantId);
                if (study.equals("HPM")) {
                    if (protocol.get(study).equals("TRE")) {
                        Launcher.HPM_RestrictedWatcher.resetStateMachine(participantId);
                        Launcher.HPM_DailyMessageWatcher.resetStateMachine(participantId);
                    } else if (protocol.get(study).equals("Baseline")) {
                        Launcher.HPM_BaselineWatcher.resetStateMachine(participantId);
                        Launcher.HPM_WeeklyMessageWatcher.resetStateMachine(participantId);
                    } else if (protocol.get(study).equals("Control")) {
                        Launcher.HPM_ControlWatcher.resetStateMachine(participantId);
                        Launcher.HPM_WeeklyMessageWatcher.resetStateMachine(participantId);
                    } else {
                        logger.error("Cannot reset machine, participant not in an active protocol.");
                    }
                } else if (study.equals("CCW")) {
                    if (protocol.get(study).equals("TRE")) {
                        Launcher.CCW_RestrictedWatcher.resetStateMachine(participantId);
                        Launcher.CCW_DailyMessageWatcher.resetStateMachine(participantId);
                    } else if (protocol.get(study).equals("Baseline")) {
                        Launcher.CCW_BaselineWatcher.resetStateMachine(participantId);
                        Launcher.CCW_WeeklyMessageWatcher.resetStateMachine(participantId);
                    } else if (protocol.get(study).equals("Control")) {
                        Launcher.CCW_ControlWatcher.resetStateMachine(participantId);
                        Launcher.CCW_WeeklyMessageWatcher.resetStateMachine(participantId);
                    } else {
                        logger.error("Cannot reset machine, participant not in an active protocol.");
                    }
                } else if (study.equals("Sleep")) {
                    Launcher.sleepWatcher.resetStateMachine(participantId);
                }

                Map<String,String> response = new HashMap<>();
                response.put("status", "ok");
                responseString = gson.toJson(response);

            } else {
                Map<String,String> response = new HashMap<>();
                response.put("status","error");
                response.put("status_desc","participant not found");
                responseString = gson.toJson(response);
            }

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("resetStateMachine");
            logger.error(exceptionAsString);

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    @POST
    @Path("/update-timezone")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTimeZone(MultivaluedMap<String, String> data) {

        String responseString;
        try {
            String participantId = data.get("uuid").get(0);
            String tz = data.get("tz").get(0);
            String study = data.get("study").get(0);
            if (participantId != null) {
                //send to state machine
                Map<String, String> protocol = Launcher.dbEngine.getProtocolFromParticipantId(participantId);
                if (study.equals("HPM")) {
                    if (protocol.get(study).equals("TRE")) {
                        Launcher.HPM_RestrictedWatcher.updateTimeZone(participantId, tz);
                        Launcher.HPM_DailyMessageWatcher.updateTimeZone(participantId, tz);
                    } else if (protocol.get(study).equals("Baseline")) {
                        Launcher.HPM_BaselineWatcher.updateTimeZone(participantId, tz);
                        Launcher.HPM_WeeklyMessageWatcher.updateTimeZone(participantId, tz);
                    } else if (protocol.get(study).equals("Control")) {
                        Launcher.HPM_ControlWatcher.updateTimeZone(participantId, tz);
                        Launcher.HPM_WeeklyMessageWatcher.updateTimeZone(participantId, tz);
                    }
                } else if (study.equals("CCW")) {
                    if (protocol.get(study).equals("TRE")) {
                        Launcher.CCW_RestrictedWatcher.updateTimeZone(participantId, tz);
                        Launcher.CCW_DailyMessageWatcher.updateTimeZone(participantId, tz);
                    } else if (protocol.get(study).equals("Baseline")) {
                        Launcher.CCW_BaselineWatcher.updateTimeZone(participantId, tz);
                        Launcher.CCW_WeeklyMessageWatcher.updateTimeZone(participantId, tz);
                    } else if (protocol.get(study).equals("Control")) {
                        Launcher.CCW_ControlWatcher.updateTimeZone(participantId, tz);
                        Launcher.CCW_WeeklyMessageWatcher.updateTimeZone(participantId, tz);
                    }
                } else if (study.equals("Sleep")) {
                    Launcher.sleepWatcher.updateTimeZone(participantId, tz);
                }

                Map<String,String> response = new HashMap<>();
                response.put("status", "ok");
                responseString = gson.toJson(response);

            } else {
                Map<String,String> response = new HashMap<>();
                response.put("status","error");
                response.put("status_desc","participant not found");
                responseString = gson.toJson(response);
            }

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();
            logger.error("updateTimeZone");
            logger.error(exceptionAsString);

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    private Map<String, String> convertMultiToRegularMap(MultivaluedMap<String, String> m) {
        Map<String, String> map = new HashMap<String, String>();
        if (m == null) {
            return map;
        }
        for (Map.Entry<String, List<String>> entry : m.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (String s : entry.getValue()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(s);
            }
            map.put(entry.getKey(), sb.toString());
        }
        return map;
    }
}
