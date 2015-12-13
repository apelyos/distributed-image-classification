package messages;
import javax.xml.bind.annotation.*;

// sent by the manager for localApp

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Conclusion {
	public String fileKey;
	
	@SuppressWarnings("unused")
	private Conclusion() { }
	
	public Conclusion(String key) {
		fileKey = key;
	}
}
