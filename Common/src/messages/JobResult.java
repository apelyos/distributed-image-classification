package messages;
import java.util.UUID;

import javax.xml.bind.annotation.*;

// sent by the workers for manager




@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JobResult {
	/*
	KEY TABLE:
		Thumbnail: up to 64x64
		Small: up to 256x256
		Medium: up to 640x480
		Large: up to 1024x768
		Huge: larger than 1024x768
		Dead: dead link
	 */
	@XmlEnum
	public enum ImageSizes {THUMBNAIL, SMALL, MEDIUM, LARGE, HUGE, DEAD}
    public int serialNumber;
    public String imageURL;
    public UUID managerUUID;
    
	@SuppressWarnings("unused")
	private JobResult () {}
	
	public JobResult (String imageURL, int serialNumber, UUID managerUUID) {
		this.imageURL = imageURL;
		this.serialNumber = serialNumber; 
		this.managerUUID = managerUUID;
	}
}


