package messages;

import java.util.UUID;

import javax.xml.bind.annotation.*;

// sent by the workers for manager




@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JobResult {

	@XmlEnum
	public enum ImageSize {THUMBNAIL, SMALL, MEDIUM, LARGE, HUGE, DEAD}
    public int serialNumber;
    public String imageURL;
    public UUID managerUUID;
    public ImageSize size;
    
	@SuppressWarnings("unused")
	private JobResult () {}
	
	public JobResult (String imageURL, int serialNumber, UUID managerUUID, ImageSize size) {
		this.imageURL = imageURL;
		this.serialNumber = serialNumber; 
		this.managerUUID = managerUUID;
		this.size = size;
	}
}


