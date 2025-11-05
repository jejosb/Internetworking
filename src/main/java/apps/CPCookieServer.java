package apps;

import cp.CPProtocol;
import exceptions.IWProtocolException;
import phy.PhyProtocol;

import java.io.IOException;

public class CPCookieServer {
    protected static final int COOKIE_SERVER_PORT = 3000;

    public static void main(String[] args) {
        // Set up the virtual link protocol
        PhyProtocol phy = new PhyProtocol(COOKIE_SERVER_PORT);

        // Set up command protocol
        CPProtocol cp;
        try {
            cp = new CPProtocol(phy, true);
        } catch (Exception e) {
            System.err.println("Failed to initialize CPProtocol: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Start server processing
        System.out.println("Cookie Server listening on port " + COOKIE_SERVER_PORT);
        while (true) {
            try {
                cp.receive();
            } catch (IOException e) {
                return;
            } catch (IWProtocolException e) {
                // Continue on protocol errors
            }
        }
    }
}