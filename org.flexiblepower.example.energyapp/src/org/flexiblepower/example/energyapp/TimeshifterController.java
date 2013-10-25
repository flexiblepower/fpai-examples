package org.flexiblepower.example.energyapp;

import java.util.Date;

import org.flexiblepower.rai.Allocation;
import org.flexiblepower.rai.ControllableResource;
import org.flexiblepower.rai.Controller;
import org.flexiblepower.rai.TimeShifterControlSpace;
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
		
		long timeInTheMiddle = (controlSpace.getStartAfter().getTime() + controlSpace.getStartBefore().getTime())/2;
		Allocation allocation = new Allocation(controlSpace, new Date(timeInTheMiddle), controlSpace.getEnergyProfile());
		controllableResource.handleAllocation(allocation);
	}
	
}
