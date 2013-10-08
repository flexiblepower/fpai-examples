package org.flexiblepower.example.washingmachine.driver.api;

import java.util.Date;

import org.flexiblepower.rai.values.EnergyProfile;
import org.flexiblepower.ral.ResourceState;

public interface WashingMachineState extends ResourceState {
	
	Date getStartTime();
	
	String getProgram();
	
	EnergyProfile getEnergyProfile();

}
