package fasting;

import fasting.Configs.Config;
import fasting.Configs.FileConfig;
import fasting.Database.DBEngine;
import fasting.MessagingUtils.MessageSchedulerExecutor;
import fasting.MessagingUtils.MsgUtils;
import fasting.Protocols.CCW.CCW_Baseline.CCW_BaselineWatcher;
import fasting.Protocols.CCW.CCW_Control.CCW_ControlWatcher;
import fasting.Protocols.CCW.CCW_DailyMessage.CCW_DailyMessageWatcher;
import fasting.Protocols.CCW.CCW_Restricted.CCW_RestrictedWatcher;
import fasting.Protocols.CCW.CCW_WeeklyMessage.CCW_WeeklyMessageWatcher;
import fasting.Protocols.HPM.HPM_Baseline.HPM_BaselineWatcher;
import fasting.Protocols.HPM.HPM_Control.HPM_ControlWatcher;
import fasting.Protocols.HPM.HPM_DailyMessage.HPM_DailyMessageWatcher;
import fasting.Protocols.HPM.HPM_Restricted.HPM_RestrictedWatcher;
import fasting.Protocols.HPM.HPM_WeeklyMessage.HPM_WeeklyMessageWatcher;
import fasting.Protocols.Testing;
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
import java.util.HashMap;
import java.util.Map;


public class Launcher {
    private static Logger logger;
    public static Config config;
    public static DBEngine dbEngine;
    public static MsgUtils msgUtils;
    public static MessageSchedulerExecutor queuedMessageWatcher;
    public static HPM_RestrictedWatcher HPM_RestrictedWatcher;
    public static HPM_ControlWatcher HPM_ControlWatcher;
    public static HPM_BaselineWatcher HPM_BaselineWatcher;
    public static HPM_WeeklyMessageWatcher HPM_WeeklyMessageWatcher;
    public static HPM_DailyMessageWatcher HPM_DailyMessageWatcher;
    public static CCW_RestrictedWatcher CCW_RestrictedWatcher;
    public static CCW_ControlWatcher CCW_ControlWatcher;
    public static CCW_BaselineWatcher CCW_BaselineWatcher;
    public static CCW_WeeklyMessageWatcher CCW_WeeklyMessageWatcher;
    public static CCW_DailyMessageWatcher CCW_DailyMessageWatcher;
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

            // start watching the queued messages database
            new MessageSchedulerExecutor().startWatcher();

            //testing
//            TimezoneHelper TZHelper = new TimezoneHelper("America/Louisville","Etc/UTC");

            //start HPM protocols
            HPM_RestrictedWatcher = new HPM_RestrictedWatcher();
            HPM_ControlWatcher = new HPM_ControlWatcher();
            HPM_BaselineWatcher = new HPM_BaselineWatcher();
            HPM_WeeklyMessageWatcher = new HPM_WeeklyMessageWatcher(); // only for Baseline/Control
            HPM_DailyMessageWatcher = new HPM_DailyMessageWatcher(); // only for TRE

            //start CCW protocols
            CCW_RestrictedWatcher = new CCW_RestrictedWatcher();
            CCW_ControlWatcher = new CCW_ControlWatcher();
            CCW_BaselineWatcher = new CCW_BaselineWatcher();
            CCW_WeeklyMessageWatcher = new CCW_WeeklyMessageWatcher(); // only for Baseline/Control
            CCW_DailyMessageWatcher = new CCW_DailyMessageWatcher(); // only for TRE

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
