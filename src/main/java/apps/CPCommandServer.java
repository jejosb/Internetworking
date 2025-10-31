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
                
                // Check if we received a valid message
                if (receivedMsg == null) {
                    continue;
                }
                
                // Extract the command from the message
                String commandToProcess;
                if (receivedMsg instanceof cp.CPCommandMsg) {
                    commandToProcess = ((cp.CPCommandMsg) receivedMsg).getCommand();
                } else {
                    // Fallback: use getData() if not a CPCommandMsg
                    commandToProcess = receivedMsg.getData();
                }
                
                // Process the command and send response
                String response = processCommand(commandToProcess);
                
                // Create CPCommandResponseMsg with CRC
                CPCommandResponseMsg responseMsg = new CPCommandResponseMsg(response, true);
                // Create the message with CRC
                responseMsg.createMessage(response);
                String responseData = new String(responseMsg.getDataBytes());
                
                // Send response back to client
                // Use configuration from received message to send response back to correct client
                PhyConfiguration responseConfig = (PhyConfiguration) receivedMsg.getConfiguration();
                phy.send(responseData, responseConfig);
                
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
            return "Server Status: Running - All systems operational";
        } else if (cmd.startsWith("print ")) {
            String text = cmd.substring(6); // Remove "print " prefix
            if (text.startsWith("\"") && text.endsWith("\"")) {
                text = text.substring(1, text.length() - 1); // Remove quotes
            }
            return "Printed: " + text;
        } else {
            return "ERROR: Unknown command. Supported commands: status, print \"text\"";
        }
    }
}
