package nl.tno.hexabus.driver;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        HexabusLifecycle lifecycle = new HexabusLifecycle();
        new Thread(lifecycle).start();

        boolean on = false;
        while (true) {
            System.in.read();
            lifecycle.switchAllTo(on);
            on = !on;
        }
    }
}
