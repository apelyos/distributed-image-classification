package messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import messages.JobResult.ImageSize;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Summary {
	private HashMap<ImageSize, ListWrapper> map;
	
	public Summary() { 
		map = new HashMap<ImageSize,ListWrapper>();
	}
	
	public void addEntry(ImageSize imgSize, String imgUrl) {
		ListWrapper wrap = map.get(imgSize);
		if (wrap != null) 
			wrap.getList().add(imgUrl);
		else {
			ArrayList<String> nl = new ArrayList<String>();
			nl.add(imgUrl);
			map.put(imgSize, new ListWrapper(nl));
		}
	}
	
	// can return null so beware
	public List<String> getListOfSize(ImageSize imgSize) {
		ListWrapper res =  map.get(imgSize);
		if (res != null) 
			return res.getList();
		
		return null;
	}
}

class ListWrapper {
    private List<String> list;
    
    @SuppressWarnings("unused")
	private ListWrapper() { }
    
    public ListWrapper(List<String> lst) {
    	list = lst;
    }
    
    public List<String> getList() {
    	return list;
    }
    
    public void setList (List<String> lst) {
    	this.list = lst;
    }
}
