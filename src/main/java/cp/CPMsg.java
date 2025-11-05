package cp;

import core.Msg;
import exceptions.IWProtocolException;
import exceptions.IllegalMsgException;

class CPMsg extends Msg {
    protected static final String CP_HEADER = "cp";
    @Override
    protected void create(String sentence) {
        data = CP_HEADER + " " + sentence;
        this.dataBytes = data.getBytes();
    }

    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        CPMsg parsedMsg;
        if(!sentence.startsWith(CP_HEADER))
            throw new IllegalMsgException();

        String[] parts = sentence.split("\\s+", 2);
        if(parts.length < 2)
            throw new IllegalMsgException();
        String rest = parts[1];
        if(rest.startsWith(CPCookieRequestMsg.CP_CREQ_HEADER)) {
            parsedMsg = new CPCookieRequestMsg();
        } else if(rest.startsWith(CPCookieResponseMsg.CP_CRES_HEADER)) {
            parsedMsg = new CPCookieResponseMsg();
        } else if(rest.startsWith(CPCommandResponseMsg.CP_CMD_RES_HEADER)) {
            // WICHTIG: zuerst command_response prüfen, da es mit "command" beginnt
            parsedMsg = new CPCommandResponseMsg();
        } else if(rest.startsWith(CPCommandMsg.CP_CMD_HEADER)) {
            parsedMsg = new CPCommandMsg();
        } else {
            // Fallback: treat as command
            parsedMsg = new CPCommandMsg();
        }

        parsedMsg = (CPMsg) parsedMsg.parse(rest);
        return parsedMsg;
    }

}
