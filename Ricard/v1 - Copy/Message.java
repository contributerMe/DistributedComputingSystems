
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Type {
        REQUEST, REPLY, RELEASE
    }
    
    private Type type;
    private int timestamp;
    private int siteId;
    
    public Message(Type type, int timestamp, int siteId) {
        this.type = type;
        this.timestamp = timestamp;
        this.siteId = siteId;
    }
    
    public Type getType() {
        return type;
    }
    
    public int getTimestamp() {
        return timestamp;
    }
    
    public int getSiteId() {
        return siteId;
    }
    
    @Override
    public String toString() {
        return "Message[type=" + type + ", timestamp=" + timestamp + ", siteId=" + siteId + "]";
    }
}
