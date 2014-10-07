package org.flexiblepower.dummy.energyapp;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import javax.measure.quantity.Energy;

import org.flexiblepower.dummy.energyapp.DummyEnergyApp.Config;
import org.flexiblepower.efi.EfiControllerManager;
import org.flexiblepower.efi.buffer.ActuatorAllocation;
import org.flexiblepower.efi.buffer.ActuatorBehaviour;
import org.flexiblepower.efi.buffer.BufferAllocation;
import org.flexiblepower.efi.buffer.BufferRegistration;
import org.flexiblepower.efi.buffer.BufferStateUpdate;
import org.flexiblepower.efi.buffer.BufferSystemDescription;
import org.flexiblepower.efi.buffer.RunningModeBehaviour;
import org.flexiblepower.efi.util.FillLevelFunction;
import org.flexiblepower.efi.util.RunningMode;
import org.flexiblepower.messaging.Connection;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.messaging.MessageHandler;
import org.flexiblepower.rai.AllocationStatusUpdate;
import org.flexiblepower.rai.ControlSpaceRevoke;
import org.flexiblepower.ral.drivers.battery.BatteryMode;
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

    private final BlockingQueue<BufferRegistration<?>> bufferRegistrations = new LinkedBlockingQueue<BufferRegistration<?>>();
    private final BlockingQueue<BufferSystemDescription> bufferSystemDescriptions = new LinkedBlockingQueue<BufferSystemDescription>();
    private final BlockingQueue<BufferStateUpdate<?>> bufferStateUpdates = new LinkedBlockingQueue<BufferStateUpdate<?>>();
    private final BlockingQueue<AllocationStatusUpdate> allocationStatusUpdates = new LinkedBlockingQueue<AllocationStatusUpdate>();
    private final BlockingQueue<ControlSpaceRevoke> controlSpaceRevokes = new LinkedBlockingQueue<ControlSpaceRevoke>();

    private BufferSystemDescription latestBufferSystemDescriptionMessage = null;

    @Override
    public MessageHandler onConnect(Connection connection) {
        this.connection = connection;
        return new MessageHandler() {

            @Override
            public void handleMessage(Object message) {
                log.info("Received message");

                if (BufferRegistration.class.isAssignableFrom(message.getClass())) {
                    BufferRegistration<?> resourceMessage = (BufferRegistration<?>) message;
                    log.debug("Received BufferRegistration");
                    bufferRegistrations.add(resourceMessage);
                } else if (BufferSystemDescription.class.isAssignableFrom(message.getClass())) {
                    BufferSystemDescription resourceMessage = (BufferSystemDescription) message;
                    log.debug("Received BufferSystemDescription");
                    bufferSystemDescriptions.add(resourceMessage);
                    latestBufferSystemDescriptionMessage = resourceMessage;
                } else if (BufferStateUpdate.class.isAssignableFrom(message.getClass())) {
                    BufferStateUpdate<?> untypedBufferStateUpdate = (BufferStateUpdate<?>) message;
                    BufferStateUpdate<Energy> resourceMessage = (BufferStateUpdate<Energy>) untypedBufferStateUpdate;
                    log.debug("Received BufferStateUpdate");
                    bufferStateUpdates.add(resourceMessage);
                    handleBufferStateUpdateMessage(resourceMessage);
                } else if (AllocationStatusUpdate.class.isAssignableFrom(message.getClass())) {
                    AllocationStatusUpdate resourceMessage = (AllocationStatusUpdate) message;
                    log.debug("Received AllocationStatusUpdate");
                    allocationStatusUpdates.add(resourceMessage);
                } else if (ControlSpaceRevoke.class.isAssignableFrom(message.getClass())) {
                    ControlSpaceRevoke resourceMessage = (ControlSpaceRevoke) message;
                    log.debug("Received ControlSpaceRevoke");
                    controlSpaceRevokes.add(resourceMessage);
                } else {
                    throw new AssertionError("Unknown message class : " + message.getClass());
                }

            }

            @Override
            public void disconnected() {
                DummyEnergyApp.this.connection = null;
            }
        };
    }

    private void handleBufferStateUpdateMessage(BufferStateUpdate<?> bufferStateUpdate) {
        if (latestBufferSystemDescriptionMessage != null) {
            sendAllocation(latestBufferSystemDescriptionMessage, bufferStateUpdate);
        }
    }

    private void
            sendAllocation(BufferSystemDescription bufferSystemDescription, BufferStateUpdate<?> bufferStateUpdate) {
        long now = bufferStateUpdate.getTimestamp().getTime();

        Collection<ActuatorBehaviour> actuators = bufferSystemDescription.getActuators();
        ActuatorBehaviour actuatorBehaviour = actuators.iterator().next();
        int actuatorId = actuatorBehaviour.getId();
        Collection<RunningMode<FillLevelFunction<RunningModeBehaviour>>> runningModes = actuatorBehaviour.getRunningModes();
        int runningModeId = BatteryMode.CHARGE.ordinal();
        Set<ActuatorAllocation> actuatorAllocations = new HashSet<ActuatorAllocation>();
        actuatorAllocations.add(new ActuatorAllocation(actuatorId, runningModeId, new Date(now + 5000)));
        BufferAllocation bufferAllocation = new BufferAllocation(bufferStateUpdate,
                                                                 new Date(now),
                                                                 false,
                                                                 actuatorAllocations);
        log.debug("constructed buffer allocation=" + bufferAllocation);
        connection.sendMessage(bufferAllocation);

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
