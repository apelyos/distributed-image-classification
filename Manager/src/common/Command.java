package common;
import javax.xml.bind.annotation.*;

// sent by the localApp for manager 

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Command {
	
	@XmlEnum
	public enum CommandTypes {
		INITIATE, TERMINATE
	}
	
	public CommandTypes type;
	
	public String payload;
}
