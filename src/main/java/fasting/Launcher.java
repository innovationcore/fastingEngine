package fasting;

import fasting.Configs.Config;
import fasting.Configs.FileConfig;
import fasting.Database.DBEngine;
import fasting.MessagingUtils.MsgUtils;
import fasting.Protocols.RestrictedWatcher;
import fasting.TimeUtils.TimezoneHelper;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.*;


public class Launcher {

    private static Logger logger;
    public static Config config;
    public static DBEngine dbEngine;

    public static MsgUtils msgUtils;

    public static RestrictedWatcher restrictedWatcher;

    public static void main(String[] argv) {

        logger = LoggerFactory.getLogger(Launcher.class);

        try {

            //get config info
            String configPath = "config.ini";
            Map<String, Object> fileConfigMap;
            fileConfigMap = initConfigMap(configPath);
            config = new Config(fileConfigMap);

            //init db engine
            dbEngine = new DBEngine();

            //init message utils
            msgUtils = new MsgUtils();
            //msgUtils.sendMessage("+18592702334", "Yo man");
            //protocols.Testing testing = new protocols.Testing();

            //testing.testWorking();
            //testing.testNoStart();
            //testing.testNoEnd();
            //testing.saveStateDemo();

            //Embedded HTTP initialization
            startServer();

            //test timezone helper
            // TimezoneHelper timezoneHelper = new TimezoneHelper("America/Louisville", "America/Louisville");
            // System.out.println("Time until Noon: " + timezoneHelper.getSecondsTo1159am());
            // System.out.println("Time until 9pm: " + timezoneHelper.getSecondsTo2059pm());
            // System.out.println("Time until Midnight: " +timezoneHelper.getSecondsTo2359pm());
            // System.out.println("Time until 4am: " +timezoneHelper.getSecondsTo359am());
            // System.out.println("User LocalTime: " +timezoneHelper.getUserLocalTime());
            // System.out.println("Unix time in seconds: " + timezoneHelper.getUserLocalTimeSeconds());
            // System.out.println("User time til 4am: " + timezoneHelper.getDateFromAddingSeconds(timezoneHelper.getSecondsTo359am()));


            //start protocols
            restrictedWatcher = new RestrictedWatcher();
            // restrictedWatcher.testWorking();


        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static void startServer() throws IOException {

        final ResourceConfig rc = new ResourceConfig()
                .packages("fasting.Webapi");

        System.out.println("Starting Web Server...");
        int web_port = config.getIntegerParam("web_port",9000);
        URI BASE_URI = UriBuilder.fromUri("http://0.0.0.0/").port(web_port).build();
        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc);

        try {
            httpServer.start();
            System.out.println("Web Server Started...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String,Object> initConfigMap(String configName) {
        Map<String, Object> configParams = null;
        try {

            configParams = new HashMap<>();

            String configFileString = configName;

            File configFile = Paths.get(configFileString).toFile();
            FileConfig config = null;
            if (configFile.isFile()) {
                //config.fasting.Config
                config = new FileConfig(configFile.getAbsolutePath());
                configParams = config.getConfigMap();

            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        return configParams;
    }



}
