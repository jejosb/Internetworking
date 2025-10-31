package cp;

import core.Msg;
import exceptions.IllegalMsgException;
import java.util.zip.CRC32;

public class CPCommandResponseMsg extends CPMsg {
    private long crc32;
    private String response;
    private boolean success;
    
    // Getter und Setter
    public long getCrc32() { 
        return crc32; 
    }
    
    public void setCrc32(long crc32) { 
        this.crc32 = crc32; 
    }
    
    public String getResponse() { 
        return response; 
    }
    
    public void setResponse(String response) { 
        this.response = response; 
    }
    
    public boolean isSuccess() { 
        return success; 
    }
    
    public void setSuccess(boolean success) { 
        this.success = success; 
    }
    
    public CPCommandResponseMsg() {}
    
    public CPCommandResponseMsg(String response, boolean success) {
        this.response = response;
        this.success = success;
    }
    
    // Public method to create message with CRC
    public void createMessage(String data) {
        create(data);
    }
    
    @Override
    protected void create(String data) {
        this.response = data;
        // CRC32 berechnen
        CRC32 crc = new CRC32();
        crc.update(data.getBytes());
        this.crc32 = crc.getValue();
        
        // Nachricht mit CRC erstellen
        String messageWithCrc = data + " " + this.crc32;
        super.create(messageWithCrc);
    }
    
    @Override
    protected Msg parse(String sentence) throws IllegalMsgException {
        String[] parts = sentence.split("\\s+");
        if (parts.length < 2) {
            throw new IllegalMsgException();
        }
        try {
            // Last part is CRC32
            this.crc32 = Long.parseLong(parts[parts.length - 1]);
            
            // Extract response: from index 1 (after 'cp') to before CRC
            // Format: "cp <response-text> <crc32>"
            StringBuilder responseBuilder = new StringBuilder();
            for (int i = 1; i < parts.length - 1; i++) {
                if (i > 1) responseBuilder.append(" ");
                responseBuilder.append(parts[i]);
            }
            this.response = responseBuilder.toString();
            
            // Validate CRC
            CRC32 crc = new CRC32();
            crc.update(this.response.getBytes());
            if (crc.getValue() != this.crc32) {
                throw new IllegalMsgException();
            }
            this.data = sentence;
            this.dataBytes = sentence.getBytes();
            return this;
        } catch (NumberFormatException e) {
            throw new IllegalMsgException();
        }
    }
}
