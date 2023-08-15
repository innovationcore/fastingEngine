package fasting;

import fasting.Configs.Config;
import fasting.Configs.FileConfig;
import fasting.Database.DBEngine;
import fasting.MessagingUtils.MsgUtils;
import fasting.Protocols.HPM_DailyMessage.HPM_DailyMessageWatcher;
import fasting.Protocols.Testing;
import fasting.Protocols.HPM_Restricted.HPM_RestrictedWatcher;
import fasting.Protocols.HPM_Control.HPM_ControlWatcher;
import fasting.Protocols.HPM_Baseline.HPM_BaselineWatcher;
import fasting.Protocols.HPM_WeeklyMessage.HPM_WeeklyMessageWatcher;
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
    public static HPM_RestrictedWatcher HPM_RestrictedWatcher;
    public static HPM_ControlWatcher HPM_ControlWatcher;
    public static HPM_BaselineWatcher HPM_BaselineWatcher;
    public static HPM_WeeklyMessageWatcher HPM_WeeklyMessageWatcher;
    public static HPM_DailyMessageWatcher HPM_DailyMessageWatcher;
    public static Testing testing;
    public static TimezoneHelper TZHelper;

    public static void main(String[] argv) {

        logger = LoggerFactory.getLogger(Launcher.class);

        try {
            if (argv.length == 0) {
                System.out.println("Please specify config file path as first parameter.");
                System.exit(-1);
            }
            //get config info
            String configPath = argv[0];
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
//            TimezoneHelper TZHelper = new TimezoneHelper("America/Louisville","Etc/UTC");
//            System.out.println(TZHelper.getSecondsToFriday5pm());
//            System.out.println(TZHelper.getSecondsTo5pm());



            //start protocols
            HPM_RestrictedWatcher = new HPM_RestrictedWatcher();
            HPM_ControlWatcher = new HPM_ControlWatcher();
            HPM_BaselineWatcher = new HPM_BaselineWatcher();
            HPM_WeeklyMessageWatcher = new HPM_WeeklyMessageWatcher(); // only for Baseline/Control
            HPM_DailyMessageWatcher = new HPM_DailyMessageWatcher(); // only for TRE

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static void startServer() {

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

            File configFile = Paths.get(configName).toFile();
            FileConfig config;
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
