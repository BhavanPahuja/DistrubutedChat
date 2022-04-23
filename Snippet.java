package registry;

//Define format of snippets
public class Snippet {
    private String message;
    private String source_address;
    private int source_port;
    private int timestamp;

    //Constructor
    public Snippet(String msg, String source_a, int source_p, int time) {
        this.message = msg;
        this.source_address = source_a;
        this.source_port = source_p;
        this.timestamp = time;
    }

    //Get message contetn
    public String getContent() {
        return this.message;
    }

    //Get source address
    public String getSourceAddress() {
        return this.source_address;
    }

    //Get source port
    public int getSourcePort() {
        return this.source_port;
    }

    //Get timestamp
    public int getTimestamp() {
        return this.timestamp;
    }

    public String toString() {
        return this.timestamp + " " + this.message + " " + this.source_address + ":" + this.source_port;
    }
}