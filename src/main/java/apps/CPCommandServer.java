package apps;

import core.Msg;
import cp.CPProtocol;
import cp.CPCommandResponseMsg;
import exceptions.IWProtocolException;
import phy.PhyProtocol;
import phy.PhyConfiguration;

import java.io.IOException;

public class CPCommandServer {
    protected static final int COMMAND_SERVER_PORT = 2000;

    public static void main(String[] args) {
        // Set up the virtual link protocol
        PhyProtocol phy = new PhyProtocol(COMMAND_SERVER_PORT);

        // Set up command protocol
        CPProtocol cp;
        try {
            cp = new CPProtocol(phy, false); // false = Command Server
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Start server processing
        System.out.println("Command Server listening on port " + COMMAND_SERVER_PORT);
        while (true) {
            try {
                Msg receivedMsg = cp.receive();
                
                // Debug: print raw received data
                if (receivedMsg != null) {
                    System.out.println("[CPCommandServer] RECV: " + receivedMsg.getData());
                }
                
                // Check if we received a valid message
                if (receivedMsg == null) {
                    continue;
                }
                
                // Extract the command and id from the message
                String commandToProcess;
                int cmdId = 0;
                if (receivedMsg instanceof cp.CPCommandMsg cmdMsg) {
                    commandToProcess = cmdMsg.getCommand();
                    cmdId = cmdMsg.getId();
                } else {
                    // Fallback: use getData() if not a CPCommandMsg
                    commandToProcess = receivedMsg.getData();
                }
                
                // Process the command and send response
                String response = processCommand(commandToProcess);
                boolean ok = !response.startsWith("ERROR");
                String payload = "";
                if (ok) {
                    // Check if response contains payload after "OK:"
                    if (response.startsWith("OK:")) {
                        payload = response.substring(3); // Extract text after "OK:"
                    }
                    // If response is just "OK", payload remains empty
                } else {
                    payload = extractErrorMessage(response);
                }
                
                // Create CPCommandResponseMsg with CRC following spec
                CPCommandResponseMsg responseMsg = new CPCommandResponseMsg();
                responseMsg.create(cmdId, ok, payload);
                String responseData = new String(responseMsg.getDataBytes());
                
                // Send response back to client
                // Use configuration from received message to send response back to correct client
                PhyConfiguration responseConfig = (PhyConfiguration) receivedMsg.getConfiguration();
                phy.send(responseData, responseConfig);
                System.out.println("[CPCommandServer] SEND: " + responseData);
                
            } catch (IOException e) {
                return;
            } catch (IWProtocolException e) {
                // Continue on protocol errors
            }
        }
    }
    
    private static String processCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "ERROR: Empty command";
        }
        
        String cmd = command.trim();
        
        if (cmd.equals("status")) {
            return "OK"; // No payload expected
        } else if (cmd.startsWith("print ")) {
            String text = cmd.substring(6); // Remove "print " prefix
            if (text.startsWith("\"") && text.endsWith("\"")) {
                text = text.substring(1, text.length() - 1); // Remove quotes
            }
            // Return the text that was printed as payload
            return "OK:" + text;
        } else {
            return "ERROR: Unknown command. Supported commands: status, print \"text\"";
        }
    }

    private static String extractErrorMessage(String response) {
        // Expect formats like "ERROR: <text>" or "ERROR: <code> <text>"
        int idx = response.indexOf(":");
        if (idx >= 0 && idx + 1 < response.length()) {
            return response.substring(idx + 1).trim();
        }
        return response;
    }
}
