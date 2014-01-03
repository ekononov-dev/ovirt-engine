package org.ovirt.engine.core.notifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.ovirt.engine.core.notifier.utils.NotificationProperties;

/**
 * Main class of event notification service. Initiate the service and handles termination signals
 */
public class Notifier {
    private static final Logger log = Logger.getLogger(Notifier.class);

    private static ScheduledExecutorService notifyScheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledExecutorService monitorScheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Command line argument, that tells Notifier to validate properties only (it exits after validation)
     */
    private static final String ARG_VALIDATE = "validate";

    /**
     * Initializes logging configuration
     */
    private static void initLogging() {
        String cfgFile = System.getProperty("log4j.configuration");
        if (StringUtils.isNotBlank(cfgFile)) {
            try {
                URL url = new URL(cfgFile);
                LogManager.resetConfiguration();
                DOMConfigurator.configure(url);
            } catch (FactoryConfigurationError | MalformedURLException ex) {
                System.out.println("Cannot configure logging: " + ex.getMessage());
            }
        }
    }

    /**
     * @param args
     *            command line arguments, if {@code args[0]} contains {@code validate}, then only validation is
     *            executed and after that process ends. Otherwise process will continue to execute in standard way
     */
    public static void main(String[] args) {
        NotificationProperties prop = null;
        initLogging();

        try {
            prop = NotificationProperties.getInstance();
            prop.validate();
        } catch (Exception ex) {
            log.error("Failed to parse configuration.", ex);
            // print error also to stderr to be seen in console during service startup
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        if (args != null && args.length > 0 && ARG_VALIDATE.equals(args[0])) {
            // command line argument to validate only entered
            System.exit(0);
        }

        NotifierSignalHandler handler = new NotifierSignalHandler();
        handler.addScheduledExecutorService(notifyScheduler);
        handler.addScheduledExecutorService(monitorScheduler);
        Runtime.getRuntime().addShutdownHook(handler);

        try {
            NotificationService notificationService = new NotificationService(prop);
            EngineMonitorService engineMonitorService = new EngineMonitorService(prop);

            // add notification service to scheduler with its configurable interval
            handler.addServiceHandler(notifyScheduler.scheduleWithFixedDelay(notificationService,
                    1,
                    prop.getLong(NotificationProperties.INTERVAL_IN_SECONDS),
                    TimeUnit.SECONDS));

            // add engine monitor service to scheduler with its configurable interval
            handler.addServiceHandler(monitorScheduler.scheduleWithFixedDelay(engineMonitorService,
                    1,
                    prop.getLong(NotificationProperties.ENGINE_INTERVAL_IN_SECONDS),
                    TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("Failed to run the event notification service. ", e);
            // flag exit code to calling script after threads shut down.
            System.exit(1);
        }
    }

    /**
     * Class designed to handle a proper shutdown in case of an external signal which was registered was caught by the
     * program.
     */
    public static class NotifierSignalHandler extends Thread {
        private List<ScheduledFuture<?>> serviceHandler = new ArrayList<ScheduledFuture<?>>();
        private List<ScheduledExecutorService> scheduler = new ArrayList<ScheduledExecutorService>();

        @Override
        public void run() {
            log.info("Preparing for shutdown after receiving signal " );
            if (serviceHandler.size() > 0) {
                for (ScheduledFuture<?> scheduled : serviceHandler) {
                    scheduled.cancel(true);
                }
            }
            if (scheduler.size() > 0) {
                for (ScheduledExecutorService executer : scheduler) {
                    executer.shutdown();
                }
            }
            log.info("Event Notification service was shutdown");
        }

        public void addScheduledExecutorService(ScheduledExecutorService scheduler) {
            this.scheduler.add(scheduler);
        }

        public void addServiceHandler(ScheduledFuture<?> serviceHandler) {
            this.serviceHandler.add(serviceHandler);
        }
    }
}

