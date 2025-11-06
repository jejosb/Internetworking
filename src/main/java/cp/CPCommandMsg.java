package cp;

import core.Msg;
import exceptions.IllegalMsgException;
import java.util.zip.CRC32;

public class CPCommandMsg extends CPMsg {
    public static final String CP_CMD_HEADER = "command";
    private long crc32;
    private int id;
    private int length;
    private String command;
    
    // Task 1.2.3.b: Add Getter/Setter methods as needed for other tasks.
    public long getCrc32() { 
        return crc32; 
    }
    public int getId() { return id; }
    public int getLength() { return length; }
    
    public void setId(int id) { this.id = id; }
    public void setLength(int length) { this.length = length; }
    
    public String getCommand() { 
        return command; 
    }
    
    public void setCommand(String command) { 
        this.command = command; 
    }
    
    // Task 1.2.3.a: Use the CRC32 class from Java to calculate the CRC checksum
    private static long crcFor(int id, int length, String command) {
        CRC32 crc = new CRC32();
        String payload = id + " " + length + (length > 0 ? " " + command : "");
        crc.update(payload.getBytes());
        return crc.getValue();
    }
    
    public void create(int id, String command) {
        this.id = id;
        this.command = command;
        this.length = command == null ? 0 : command.getBytes().length;
        this.crc32 = crcFor(this.id, this.length, this.command == null ? "" : this.command);
        String sentence = CP_CMD_HEADER + " " + this.id + " " + this.length
                + (this.length > 0 ? " " + this.command : "")
                + " " + this.crc32;
        super.create(sentence);
    }
    
    @Override
    protected void create(String data) {
        // Default: assume id already set externally to 0
        create(this.id, data);
    }
    
    @Override
    protected Msg parse(String sentence) throws IllegalMsgException {
        String[] parts = sentence.split("\\s+");
        // Minimum parts: "command" <id> <length> <crc32> = 4 parts
        // If length > 0, then command follows: "command" <id> <length> <command> <crc32> = 5+ parts
        if (parts.length < 4) {
            throw new IllegalMsgException();
        }
        if (!CP_CMD_HEADER.equals(parts[0])) throw new IllegalMsgException();
        try {
            this.id = Integer.parseInt(parts[1]);
            this.length = Integer.parseInt(parts[2]);
            this.crc32 = Long.parseLong(parts[parts.length - 1]);
            // Reconstruct command (may be empty)
            String cmd = "";
            if (parts.length > 4) {
                StringBuilder b = new StringBuilder();
                for (int i = 3; i < parts.length - 1; i++) {
                    if (i > 3) b.append(" ");
                    b.append(parts[i]);
                }
                cmd = b.toString();
            }
            this.command = cmd;
            if (this.length != (this.command == null ? 0 : this.command.getBytes().length)) {
                throw new IllegalMsgException();
            }
            long calc = crcFor(this.id, this.length, this.command);
            if (calc != this.crc32) throw new IllegalMsgException();
            this.data = sentence;
            this.dataBytes = sentence.getBytes();
            return this;
        } catch (NumberFormatException e) {
            throw new IllegalMsgException();
        }
    }
}
