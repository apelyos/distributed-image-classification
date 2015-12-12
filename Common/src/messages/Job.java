package messages;
import javax.xml.bind.annotation.*;

// sent by the manager for workers

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Job {
	@SuppressWarnings("unused")
	private Job () {
		// for jaxb
	}
	
	public Job (String imgURL, int serial, String managerUuid) {
		this.imageUrl = imgURL;
		this.serialNumber = serial; 
		this.managerUuid = managerUuid;
	}
	
    public int serialNumber;
    
    public String imageUrl;
    
    public String managerUuid;
}
