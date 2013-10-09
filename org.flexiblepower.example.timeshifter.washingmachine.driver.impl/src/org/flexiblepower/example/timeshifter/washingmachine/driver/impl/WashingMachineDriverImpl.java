package org.flexiblepower.example.timeshifter.washingmachine.driver.impl;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.flexiblepower.example.timeshifter.washingmachine.driver.impl.WashingMachineDriverImpl.Config;
import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineControlParameters;
import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineDriver;
import org.flexiblepower.example.timeshifter.washingmachine.driver.api.WashingMachineState;
import org.flexiblepower.observation.ObservationConsumer;
import org.flexiblepower.observation.ObservationProviderRegistrationHelper;
import org.flexiblepower.ral.ResourceDriver;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledState;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;


/**
 * This is an example of a driver for a washing machine.
 */
@Component(designateFactory = Config.class, provide = ResourceDriver.class)
public class WashingMachineDriverImpl extends AbstractResourceDriver<WashingMachineState, WashingMachineControlParameters> implements WashingMachineDriver, Runnable {
	
	@Meta.OCD
	interface Config {
		@Meta.AD(deflt = "washingmachine", description = "Resource identifier")
		String resourceId();

	}

	/** Reference to the 'scheduling' of this object */
	private ScheduledFuture<?> scheduledFuture;
	/** Reference to the registration as observationProvider */
	private ServiceRegistration<?> observationProviderRegistration;
	/** Configuration of this object */
	private Config config;
	
	
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
		String resourceId = config.resourceId();
		observationProviderRegistration = new ObservationProviderRegistrationHelper(this)
				.observationType(UncontrolledState.class).observationOf(resourceId).observedBy(resourceId).register();
	}

	/**
	 * This method is called before the driver gets destroyed
	 */
	@Deactivate
	public void deactivate() {
		scheduledFuture.cancel(false);
		observationProviderRegistration.unregister();
	}
	
	@Override
	public void setControlParameters(
			WashingMachineControlParameters resourceControlParameters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subscribe(
			ObservationConsumer<? super WashingMachineState> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe(
			ObservationConsumer<? super WashingMachineState> consumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	

}
