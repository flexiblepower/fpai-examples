package org.flexiblepower.example.energyapp;

import org.flexiblepower.control.ControllerManager;
import org.flexiblepower.rai.ControllableResource;
import org.flexiblepower.rai.TimeShifterControlSpace;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = EnergyApp.Config.class, provide = ControllerManager.class)
public class EnergyApp implements ControllerManager {
	
	@Meta.OCD(name = "Simple Energy App")
	interface Config {
		@Meta.AD(deflt = "washingmachine", description = "Resource identifier")
		String[] resourceIds();

	}

	@Override
	public void registerResource(ControllableResource<?> resource) {
		System.out.println("Received Control Space");
		if (resource.getControlSpaceType().equals(TimeShifterControlSpace.class)) {
			System.out.println("Received Timeshifter control space");
			TimeshifterController timeshifterController = new TimeshifterController();
			timeshifterController.bind((ControllableResource<TimeShifterControlSpace>) resource);
		}
		
	}

	@Override
	public void unregisterResource(ControllableResource<?> resource) {
		// TODO Auto-generated method stub
		
	}

}
