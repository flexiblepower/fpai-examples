package org.flexiblepower.example.timeshifter.washingmachine.driver.impl;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineControlParameters;
import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineDriver;
import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineState;
import org.flexiblepower.example.timeshifter.washingmachine.driver.impl.WashingMachineDriverImpl.Config;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ext.ObservationProviderRegistrationHelper;
import org.flexiblepower.rai.values.EnergyProfile;
import org.flexiblepower.ral.ResourceDriver;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
import org.flexiblepower.time.TimeService;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

/**
 * This is an example of a driver implementation for a washing machine.
 */
@Component(designateFactory = Config.class, provide = ResourceDriver.class)
public class WashingMachineDriverImpl
		extends
		AbstractResourceDriver<WashingMachineState, WashingMachineControlParameters>
		implements WashingMachineDriver, Runnable {

	@Meta.OCD(name = "Washing Machine Driver")
	interface Config {
		@Meta.AD(deflt = "washingmachine", description = "Resource identifier")
		String resourceId();

	}

	/** Reference to the 'scheduling' of this object */
	private ScheduledFuture<?> scheduledFuture;
	/** Reference to shared scheduler */
	private ScheduledExecutorService schedulerService;
	/** Reference to the registration as observationProvider */
	private ServiceRegistration<?> observationProviderRegistration;
	/** Reference to the registration as widget */
	private ServiceRegistration<Widget> widgetRegistration;
	/** Reference to the {@link TimeService} */
	private TimeService timeService;
	/** Configuration of this object */
	private Config config;
	private WashingMachineWidget widget;
	private WashingMachineState currentWashingMachineState;

	/**
	 * This method gets called after this component gets a configuration and
	 * after the methods with the Reference annotation are called
	 * 
	 * @param bundleContext
	 *            OSGi BundleContext-object
	 * @param properties
	 *            Map containing the configuration of this component
	 */
	@Activate
	public void activate(BundleContext bundleContext,
			Map<String, Object> properties) {
		// Get the configuration
		config = Configurable.createConfigurable(Config.class, properties);

		// Register us as an ObservationProvider
		String resourceId = config.resourceId();
		observationProviderRegistration = new ObservationProviderRegistrationHelper(
				this).observationType(WashingMachineState.class)
				.observationOf(resourceId).register();
		widget = new WashingMachineWidget(this);
		widgetRegistration = bundleContext.registerService(Widget.class, widget, null);
		
		schedulerService.scheduleAtFixedRate(this, 0, 5, java.util.concurrent.TimeUnit.SECONDS);
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
	
	/**
	 * Sets a reference to a ScheduledExecutorService. This service can be used
	 * to schedule tasks which implement the Runnable interface. Using a central
	 * scheduler is more efficient than starting your own thread.
	 * 
	 * This method is called before the Activate method
	 * 
	 * @param timeService
	 */
	@Reference(optional = false)
	public void setSchedulerService(ScheduledExecutorService schedulerService) {
		this.schedulerService = schedulerService;
	}

	/**
	 * This method is called before the driver gets destroyed
	 */
	@Deactivate
	public void deactivate() {
		widgetRegistration.unregister();
		scheduledFuture.cancel(false);
		observationProviderRegistration.unregister();
	}

	@Override
	public void setControlParameters(
			WashingMachineControlParameters resourceControlParameters) {
		// TODO Auto-generated method stub
		Date programStartTime = resourceControlParameters.getProgramStartTime();
		logger.info("Start time is set to " + programStartTime);
	}

	@Override
	public void run() {
		try {
			currentWashingMachineState = generateState();
			logger.info("Washing machine ready to start between " +
						currentWashingMachineState.getEarliestStartTime() +
						" and " + currentWashingMachineState.getLatestStartTime());
			publish(new Observation<WashingMachineState>(timeService.getTime(), currentWashingMachineState));
		}  catch (RuntimeException e) {
			// When you don't catch your exception here, your Runnable won't be
			// scheduled again
			logger.error("An error occured while retrieving washing machine information", e);
		}
	}
	
	public WashingMachineState getCurrentState() {
		return currentWashingMachineState;
	}

	/**
	 * Helper method to generate an {@link WashingMachineState} object
	 * 
	 * @return
	 */
	private WashingMachineState generateState() {
		final String programName = "Cotton Speed Wash";
		final Date earliestStartTime = timeService.getTime();
		final Date latestStartTime = new Date(earliestStartTime.getTime()
				+ (2 * 60 * 60 * 1000)); // latestStartTime is earliestStartTime
											// + 2 hours

		EnergyProfile.Builder b = EnergyProfile.create();
		b.add(Measure.valueOf(60, SI.SECOND), Measure.valueOf(0.015, NonSI.KWH));
		b.add(Measure.valueOf(60, SI.SECOND), Measure.valueOf(0.012, NonSI.KWH));
		final EnergyProfile energyProfile = b.build();
		
		return new WashingMachineState() {
			
			public boolean isConnected() {
				return true;
			}
			
			public String getProgramName() {
				return programName;
			}
			
			public Date getLatestStartTime() {
				return latestStartTime;
			}
			
			public EnergyProfile getEnergyProfile() {
				return energyProfile;
			}
			
			@Override
			public Date getEarliestStartTime() {
				return earliestStartTime;
			}
		};
	}

}
