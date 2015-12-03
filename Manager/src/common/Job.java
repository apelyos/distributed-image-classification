package common;
import javax.xml.bind.annotation.*;

// sent by the manager for workers

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Job {
	@SuppressWarnings("unused")
	private Job () {
		// for jaxb
	}
	
	public Job (String imgURL, int serial) {
		imageUrl = imgURL;
		serialNumber = serial; 
	}
	
    public int serialNumber;
    
    public String imageUrl;
}
