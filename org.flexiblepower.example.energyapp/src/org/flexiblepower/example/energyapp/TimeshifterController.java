package org.flexiblepower.example.energyapp;

import org.flexiblepower.rai.ControlSpace;
import org.flexiblepower.rai.ControllableResource;
import org.flexiblepower.rai.Controller;
import org.flexiblepower.rai.TimeShifterControlSpace;
import org.flexiblepower.ral.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeshifterController implements Controller<TimeShifterControlSpace> {
	static Logger logger = LoggerFactory.getLogger(TimeshifterController.class);
	private ControllableResource<TimeShifterControlSpace> controllableResource;
	
	public void bind(ControllableResource<TimeShifterControlSpace> controllableResource) {
		logger.info("Binding resource manager to TimeshifterController");
		this.controllableResource = controllableResource;
		controllableResource.setController(this);
	}

	@Override
	public void controlSpaceUpdated(
			ControllableResource<? extends TimeShifterControlSpace> resource,
			TimeShifterControlSpace controlSpace) {
		logger.info("Received control space with timestamp" + controlSpace.getValidFrom());		
	}
	
}
