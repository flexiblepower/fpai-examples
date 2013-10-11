package org.flexiblepower.example.timeshifter.washingmachine.driver.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineState;
import org.flexiblepower.ui.Widget;

public class WashingMachineWidget implements Widget {
	public static class Update {
		private final String earliestStartTime;
		private final String latestStartTime;
		private final String programName;
		
		public Update(String earliestStartTime, String latestStartTime, String programName) {
			this.earliestStartTime = earliestStartTime;
			this.latestStartTime = latestStartTime;
			this.programName = programName;
		}

		public String getEarliestStartTime() {
			return earliestStartTime;
		}

		public String getLatestStartTime() {
			return latestStartTime;
		}

		public String getProgramName() {
			return programName;
		}
	}
	
	private final static DateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss");
	private final WashingMachineDriverImpl washingMachineDriverImpl;
	
	public WashingMachineWidget(WashingMachineDriverImpl washingMachineDriverImpl) {
		this.washingMachineDriverImpl = washingMachineDriverImpl;
	}
	
	public Update update() {
		WashingMachineState state = washingMachineDriverImpl.getCurrentState();
		
		return new Update(FORMATTER.format(state.getEarliestStartTime()), FORMATTER.format(state.getLatestStartTime()), state.getProgramName());
	}

	@Override
	public String getTitle(Locale locale) {
		return "Washing Machine panel";
	}

}
