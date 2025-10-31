package cp;

import core.Msg;
import exceptions.IllegalMsgException;
import java.util.zip.CRC32;

public class CPCommandMsg extends CPMsg {
    private long crc32;
    private String command;
    
    // Getter und Setter
    public long getCrc32() { 
        return crc32; 
    }
    
    public void setCrc32(long crc32) { 
        this.crc32 = crc32; 
    }
    
    public String getCommand() { 
        return command; 
    }
    
    public void setCommand(String command) { 
        this.command = command; 
    }
    
    @Override
    protected void create(String data) {
        this.command = data;
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
        
        // Letztes Element ist CRC
        try {
            this.crc32 = Long.parseLong(parts[parts.length - 1]);
            
            // Rest ist Command
            StringBuilder commandBuilder = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) commandBuilder.append(" ");
                commandBuilder.append(parts[i]);
            }
            this.command = commandBuilder.toString();
            
            // CRC validieren
            CRC32 crc = new CRC32();
            crc.update(this.command.getBytes());
            long calculatedCrc = crc.getValue();
            
            if (calculatedCrc != this.crc32) {
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
//