package messages;
import javax.xml.bind.annotation.*;

// sent by the localApp for manager 

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Command {
	
	@XmlEnum
	public enum CommandTypes {
		INITIATE, INITIATE_AND_TERMINATE
	}
	
	public CommandTypes type;
	
	public String fileKey;
	
	public int jobsPerWorker;
}
