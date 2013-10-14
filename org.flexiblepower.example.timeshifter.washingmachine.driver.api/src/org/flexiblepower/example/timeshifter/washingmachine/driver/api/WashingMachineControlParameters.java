package org.flexiblepower.example.timeshifter.washingmachine.driver.api;

import java.util.Date;

import org.flexiblepower.ral.ResourceControlParameters;

public interface WashingMachineControlParameters extends ResourceControlParameters{
	
	Date getProgramStartTime();

}
