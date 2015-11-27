package common;
import javax.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Message {
    @XmlAttribute
    public String type;
 
    @XmlAttribute
    public String from;
 
    @XmlAnyElement(lax=true)
    public Object body;
}
