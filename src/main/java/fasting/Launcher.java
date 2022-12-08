package fasting;

import fasting.Configs.Config;
import fasting.Configs.FileConfig;
import fasting.Database.DBEngine;
import fasting.MessagingUtils.MsgUtils;
import fasting.Protocols.Testing;
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
import java.util.*;


public class Launcher {

    private static Logger logger;
    public static Config config;
    public static DBEngine dbEngine;
    public static MsgUtils msgUtils;
    public static RestrictedWatcher restrictedWatcher;
    public static Testing testing;
    public static TimezoneHelper TZHelper;

    public static void main(String[] argv) {

        logger = LoggerFactory.getLogger(Launcher.class);

        try {

            //get config info
            String configPath = "config.ini";
            Map<String, Object> fileConfigMap;
            fileConfigMap = initConfigMap(configPath);
            config = new Config(fileConfigMap);
            testing = new Testing();

            //init db engine
            dbEngine = new DBEngine();

            //init message utils
            msgUtils = new MsgUtils();

            //Embedded HTTP initialization
            startServer();

            //testing
            // TimezoneHelper TZHelper = new TimezoneHelper("America/Phoenix","Etc/UTC");
            // System.out.println(TZHelper.determineGoodFastTime(1670261100, 1670379900));
            // System.out.println(TZHelper.isBetween12AMand4AM());
            

    
            //start protocols
            restrictedWatcher = new RestrictedWatcher();



        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static void startServer() throws IOException {

        final ResourceConfig rc = new ResourceConfig()
                .packages("fasting.Webapi");

        logger.info("Starting Web Server...");
        int web_port = config.getIntegerParam("web_port",9000);
        URI BASE_URI = UriBuilder.fromUri("http://0.0.0.0/").port(web_port).build();
        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc);

        try {
            httpServer.start();
            logger.info("Web Server Started...");
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
} //main class
