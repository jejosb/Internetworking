package cp;

import core.Msg;
import exceptions.IllegalMsgException;
import java.util.zip.CRC32;

public class CPCommandResponseMsg extends CPMsg {
    public static final String CP_CMD_RES_HEADER = "command_response";
    private long crc32;
    private int id;
    private boolean success; // true=ok, false=error
    private int length;      // payload length in bytes
    private String response; // optional payload
    
    public long getCrc32() { return crc32; }
    public int getId() { return id; }
    public boolean isSuccess() { return success; }
    public int getLength() { return length; }
    public String getResponse() { return response; }

    public void setId(int id) { this.id = id; }
    public void setSuccess(boolean success) { this.success = success; }
    public void setResponse(String response) { this.response = response; }

    private static long crcFor(int id, boolean success, int length, String payload) {
        String status = success ? "ok" : "error";
        // CRC is calculated over "status length [payload]" WITHOUT id
        String base = status + " " + length + (length > 0 ? " " + payload : "");
        CRC32 crc = new CRC32();
        crc.update(base.getBytes());
        return crc.getValue();
    }

    public void create(int id, boolean success, String payload) {
        this.id = id;
        this.success = success;
        this.response = payload == null ? "" : payload;
        this.length = this.response.getBytes().length;
        this.crc32 = crcFor(this.id, this.success, this.length, this.response);
        String status = this.success ? "ok" : "error";
        String sentence = CP_CMD_RES_HEADER + " " + this.id + " " + status + " " + this.length
                + (this.length > 0 ? " " + this.response : "")
                + " " + this.crc32;
        super.create(sentence);
    }

    @Override
    protected void create(String data) {
        // Not used directly; default to OK with current id=0
        create(this.id, true, data);
    }
    
    @Override
    protected Msg parse(String sentence) throws IllegalMsgException {
        String[] parts = sentence.split("\\s+");
        if (parts.length < 5) throw new IllegalMsgException();
        if (!CP_CMD_RES_HEADER.equals(parts[0])) throw new IllegalMsgException();
        try {
            this.id = Integer.parseInt(parts[1]);
            String status = parts[2];
            if (!status.equals("ok") && !status.equals("error")) throw new IllegalMsgException();
            this.success = status.equals("ok");
            this.length = Integer.parseInt(parts[3]);
            this.crc32 = Long.parseLong(parts[parts.length - 1]);
            // Build payload string between length field and CRC (if any)
            String payload = "";
            if (parts.length > 5) {
                StringBuilder b = new StringBuilder();
                for (int i = 4; i < parts.length - 1; i++) {
                    if (i > 4) b.append(" ");
                    b.append(parts[i]);
                }
                payload = b.toString();
            }
            this.response = payload;
            // length check
            if (this.length != (this.response == null ? 0 : this.response.getBytes().length)) {
                throw new IllegalMsgException();
            }
            long calc = crcFor(this.id, this.success, this.length, this.response == null ? "" : this.response);
            if (calc != this.crc32) throw new IllegalMsgException();
            this.data = sentence;
            this.dataBytes = sentence.getBytes();
            return this;
        } catch (NumberFormatException e) {
            throw new IllegalMsgException();
        }
    }
}
