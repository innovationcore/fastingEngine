package fasting.Webapi;

import com.google.gson.Gson;
import fasting.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.ws.rs.core.RequestBody;


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
                Launcher.restrictedWatcher.incomingText(participantId, formsMap);

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


    // @POST
    // @Path("/next-state")
    // @Consumes({MediaType.APPLICATION_JSON})
    // @Produces(MediaType.APPLICATION_JSON)
    // public Response moveToNextState(RequestBody requestBody) {
    //     String responseString;
    //     try {
    //         String messageId = UUID.randomUUID().toString();
    //         String participantId = requestBody.participantUUID;

    //         if (participantId != null) {
    //             //send to state machine
    //             Launcher.restrictedWatcher.moveToNextState(participantId);

    //             Map<String,String> responce = new HashMap<>();
    //             responce.put("status","ok");
    //             responseString = gson.toJson(responce);

    //         } else {
    //             Map<String,String> responce = new HashMap<>();
    //             responce.put("status","error");
    //             responce.put("status_desc","participant not found");
    //             responseString = gson.toJson(responce);
    //         }

    //     } catch (Exception ex) {

    //         StringWriter sw = new StringWriter();
    //         ex.printStackTrace(new PrintWriter(sw));
    //         String exceptionAsString = sw.toString();
    //         ex.printStackTrace();
    //         logger.error("incomingText");
    //         logger.error(exceptionAsString);

    //         return Response.status(500).entity(exceptionAsString).build();
    //     }
    //     //return accesslog data
    //     return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    // }

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
