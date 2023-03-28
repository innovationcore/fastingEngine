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

    //check local
    //curl --header "X-Auth-webapi.API-key:1234" "http://localhost:8081/api/checkmydatabase"

    //check remote
    //curl --header "X-Auth-webapi.API-key:1234" "http://[linkblueid].cs.uky.edu:8081/api/checkmydatabase"
    //application/x-www-form-urlencoded
    @POST
    @Path("/incoming")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    public Response incomingText(MultivaluedMap<String, String> formParams) {

        String responseString;
        try {

            String messageId = UUID.randomUUID().toString();
            String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(formParams.get("From").get(0));

            if (participantId != null) {
                String messageDirection = "incoming";
                //logger.error(gson.toJson(convertMultiToRegularMap(formParams)));
                

                Map<String, String> formsMap = convertMultiToRegularMap(formParams);
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
                String enrollment = Launcher.dbEngine.getEnrollmentUUID(participantId);
                String enrollmentName = Launcher.dbEngine.getEnrollmentName(enrollment);
                if (enrollmentName.equals("TRE")) {
                    Launcher.restrictedWatcher.incomingText(participantId, formsMap);
                } else if (enrollmentName.equals("Baseline")) {
                    Launcher.baselineWatcher.incomingText(participantId, formsMap);
                } else if (enrollmentName.equals("Control")) {
                    Launcher.controlWatcher.incomingText(participantId, formsMap);
                } else {
                    logger.error("Text from participant not enrolled in any protocol");
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
    @Path("/get-valid-next-states/{participant_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextStates(@PathParam("participant_uuid") String participantId) {
        String responseString;
        try {

            if (!participantId.equals("")) {
                // this returns a comma delimited list as a string
                String validNextStates = "";
                String protocol = Launcher.dbEngine.getProtocolFromParticipantId(participantId);
                if (protocol.equals("TRE")) {
                    validNextStates = Launcher.restrictedWatcher.getValidNextStates(participantId);
                }
                else if (protocol.equals("Control")) {
                    validNextStates = Launcher.controlWatcher.getValidNextStates(participantId);
                }
                else if (protocol.equals("Baseline")) {
                    validNextStates = Launcher.baselineWatcher.getValidNextStates(participantId);
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

    @GET
    @Path("/next-state")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveToNextState(@QueryParam("participantUUID") String participantId,
                                    @QueryParam("toState") String nextState,
                                    @QueryParam("time") String time) {
        String responseString = "";
        try {

            if (participantId != null) {
                //send to state machine
                String newState = "";
                String protocol = Launcher.dbEngine.getProtocolFromParticipantId(participantId);
                if (protocol.equals("TRE")) {
                    newState = Launcher.restrictedWatcher.moveToState(participantId, nextState, time);
                }
                else if (protocol.equals("Control")) {
                    newState = Launcher.controlWatcher.moveToState(participantId, nextState, time);
                }
                else if (protocol.equals("Baseline")) {
                    newState = Launcher.baselineWatcher.moveToState(participantId, nextState, time);
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
            if (participantId != null) {
                //send to state machine
                String enrollment = Launcher.dbEngine.getEnrollmentUUID(participantId);
                String enrollmentName = Launcher.dbEngine.getEnrollmentName(enrollment);
                if (enrollmentName.equals("TRE")) {
                    Launcher.restrictedWatcher.resetStateMachine(participantId);
                    Launcher.dailyMessageWatcher.resetStateMachine(participantId);
                } else if (enrollmentName.equals("Baseline")) {
                    Launcher.baselineWatcher.resetStateMachine(participantId);
                    Launcher.weeklyMessageWatcher.resetStateMachine(participantId);
                } else if (enrollmentName.equals("Control")) {
                    Launcher.controlWatcher.resetStateMachine(participantId);
                    Launcher.weeklyMessageWatcher.resetStateMachine(participantId);
                } else {
                    logger.error("Cannot reset machine, participant not in an active protocol.");
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
