package nl.tno.hexabus.driver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import de.fraunhofer.itwm.hexabus.Hexabus;
import de.fraunhofer.itwm.hexabus.Hexabus.PacketType;
import de.fraunhofer.itwm.hexabus.HexabusDevice;
import de.fraunhofer.itwm.hexabus.HexabusInfoPacket;
import de.fraunhofer.itwm.hexabus.HexabusPacket;

@Component(immediate = true, provide = {})
public class HexabusLifecycle implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(HexabusLifecycle.class);

    private static final InetAddress MCAST_ADDR;

    static {
        try {
            MCAST_ADDR = InetAddress.getByName("ff02::1");
        } catch (UnknownHostException ex) {
            throw new Error("Should not be possible", ex);
        }
    }

    private final Map<InetAddress, HexabusDriver> drivers;
    private final AtomicBoolean running;

    public HexabusLifecycle() {
        drivers = Collections.synchronizedMap(new HashMap<InetAddress, HexabusDriver>());
        running = new AtomicBoolean(true);
    }

    private ScheduledExecutorService scheduler;

    @Reference
    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        running.set(true);
        scheduler.execute(this);
    }

    @Deactivate
    public void deactivate() {
        running.set(false);
    }

    @Override
    public void run() {
        MulticastSocket ms = null;
        int retry = 5;

        byte[] buffer = new byte[136];
        DatagramPacket p = new DatagramPacket(buffer, buffer.length);

        Hexabus hexabus;
        try {
            hexabus = new Hexabus();
        } catch (SocketException ex) {
            ex.printStackTrace();
            return;
        }

        while (running.get()) {
            if (ms == null || ms.isClosed()) {
                try {
                    ms = new MulticastSocket(61616);
                    ms.joinGroup(MCAST_ADDR);
                    retry = 5;
                } catch (IOException ex) {
                    logger.warn("Error while opening multicast socket, trying again in " + retry + " seconds", ex);
                    try {
                        Thread.sleep(retry * 1000);
                    } catch (InterruptedException e) {
                    }
                    retry *= 2;
                }
            }

            try {
                ms.receive(p);
                HexabusPacket packet = Hexabus.parsePacket(p);
                logger.trace("Mulcicast packet received: {}", packet);

                if (!drivers.containsKey(p.getAddress())) {
                    drivers.put(p.getAddress(),
                                new HexabusDriver(new HexabusDevice(hexabus, p.getAddress(), p.getPort()),
                                                  bundleContext));
                }

                HexabusDriver driver = drivers.get(p.getAddress());
                if (packet.getPacketType() == PacketType.INFO) {
                    HexabusInfoPacket infoPacket = (HexabusInfoPacket) packet;
                    if (infoPacket.getEid() == 2) {
                        driver.updatePower(infoPacket.getUint32());
                    }
                }
            } catch (IOException ex) {
                logger.warn("I/O error with the multicast socket", ex);
                ms.close();
                ms = null;
            }
        }

        if (ms != null) {
            try {
                ms.leaveGroup(MCAST_ADDR);
                ms.close();
            } catch (IOException ex) {
                logger.error("I/O Error", ex);
            }
        }

        synchronized (drivers) {
            Iterator<HexabusDriver> it = drivers.values().iterator();
            while (it.hasNext()) {
                HexabusDriver driver = it.next();
                driver.close();
                it.remove();
            }
        }
    }

    public void switchAllTo(boolean on) throws IOException {
        synchronized (drivers) {
            for (HexabusDriver driver : drivers.values()) {
                driver.switchTo(on);
            }
        }
    }
}
