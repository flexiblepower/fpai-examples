package org.flexiblepower.example.uncontrolled.manager;

import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Energy;
import javax.measure.unit.SI;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationProvider;
import org.flexiblepower.rai.Allocation;
import org.flexiblepower.rai.UncontrolledControlSpace;
import org.flexiblepower.rai.values.EnergyProfile;
import org.flexiblepower.ral.ResourceManager;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledControlParameters;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledDriver;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledState;
import org.flexiblepower.ral.ext.AbstractResourceManager;
import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = org.flexiblepower.example.uncontrolled.manager.UncontrolledManagerExample.Config.class, immediate = true, provide = ResourceManager.class)
public class UncontrolledManagerExample extends
		AbstractResourceManager<UncontrolledControlSpace, UncontrolledState, UncontrolledControlParameters> {

	@Meta.OCD
	interface Config {
		@Meta.AD(deflt = "uncontrolled", description = "Resource identifier")
		String resourceId();

		@Meta.AD(deflt = "true", description = "Show the widget")
		boolean showWidget();
	}

	/** Reference to the {@link TimeService} */
	private TimeService timeService;
	/** Configuration of this object */
	private Config config;

	public UncontrolledManagerExample() {
		super(UncontrolledDriver.class, UncontrolledControlSpace.class);
	}

	@Activate
	public void activate(BundleContext bundleContext, Map<String, Object> properties) {
		// Get the configuration
		config = Configurable.createConfigurable(Config.class, properties);
	}

	@Deactivate
	public void deactivate() {

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

	@Override
	public void consume(ObservationProvider<? extends UncontrolledState> source,
			Observation<? extends UncontrolledState> observation) {
		UncontrolledState uncontrolledState = observation.getValue();
		logger.debug("Received a state from " + source + " with a demand of " + uncontrolledState.getDemand());
		publish(constructControlSpace(uncontrolledState));
	}

	private UncontrolledControlSpace constructControlSpace(UncontrolledState uncontrolledState) {
		// Calculate the amount of energy for 10 seconds (1 watt = 1 joule /
		// second)
		Measure<Double, Energy> energy = Measure.valueOf(uncontrolledState.getDemand().doubleValue(SI.WATT) * 10,
				SI.JOULE);
		// Create an EnergyProfyle for the next 10 seconds
		EnergyProfile energyProfile = EnergyProfile.create().add(Measure.valueOf(10, SI.SECOND), energy).build();
		// Construct the ControlSpace object
		return new UncontrolledControlSpace(this.config.resourceId(), timeService.getTime(), energyProfile);
	}

	@Override
	public void handleAllocation(Allocation allocation) {
		// Uncontrolled devices can't handle allocations, nothing to do here
	}
}
