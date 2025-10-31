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
    private int id;
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

        // Create command message
        CPCommandMsg msg = new CPCommandMsg();
        msg.create(s);
        
        // Send through physical layer
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
        
        while(waitForResp && count < 3) {
            try {
                // a. Wait maximum 3 seconds for response from Command Server
                Msg in = this.PhyProto.receive(3000); // 3 seconds timeout
                
                if (in == null) {
                    count += 1;
                    continue;
                }
                
                // Check if message is from CP protocol
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP) {
                    continue;
                }
                
                // b. Parse the received message
                try {
                    resMsg = new CPMsg().parse(in.getData());
                    resMsg.setConfiguration(in.getConfiguration());
                    
                    // c. Check if response matches the sent command message
                    if (resMsg instanceof CPCommandMsg) {
                        // d. Check if Command Server accepted the command
                        String responseData = resMsg.getData();
                        if (responseData != null && !responseData.isEmpty()) {
                            // e. Return the result to the client
                            waitForResp = false;
                        } else {
                            // Invalid message, retry
                            continue;
                        }
                    } else if (resMsg instanceof CPCommandResponseMsg) {
                        // CRC-validierte Command Response Message
                        CPCommandResponseMsg responseMsg = (CPCommandResponseMsg) resMsg;
                        if (responseMsg.getResponse() != null && !responseMsg.getResponse().isEmpty()) {
                            // e. Return the result to the client
                            waitForResp = false;
                        } else {
                            // Invalid response, retry
                            continue;
                        }
                    } else {
                        // Unknown message type, retry
                        continue;
                    }
                } catch (IWProtocolException e) {
                    // b. If parser throws exception, discard the message and retry
                    continue;
                }
                
            } catch (SocketTimeoutException e) {
                count += 1;
            } catch (Exception e) {
                count += 1;
            }
        }
        
        // If no valid response received after 3 attempts
        if (resMsg == null || count >= 3) {
            throw new CookieTimeoutException();
        }

        // Prüfe auf error-Antwort
        if (resMsg instanceof CPCommandResponseMsg) {
            CPCommandResponseMsg responseMsg = (CPCommandResponseMsg) resMsg;
            if (responseMsg.getResponse() != null && responseMsg.getResponse().trim().startsWith("error")) {
                throw new CookieTimeoutException();
            }
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
        if (cpmIn instanceof CPCookieRequestMsg) {
            // Generate a random cookie
            int newCookie = rnd.nextInt(1000000);
            
            // Create cookie response
            CPCookieResponseMsg response = new CPCookieResponseMsg(true);
            response.create(String.valueOf(newCookie));
            
            // Send response back
            this.PhyProto.send(new String(response.getDataBytes()), cpmIn.getConfiguration());
        }
    }


    // Method for the client to request a cookie
    public void requestCookie() throws IOException, IWProtocolException {
        CPCookieRequestMsg reqMsg = new CPCookieRequestMsg();
        reqMsg.create(null);
        Msg resMsg = new CPMsg();

        boolean waitForResp = true;
        int count = 0;
        while(waitForResp && count < 3) {
            this.PhyProto.send(new String(reqMsg.getDataBytes()), this.PhyConfigCookieServer);

            try {
                Msg in = this.PhyProto.receive(CP_TIMEOUT);
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP)
                    continue;
                resMsg = ((CPMsg) resMsg).parse(in.getData());
                if(resMsg instanceof CPCookieResponseMsg)
                    waitForResp = false;
            } catch (SocketTimeoutException e) {
                count += 1;
            } catch (IWProtocolException ignored) {
            }
        }

        if(count == 3)
            throw new CookieRequestException();
        if(resMsg instanceof CPCookieResponseMsg && !((CPCookieResponseMsg) resMsg).getSuccess()) {
            throw new CookieRequestException();
        }
         assert resMsg instanceof CPCookieResponseMsg;
         this.cookie = ((CPCookieResponseMsg)resMsg).getCookie();
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

    public int getCookieValue() { return cookieValue;}
}
