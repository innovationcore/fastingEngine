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
            logger.info(gson.toJson(formParams.toString()));


            String messageId = UUID.randomUUID().toString();
            String participantId = Launcher.dbEngine.getParticipantIdFromPhoneNumber(formParams.get("From").get(0));
            Date date = new Date();
            String messageDirection = "incoming";
            logger.error(gson.toJson(convertMultiToRegularMap(formParams)));



            String insertQuery = "INSERT INTO messages " +
                    "(`message_id`, `participant_uuid`, `TS`, `message_direction`, `message_json`)" +
                    " VALUES ('" + messageId + "', '" +
                    participantId + "' ,'" + date + "', ' " +
                    formParams.get("ToCity").get(0) + "' ,'" + formParams.get("ToZip").get(0) + "', ' " +
                    formParams.get("From").get(0) + "' ,'" + formParams.get("FromCountry").get(0) + "', ' " +
                    formParams.get("FromState").get(0) + "' ,'" + formParams.get("FromCity").get(0) + "', ' " +
                    formParams.get("FromZip").get(0) + "' ,'" + formParams.get("SmsMessageSid").get(0) + "', ' " +
                    formParams.get("SmsSid").get(0) + "' ,'" + formParams.get("SmsStatus").get(0) + "' ,' " +
                    formParams.get("MessageSid").get(0) + "' ,'" + formParams.get("AccountSid").get(0) + "', ' " +
                    formParams.get("Body").get(0) + "' ,'" + formParams.get("NumMedia").get(0) + "', ' " +
                    formParams.get("NumSegments").get(0) + "' ,'" + formParams.get("ApiVersion").get(0) + "', ' " +
                    timestamp + "')";


            /*
            long timestamp = System.currentTimeMillis() / 1000;
            String insertQuery = "INSERT INTO messages " +
                    "(`To`, `ToCountry`, `ToState`, `ToCity`, `ToZip`, " +
                    "`From`, `FromCountry`, `FromState`, `FromCity`, `FromZip`, " +
                    "`SmsMessageSid`, `SmsSid`, `SmsStatus`, `MessageSid`, `AccountSid`, " +
                    "`Body`, `NumMedia`, `NumSegments`, `ApiVersion`, `received_at`)" +
                    " VALUES ('" + formParams.get("To").get(0) + "', '" +
                    formParams.get("ToCountry").get(0) + "' ,'" + formParams.get("ToState").get(0) + "', ' " +
                    formParams.get("ToCity").get(0) + "' ,'" + formParams.get("ToZip").get(0) + "', ' " +
                    formParams.get("From").get(0) + "' ,'" + formParams.get("FromCountry").get(0) + "', ' " +
                    formParams.get("FromState").get(0) + "' ,'" + formParams.get("FromCity").get(0) + "', ' " +
                    formParams.get("FromZip").get(0) + "' ,'" + formParams.get("SmsMessageSid").get(0) + "', ' " +
                    formParams.get("SmsSid").get(0) + "' ,'" + formParams.get("SmsStatus").get(0) + "' ,' " +
                    formParams.get("MessageSid").get(0) + "' ,'" + formParams.get("AccountSid").get(0) + "', ' " +
                    formParams.get("Body").get(0) + "' ,'" + formParams.get("NumMedia").get(0) + "', ' " +
                    formParams.get("NumSegments").get(0) + "' ,'" + formParams.get("ApiVersion").get(0) + "', ' " +
                    timestamp + "')";

            logger.info(insertQuery);

            //record incoming
            Launcher.dbEngine.executeUpdate(insertQuery);

            //send to state machine
            Launcher.restrictedWatcher.incomingText(convertMultiToRegularMap(formParams));
            */

            Map<String,String> responce = new HashMap<>();
            responce.put("status","ok");
            responseString = gson.toJson(responce);

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
