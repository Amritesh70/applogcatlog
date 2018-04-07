package citylink.com.applogcatloglibrary;

/**
 * Created by Amritesh Sinha on 4/3/2018.
 */

public class Packet {
    private String id;
    private String packetInformation;
    private String statusPacket;
    private String jobId;
    private String dateTime;

    public Packet(){

    }
    public Packet(String packetInformation, String statusPacket, String jobId) {
        this.packetInformation = packetInformation;
        this.statusPacket = statusPacket;
        this.jobId = jobId;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPacketInformation() {
        return packetInformation;
    }

    public void setPacketInformation(String packetInformation) {  this.packetInformation = packetInformation;   }

    public String getStatusPacket() {
        return statusPacket;
    }

    public void setStatusPacket(String statusPacket) {
        this.statusPacket = statusPacket;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
