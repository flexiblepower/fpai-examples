package org.flexiblepower.dummy.energyapp;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.flexiblepower.dummy.energyapp.DummyEnergyApp.Config;
import org.flexiblepower.efi.EfiControllerManager;
import org.flexiblepower.messaging.Connection;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.messaging.MessageHandler;
import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = Endpoint.class, immediate = true)
public class DummyEnergyApp implements EfiControllerManager {
    private static final Logger log = LoggerFactory.getLogger(DummyEnergyApp.class);
    private volatile Connection connection;
    private Config configuration;
    private TimeService timeService;
    private ScheduledExecutorService scheduler;

    @Meta.OCD(name = "Dummy Energy App")
    interface Config {
        @Meta.AD(deflt = "Appname", description = "Dummy Energy App")
        String appName();
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public MessageHandler onConnect(Connection connection) {
        this.connection = connection;
        if (connection.getPort().name().equals("buffer")) {
            return new BufferMessageHandler(connection, timeService);
        } else {
            return null;
        }
    }

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        configuration = Configurable.createConfigurable(Config.class, properties);
        log.debug("Dummy Energy App Activated");
    }

    @Deactivate
    public void deactivate() {
    }

    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }

    @Reference
    public void setScheduledExecutorService(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
}
