package messages;
import java.util.UUID;

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
	
	public UUID key;
	
	public String fileKey;
	
	public int jobsPerWorker;
}
