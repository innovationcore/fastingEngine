package fasting.MessagingUtils;

import fasting.Launcher;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageSchedulerExecutor {

    public static void startWatcher() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(MessageSchedulerExecutor::checkDatabase, 0, 5, TimeUnit.SECONDS);
    }

    private static void checkDatabase() {
        try {
            Launcher.dbEngine.checkQueuedMessageDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
