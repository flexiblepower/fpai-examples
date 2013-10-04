package org.flexiblepower.example.uncontrolled.driver;

public class MockUncontrolledDevice {

	private double currentDemandWatt;

	public MockUncontrolledDevice() {
		currentDemandWatt = 0;
	}

	public double getCurrentDemandInWatt() {
		currentDemandWatt += Math.random() * 20 - 10;
		currentDemandWatt = Math.min(Math.max(0, currentDemandWatt), 1000);
		return currentDemandWatt;
	}
}
