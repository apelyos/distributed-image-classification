package dal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;

public class NodesMgmt {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private AmazonEC2 _ec2;
	private NodeType _ofType;
	private String _defaultTag;
	private final String _tagKey = "Type";
	
	public enum NodeType {
		MANAGEMENT, WORKER
	}

	public NodesMgmt(NodeType type) throws FileNotFoundException, IOException {
		File creds = new File(Configuration.CREDS_FILE);
		if (creds.exists()) {
			_ec2 = new AmazonEC2Client(new PropertiesCredentials(creds));
		} else {
			_ec2 = new AmazonEC2Client();
		}
        _ec2.setEndpoint(Configuration.EC2_ENDPOINT);
        _ofType = type;
        _defaultTag = Configuration.EC2_TAG_PREFIX +  _ofType.toString();;
 	}
	
	public void runInstance() {
		runInstances(1);
	}
	
	public int getNumberOfRunningInstances() {
		return getAllRunningInstances().size();
	}
	
	// returns list of IDs
	public List<String> getAllRunningInstances() {
		logger.info("Get running instances request");
		DescribeInstancesRequest request = new DescribeInstancesRequest();

		List<String> valuesT1 = new ArrayList<String>();
		valuesT1.add(_defaultTag);
		Filter filter1 = new Filter("tag:" + _tagKey, valuesT1);
		List<String> valuesT2 = new ArrayList<String>();
		valuesT2.add("running"); 
		valuesT2.add("pending");
		Filter filter2 = new Filter("instance-state-name",valuesT2);

		DescribeInstancesResult result = _ec2.describeInstances(request.withFilters(filter1,filter2));
		List<Reservation> reservations = result.getReservations();
		
		List<String> instancesID = new ArrayList<String>();
		for (Reservation reservation : reservations) {
			List<Instance> instances = reservation.getInstances();
			for (Instance instance : instances) {
				instancesID.add(instance.getInstanceId());
			}
		}
		
		return instancesID;
	}
	
	public void terminateAllRunningInstances() {
		TerminateInstancesRequest request = new TerminateInstancesRequest(getAllRunningInstances());
		_ec2.terminateInstances(request);
	}
	
	
	// returns a list with the ID's of the running instances
	public List<String> runInstances(int numberOfInstances) {
		if (numberOfInstances <= 0)
			return null;
		
        RunInstancesRequest request = new RunInstancesRequest(Configuration.EC2_IMAGE_ID, numberOfInstances, numberOfInstances);
        request.setInstanceType(Configuration.EC2_INSTANCE_TYPE);
        IamInstanceProfileSpecification prof = new IamInstanceProfileSpecification();
        prof.setName(Configuration.EC2_IAM_PROFILE); // security concerns
        request.setIamInstanceProfile(prof);
        request.setUserData(getStartupScript()); // base64
        request.setKeyName(Configuration.EC2_KEYPAIR_NAME);
        List<Instance> instances = _ec2.runInstances(request).getReservation().getInstances();
        
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(new Tag(_tagKey, _defaultTag));
        tags.add(new Tag("Name", _ofType.toString().toLowerCase()));
        
        logger.info("Launch instances: " + instances);
        
        List<String> instancesID = new ArrayList<String>();
        for (Instance instance : instances) {
        	instancesID.add(instance.getInstanceId());
        }
        
        // tag the resources
		CreateTagsRequest tagreq = new CreateTagsRequest(instancesID, tags);
        _ec2.createTags(tagreq);
        
        return instancesID;
	}
	
	private String getStartupScript() {
		String script = String.join("\r\n"
		         , "#!/bin/bash"
		         , "yum install java-1.8.0 -q -y"
		         , "alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java"
		         , "aws s3 cp s3://dsps161-ass1-binaries/{0} /tmp/{0} --region us-west-2"
		         , "chmod +x /tmp/{0}"
		         , "java -jar /tmp/{0} {1}"
		         );
		
		if (_ofType == NodeType.WORKER) {
			// worker startup script
			script = MessageFormat.format(script, "Worker.jar", "");
		} else {
			// manager startup script
			script = MessageFormat.format(script, "JobsManager.jar", "");
		}
		
		return Base64.getEncoder().encodeToString(script.getBytes());
	}
	
	// can return null
	public static String getMyInstanceID() {
		return EC2MetadataUtils.getInstanceId();
	}
	
	// will work only on an actual aws instance
	public void commitSuicide() {
		if (getMyInstanceID() != null) {
			logger.info("Commiting suicide. See you in another life when we are both cats.");
			List<String> instancesID = new ArrayList<String>();
			instancesID.add(getMyInstanceID());
			TerminateInstancesRequest request = new TerminateInstancesRequest(instancesID);
			_ec2.terminateInstances(request);
		}
	}
}
