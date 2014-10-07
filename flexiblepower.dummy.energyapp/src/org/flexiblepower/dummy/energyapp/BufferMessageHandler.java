package org.flexiblepower.dummy.energyapp;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.measure.quantity.Energy;

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
import org.flexiblepower.messaging.MessageHandler;
import org.flexiblepower.rai.AllocationStatusUpdate;
import org.flexiblepower.rai.ControlSpaceRevoke;
import org.flexiblepower.ral.drivers.battery.BatteryMode;
import org.flexiblepower.time.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(BufferMessageHandler.class);
    private final TimeService timeService;
    Connection connection;
    private BufferSystemDescription latestBufferSystemDescriptionMessage = null;

    public BufferMessageHandler(Connection connection, TimeService timeService) {
        log.debug("Constructing new BufferMessageHandler.");
        this.timeService = timeService;
        this.connection = connection;
    }

    @Override
    public void handleMessage(Object message) {
        log.info("Received message");

        if (BufferRegistration.class.isAssignableFrom(message.getClass())) {
            BufferRegistration<?> resourceMessage = (BufferRegistration<?>) message;
            log.debug("Received BufferRegistration");
        } else if (BufferSystemDescription.class.isAssignableFrom(message.getClass())) {
            BufferSystemDescription resourceMessage = (BufferSystemDescription) message;
            log.debug("Received BufferSystemDescription");
            latestBufferSystemDescriptionMessage = resourceMessage;
        } else if (BufferStateUpdate.class.isAssignableFrom(message.getClass())) {
            BufferStateUpdate<?> untypedBufferStateUpdate = (BufferStateUpdate<?>) message;
            BufferStateUpdate<Energy> resourceMessage = (BufferStateUpdate<Energy>) untypedBufferStateUpdate;
            log.debug("Received BufferStateUpdate");
            handleBufferStateUpdateMessage(resourceMessage);
        } else if (AllocationStatusUpdate.class.isAssignableFrom(message.getClass())) {
            AllocationStatusUpdate resourceMessage = (AllocationStatusUpdate) message;
            log.debug("Received AllocationStatusUpdate");
        } else if (ControlSpaceRevoke.class.isAssignableFrom(message.getClass())) {
            ControlSpaceRevoke resourceMessage = (ControlSpaceRevoke) message;
            log.debug("Received ControlSpaceRevoke");
        } else {
            throw new AssertionError("Unknown message class : " + message.getClass());
        }

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

    @Override
    public void disconnected() {
        // TODO Auto-generated method stub
        connection = null;
    }

}
