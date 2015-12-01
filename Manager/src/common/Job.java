package common;
import javax.xml.bind.annotation.*;

// sent by the manager for workers

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Job {
    public String name;
    
    public String content;
}
