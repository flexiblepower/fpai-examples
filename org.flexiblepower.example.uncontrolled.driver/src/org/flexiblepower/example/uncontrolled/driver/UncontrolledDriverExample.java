package org.flexiblepower.example.uncontrolled.driver;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.flexiblepower.example.uncontrolled.driver.UncontrolledDriverExample.Config;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationProviderRegistrationHelper;
import org.flexiblepower.ral.ResourceDriver;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledControlParameters;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledDriver;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledState;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

/**
 * This is an example of a Uncontrolled Driver
 */
@Component(designateFactory = Config.class, provide = ResourceDriver.class)
public class UncontrolledDriverExample extends AbstractResourceDriver<UncontrolledState, UncontrolledControlParameters>
		implements UncontrolledDriver, Runnable {

	@Meta.OCD
	interface Config {
		@Meta.AD(deflt = "uncontrolled")
		String resourceId();

	}

	/** Reference to the 'scheduling' of this object */
	private ScheduledFuture<?> scheduledFuture;
	/** Reference to shared scheduler */
	private ScheduledExecutorService schedulerService;
	/** Reference to the registration as observationProvider */
	private ServiceRegistration<?> observationProviderRegistration;
	/** Reference to the {@link TimeService} */
	private TimeService timeService;
	/** Configuration of this object */
	private Config config;
	/** "Connection" to the appliance */
	private MockUncontrolledDevice deviceConnection;

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
	public void activate(BundleContext bundleContext, Map<String, Object> properties) {
		// Get the configuration
		config = Configurable.createConfigurable(Config.class, properties);

		// Register us as an ObservationProvider
		String applianceId = config.resourceId();
		observationProviderRegistration = new ObservationProviderRegistrationHelper(this)
				.observationType(UncontrolledState.class).observationOf(applianceId).observedBy(applianceId).register();

		// Schedule this object; this will make sure the run method gets called
		// every 5 seconds
		scheduledFuture = schedulerService.scheduleAtFixedRate(this, 0, 5, java.util.concurrent.TimeUnit.SECONDS);

		// "Connect" with the device
		deviceConnection = new MockUncontrolledDevice();
	}

	/**
	 * This method is called before the driver gets destroyed
	 */
	@Deactivate
	public void deactivate() {
		scheduledFuture.cancel(false);
		observationProviderRegistration.unregister();
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
	 * 
	 */
	@Override
	public void run() {
		try {
			UncontrolledState currentState = getState();
			logger.info("The uncontrolled device has updated its demand to" + currentState.getDemand());
			publish(new Observation<UncontrolledState>(timeService.getTime(), currentState));
		} catch (Exception e) {
			// When you don't catch your exception here, your Runnable won't be
			// scheduled again
			logger.error("An error occured while retrieving the load of my device", e);
		}
	}

	/**
	 * Helper method to generate an {@link UncontrolledState} object
	 * 
	 * @return
	 */
	private UncontrolledState getState() {
		return new UncontrolledState() {

			@Override
			public boolean isConnected() {
				return true;
			}

			@Override
			public Date getTime() {
				return UncontrolledDriverExample.this.timeService.getTime();
			}

			@Override
			public Measurable<Power> getDemand() {
				double currentDemandInWatt = UncontrolledDriverExample.this.deviceConnection.getCurrentDemandInWatt();
				return Measure.valueOf(currentDemandInWatt, SI.WATT);
			}
		};
	}

	/**
	 * This method is defined in the interface, but doesn't really make sense
	 * for an {@link UncontrolledDriver}
	 * 
	 * @param resourceControlParameters
	 */
	@Override
	public void setControlParameters(UncontrolledControlParameters resourceControlParameters) {
		// An uncontrolled device cannot be controlled
	}

}