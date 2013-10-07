package org.flexiblepower.example.uncontrolled.driver;

import java.util.Date;
import java.util.Map;
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
import org.flexiblepower.time.SchedulerService;
import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = ResourceDriver.class)
public class UncontrolledDriverExample extends AbstractResourceDriver<UncontrolledState, UncontrolledControlParameters>
		implements UncontrolledDriver, Runnable {

	@Meta.OCD
	interface Config {
		@Meta.AD(deflt = "uncontrolled")
		String resourceId();

	}

	private ScheduledFuture<?> scheduledFuture;
	private SchedulerService schedulerService;
	private ServiceRegistration<?> observationProviderRegistration;
	private TimeService timeService;
	private Config config;
	private MockUncontrolledDevice deviceConnection;

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

	@Deactivate
	public void deactivate() {
		scheduledFuture.cancel(false);
		observationProviderRegistration.unregister();
	}

	@Reference
	public void setSchedulerService(SchedulerService schedulerService) {
		this.schedulerService = schedulerService;
	}

	@Reference
	public void setTimeService(TimeService timeService) {
		this.timeService = timeService;
	}

	@Override
	public void run() {
		try {
			UncontrolledState currentState = getState();
			publish(new Observation<UncontrolledState>(timeService.getTime(), currentState));
		} catch (Throwable t) {
			// When you don't catch your exception here, your Runnable won't be
			// scheduled again
			logger.error("An error occured while retrieving the load of my device", t);
		}
	}

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

	@Override
	public void setControlParameters(UncontrolledControlParameters resourceControlParameters) {
		// An uncontrolled device cannot be controlled
	}

}