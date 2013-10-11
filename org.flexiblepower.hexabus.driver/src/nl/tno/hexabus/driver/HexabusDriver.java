package nl.tno.hexabus.driver;

import java.io.Closeable;
import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.fraunhofer.itwm.hexabus.Hexabus.DataType;
import de.fraunhofer.itwm.hexabus.HexabusDevice;

public class HexabusDriver implements Closeable {
    private final HexabusDevice dev;

    private long currentPower;
    private boolean switchedOn;

    private ServiceRegistration<HexabusDriver> serviceRegistration;

    public HexabusDriver(HexabusDevice dev, BundleContext bundleContext) throws IOException {
        this.dev = dev;
        dev.addEndpoint(2, DataType.UINT32, "Current power (read-only)");

        if (bundleContext != null) {
            serviceRegistration = bundleContext.registerService(HexabusDriver.class, this, null);
        }
    }

    public long getCurrentPower() {
        return currentPower;
    }

    public boolean isSwitchedOn() {
        return switchedOn;
    }

    public void updatePower(long value) throws IOException {
        currentPower = value;
        switchedOn = dev.getByEid(1).queryBoolEndpoint();
    }

    @Override
    public String toString() {
        return dev.getInetAddress() + " is switched "
               + (switchedOn ? "on" : "off")
               + " and uses "
               + currentPower
               + " Watts";
    }

    public void switchTo(boolean on) throws IOException {
        dev.getByEid(1).writeEndpoint(on);
    }

    @Override
    public void close() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
