package messages;
import java.util.UUID;

import javax.xml.bind.annotation.*;

// sent by the manager for workers

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Job {
	private Job () {
		// for jaxb
	}
	
	public static Job createTerminationJob() {
		Job job = new Job();
		job.terminate = true;
		job.serialNumber = -1;
		return job;
	}
	
	public Job (String imgURL, int serial, UUID managerUuid) {
		this.imageUrl = imgURL;
		this.serialNumber = serial; 
		this.managerUuid = managerUuid ;
		this.terminate = false;
	}
	
	public boolean terminate;
	
    public int serialNumber;
    
    public String imageUrl;
    
    public UUID managerUuid;
}
