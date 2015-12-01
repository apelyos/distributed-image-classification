package common;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GenericMessage {
    @XmlAttribute
    public String type;
 
    @XmlAttribute
    public String from;
 
    @XmlAnyElement(lax=true)
    public Object body;
    
    public static GenericMessage fromXML(String xml) throws JAXBException {
    	JAXBContext jc = JAXBContext.newInstance(GenericMessage.class, Job.class, Command.class, Conclusion.class, JobResult.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        StringReader sr = new StringReader(xml);
        GenericMessage obj = (GenericMessage) unmarshaller.unmarshal(sr);
        return obj;
    }
    
    public String toXML() throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(GenericMessage.class, Job.class, Command.class, Conclusion.class, JobResult.class);
		Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); //for nice indented output
        StringWriter sw = new StringWriter();
        marshaller.marshal(this, sw);
        return sw.toString();
    }
}