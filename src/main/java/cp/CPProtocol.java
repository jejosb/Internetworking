package cp;

import core.*;
import exceptions.*;
import phy.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CPProtocol extends Protocol {
    private static final int CP_TIMEOUT = 2000;
    private static final int CP_HASHMAP_SIZE = 20;
    private int cookie;
    private int id; // next command id
    private Integer lastSentCommandId; // track last sent id for correlation
    private PhyConfiguration PhyConfigCommandServer;
    private PhyConfiguration PhyConfigCookieServer;
    private final PhyProtocol PhyProto;
    private final cp_role role;
    HashMap<PhyConfiguration, Cookie> cookieMap;
    ArrayList<CPCommandMsg> pendingCommands;
    Random rnd;

    private enum cp_role {
        CLIENT, COOKIE, COMMAND
    }

    // Constructor for clients
    public CPProtocol(InetAddress rname, int rp, PhyProtocol phyP) throws UnknownHostException {
        this.PhyConfigCommandServer = new PhyConfiguration(rname, rp, proto_id.CP);
        this.PhyProto = phyP;
        this.role = cp_role.CLIENT;
        this.cookie = -1;
    }

    // Constructor for servers
    public CPProtocol(PhyProtocol phyP, boolean isCookieServer) {
        this.PhyProto = phyP;
        if (isCookieServer) {
            this.role = cp_role.COOKIE;
            this.cookieMap = new HashMap<>();
            this.rnd = new Random();
        } else {
            this.role = cp_role.COMMAND;
            this.pendingCommands = new ArrayList<>();
        }
    }

    public void setCookieServer(InetAddress rname, int rp) throws UnknownHostException {
        this.PhyConfigCookieServer = new PhyConfiguration(rname, rp, proto_id.CP);
    }

    @Override
    public void send(String s, Configuration config) throws IOException, IWProtocolException {
        if (this.role == cp_role.CLIENT) {
            if (cookie < 0) {
                // Request a new cookie from server
                // Either updates the cookie attribute or returns with an exception
                requestCookie();
            }
        }

        // Task 1.2.1.a: Create a command message object
        CPCommandMsg msg = new CPCommandMsg();
        int currentId = this.id++;
        msg.create(currentId, s);
        this.lastSentCommandId = currentId;

        // Task 1.2.1.b: Send the command to the Command Server via the PHY layer
        if (this.role == cp_role.CLIENT) {
            this.PhyProto.send(new String(msg.getDataBytes()), this.PhyConfigCommandServer);
        } else {
            // For servers, use the provided configuration
            this.PhyProto.send(new String(msg.getDataBytes()), config);
        }
    }

    @Override
    public Msg receive() throws IOException, IWProtocolException {
        // For clients: wait for command response with timeout
        if (this.role == cp_role.CLIENT) {
            return receiveCommandResponse();
        }

        // For servers: normal receive
        try {
            // Receive message from physical layer
            Msg phyMsg = this.PhyProto.receive();

            if (phyMsg == null) {
                return null;
            }

            // Parse the received message
            CPMsg cpmIn = (CPMsg) new CPMsg().parse(phyMsg.getData());
            cpmIn.setConfiguration(phyMsg.getConfiguration());

            // Process based on role
            if (this.role == cp_role.COOKIE) {
                cookie_process(cpmIn);
                return cpmIn;
            } else if (this.role == cp_role.COMMAND) {
                return command_process(cpmIn);
            }

            return cpmIn;
        } catch (IWProtocolException e) {
            // If parsing fails, return null
            return null;
        } catch (Exception e) {
            // If any other exception occurs, return null
            return null;
        }
    }

    // Method for clients to receive command responses
    private Msg receiveCommandResponse() throws IOException, IWProtocolException {
        boolean waitForResp = true;
        int count = 0;
        Msg resMsg = null;

        while (waitForResp && count < 3) {
            try {
                // Task 1.2.2.a: For each sent command message, the client waits a maximum of
                // two seconds (2000ms) for a response from the Command Server. Call the
                // corresponding receive method of the PHY layer.
                Msg in = this.PhyProto.receive(2000); // 2 seconds timeout

                if (in == null) {
                    count += 1;
                    continue;
                }

                // Check if message is from CP protocol
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP) {
                    continue;
                }

                // Task 1.2.2.b: Call the message parser to create a CP message object from the
                // received string object according to the protocol specification.
                try {
                    resMsg = new CPMsg().parse(in.getData());
                    resMsg.setConfiguration(in.getConfiguration());

                    // Task 1.2.2.c: Check if the response matches the command message by comparing
                    // the message ID of the received message with the ID of the sent message.
                    if (resMsg instanceof CPCommandResponseMsg) {
                        CPCommandResponseMsg responseMsg = (CPCommandResponseMsg) resMsg;
                        if (this.lastSentCommandId == null || responseMsg.getId() != this.lastSentCommandId) {
                            // Not for us, continue waiting
                            continue;
                        }
                        // Task 1.2.2.d: Check if the Command Server has accepted the command.
                        if (!responseMsg.isSuccess()) {
                            throw new CookieTimeoutException();
                        }
                        // Task 1.2.2.e: Return an appropriate response to the client.
                        waitForResp = false;
                    } else {
                        // Ignore other message types
                    }
                } catch (CookieTimeoutException e) {
                    // Re-throw CookieTimeoutException immediately
                    throw e;
                } catch (IWProtocolException e) {
                    // Task 1.2.2.b: If the parser throws an exception, the message should be
                    // discarded.
                }

            } catch (SocketTimeoutException e) {
                count += 1;
            } catch (Exception e) {
                count += 1;
            }
        }

        // If no valid response received after 3 attempts
        if (waitForResp && (resMsg == null || count >= 3)) {
            throw new CookieTimeoutException();
        }

        return resMsg;
    }

    // CommandServer processing of incoming messages
    // Only CPCommandMsg are processed, all others are ignored
    private Msg command_process(CPMsg cpmIn) throws IWProtocolException {
        if (cpmIn instanceof CPCommandMsg) {
            return cpmIn;
        }
        return null;
    }

    // Processing of the CookieRequestMsg
    private void cookie_process(CPMsg cpmIn) throws IWProtocolException, IOException {
        // Task 2.1.1: Enhance CPProtocol.receive() to receive cookie requests.
        // The cookie requests shall be processed in a dedicated method.
        if (cpmIn instanceof CPCookieRequestMsg) {
            PhyConfiguration clientConfig = (PhyConfiguration) cpmIn.getConfiguration();

            // Task 2.1.2.a: The mappings of clients to cookies are stored in a Java
            // HashMap.
            // There shall never be more than 20 entries in the HashMap.
            // Check if new client and limit reached
            if (!cookieMap.containsKey(clientConfig) && cookieMap.size() >= CP_HASHMAP_SIZE) {
                // Reject request
                CPCookieResponseMsg response = new CPCookieResponseMsg(false);
                response.create("Cookie limit reached");
                this.PhyProto.send(new String(response.getDataBytes()), clientConfig);
                return;
            }

            // Task 2.1.2.b: Implementation decision on premature cookie renewal.
            // Decision: We allow clients to request a new cookie even if their old one hasn't expired yet.
            // 
            // Alternative approach: Reject premature renewal requests and force clients to wait
            // until their old cookie expires.
            //
            // Rationale for our choice: Clients may restart or lose their state, which would
            // require them to request a new cookie. Rejecting such requests would lock them out
            // until expiration, which is impractical for development, testing, and real-world
            // scenarios. Since HashMap.put() simply overwrites the existing entry, there are no
            // security concerns with allowing early renewal. The old cookie is effectively
            // invalidated when replaced, which is the desired behavior.

            // Generate a random cookie
            int newCookie = rnd.nextInt(1000000);
            Cookie cookieObj = new Cookie(System.currentTimeMillis(), newCookie);

            // Store in HashMap (Task 2.1.2.a) - this handles both new entries and updates
            // (renewals)
            cookieMap.put(clientConfig, cookieObj);

            // Task 2.1.2.c: Send an appropriate response message to the client.
            CPCookieResponseMsg response = new CPCookieResponseMsg(true);
            response.create(String.valueOf(newCookie));

            // Send response back
            this.PhyProto.send(new String(response.getDataBytes()), clientConfig);
        }
    }

    // Method for the client to request a cookie
    public void requestCookie() throws IOException, IWProtocolException {
        CPCookieRequestMsg reqMsg = new CPCookieRequestMsg();
        reqMsg.create(null);
        Msg resMsg = new CPMsg();

        boolean waitForResp = true;
        int count = 0;
        while (waitForResp && count < 3) {
            this.PhyProto.send(new String(reqMsg.getDataBytes()), this.PhyConfigCookieServer);

            try {
                Msg in = this.PhyProto.receive(CP_TIMEOUT);
                if (in == null) {
                    count += 1;
                    continue;
                }
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP)
                    continue;
                resMsg = ((CPMsg) resMsg).parse(in.getData());
                if (resMsg instanceof CPCookieResponseMsg)
                    waitForResp = false;
            } catch (SocketTimeoutException e) {
                count += 1;
            } catch (IWProtocolException ignored) {
            }
        }

        if (count == 3)
            throw new CookieRequestException();
        if (resMsg instanceof CPCookieResponseMsg && !((CPCookieResponseMsg) resMsg).getSuccess()) {
            throw new CookieRequestException();
        }
        assert resMsg instanceof CPCookieResponseMsg;
        this.cookie = ((CPCookieResponseMsg) resMsg).getCookie();
    }
}

class Cookie {
    private final long timeOfCreation;
    private final int cookieValue;

    public Cookie(long toc, int c) {
        this.timeOfCreation = toc;
        this.cookieValue = c;
    }

    public long getTimeOfCreation() {
        return timeOfCreation;
    }

    public int getCookieValue() {
        return cookieValue;
    }
}
