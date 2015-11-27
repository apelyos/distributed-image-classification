package common;
import javax.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Command {
	
	@XmlEnum
	public enum CommandTypes {
		SHUTDOWN, STARTUP, STATUS
	}
	
	public CommandTypes commandType;
}
