package org.flexiblepower.example.timeshifter.washingmachine.driver.api;

import java.util.Date;

import org.flexiblepower.rai.values.EnergyProfile;
import org.flexiblepower.ral.ResourceState;

public interface WashingMachineState extends ResourceState {
	
	Date getEarliestStartTime();
	
	Date getLatestStartTime();
	
	String getProgramName();
	
	EnergyProfile getEnergyProfile();

}
