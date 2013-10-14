package org.flexiblepower.example.timeshifter.washingmachine.manager;

import java.util.Date;
import java.util.Map;

import javax.measure.Measure;

import static javax.measure.unit.SI.SECOND;

import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineControlParameters;
import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineDriver;
import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineState;
import org.flexiblepower.example.timeshifter.washingmachine.manager.WashingMachineManager.Config;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationProvider;
import org.flexiblepower.rai.Allocation;
import org.flexiblepower.rai.TimeShifterControlSpace;
import org.flexiblepower.ral.ResourceDriver;
import org.flexiblepower.ral.ResourceManager;
import org.flexiblepower.ral.ext.AbstractResourceManager;
import org.flexiblepower.time.TimeService;
import org.flexiblepower.time.TimeUtil;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = ResourceManager.class)
public class WashingMachineManager extends AbstractResourceManager<TimeShifterControlSpace, WashingMachineState, WashingMachineControlParameters> {
	
	@Meta.OCD(name = "Washing Machine Manager")
	interface Config {
        @Meta.AD(deflt = "washingmachine", description = "Resource identifier")
        String resourceId();
        
        @Meta.AD(deflt = "3600", description = "Expiration of the ControlSpaces [s]", required = false)
        int expirationTime();
	}
	
	/** Reference to the {@link TimeService} */
	private TimeService timeService;
	/** Configuration of this object */
	private Config config;
	private Measure<Integer, javax.measure.quantity.Duration> expirationTime;
	
	public WashingMachineManager() {
		super(WashingMachineDriver.class, TimeShifterControlSpace.class);
	}
	
	/**
	 * Sets the TimeService. The TimeService is used to determine the current
	 * time.
	 * 
	 * This method is called before the Activate method
	 * 
	 * @param timeService
	 */
	@Reference(optional = false)
	public void setTimeService(TimeService timeService) {
		this.timeService = timeService;
	}
	
    @Activate
    public void init(Map<String, ?> properties) {
        config = Configurable.createConfigurable(Config.class, properties);
        expirationTime = Measure.valueOf(config.expirationTime(), SECOND);
        logger.info("Washing machine manager started");
    }

	@Override
	public void consume(
			ObservationProvider<? extends WashingMachineState> source,
			Observation<? extends WashingMachineState> observation) {
		logger.info("Observation received from " + source + ": " + observation.getValue());
		
		publish(new TimeShifterControlSpace(config.resourceId(),
											timeService.getTime(),
											TimeUtil.add(timeService.getTime(), expirationTime),
											TimeUtil.add(timeService.getTime(), expirationTime),
											observation.getValue().getEnergyProfile(),
											observation.getValue().getLatestStartTime(),
											observation.getValue().getEarliestStartTime()));		
	}

	@Override
	public void handleAllocation(Allocation allocation) {
		ResourceDriver<WashingMachineState, WashingMachineControlParameters> driver = getDriver();
		if (allocation != null && driver != null) {
			final Date startTime = allocation.getStartTime();
			logger.info("Received an allocation with start time: " + startTime);
			driver.setControlParameters(new WashingMachineControlParameters() {
				
				@Override
				public Date getProgramStartTime() {
					return startTime;
				}
			});
		} else {
			logger.info("Received null allocation");
		}
	}

}
