package sandbox;
import java.io.IOException;
import javax.xml.bind.*;

import com.amazonaws.util.EC2MetadataUtils;

import dal.NodesMgmt;
import dal.Queue;
import messages.Command;
import messages.GenericMessage;
import messages.Command.CommandTypes;
import messages.JobResult.ImageSize;
import messages.Summary;

public class Sandbox {

	public static void main(String[] args) throws JAXBException, IOException {
		String instanceId = NodesMgmt.getMyInstanceID();
		System.out.println(instanceId);
		
		Summary sum = new Summary();
		sum.addEntry(ImageSize.HUGE, "monkey");
		sum.addEntry(ImageSize.HUGE, "monkey2");
		sum.addEntry(ImageSize.HUGE, "monkey3");
		sum.addEntry(ImageSize.MEDIUM, "med3");
		GenericMessage sumM = new GenericMessage(sum);
		System.out.println(sumM.toXML());
		
		GenericMessage sumR = GenericMessage.fromXML(sumM.toXML());
		Summary sumt = (Summary) sumR.body;
		System.out.println(sumt.getListOfSize(ImageSize.HUGE));
		System.out.println(sumt.getListOfSize(ImageSize.MEDIUM));
		System.out.println(sumt.getListOfSize(ImageSize.SMALL));
		
		//NodesMgmt mgmt = new NodesMgmt(NodeType.WORKER);
		// run an instance:
		//List<String> res1 = mgmt.runInstances(1);
		//System.out.println("IDs of the started instance: " + res1);
		
		// get running instances (IDs)
		//mgmt.terminateAllRunningInstances();
		//System.out.println(mgmt.getAllRunningInstances());
		
		// create test object
		Command cmd = new Command();
		cmd.type = CommandTypes.INITIATE_AND_TERMINATE;
		cmd.fileKey = "image-urls.txt";
		cmd.jobsPerWorker = 10;
		
		Queue<Command> q = new Queue<Command>("Ass1_Manage", Command.class);
		
		q.enqueueMessage(cmd);
		
		// format outgoing message
		/*String xml = message1.toXML();
        System.out.println(xml);
        
        // init SQS
        AmazonSQS sqs = new AmazonSQSClient(new PropertiesCredentials(
        		//DistManager.class.getResourceAsStream("/AwsCredentials.properties")));
        		new FileInputStream("./AwsCredentials.properties")));
        sqs.setEndpoint("sqs.us-west-2.amazonaws.com");
        
        // get q url
        GetQueueUrlResult qUrl = sqs.getQueueUrl("Ass1_Manage_test");
        System.out.println(qUrl.getQueueUrl());
        
        // send msg
        System.out.println("Sending a message to MyQueue.\n");
        sqs.sendMessage(new SendMessageRequest(qUrl.getQueueUrl(), xml));
        
        
        // Receive messages
        System.out.println("Receiving messages from MyQueue.\n");
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(qUrl.getQueueUrl());
        List<Message> messages = 
        		sqs.receiveMessage(receiveMessageRequest).getMessages();
        for (Message message : messages) {
            System.out.println("  Message");
            System.out.println("    MessageId:     " + message.getMessageId());
            System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
            System.out.println("    Body:          " + message.getBody());
            
            // process incoming message
            GenericMessage incoming = GenericMessage.fromXML(message.getBody());
            System.out.println(incoming.from);
            System.out.println(incoming.type);           
            
            // Deletes a message
            System.out.println("Deleting the message.\n");
            sqs.deleteMessage(new DeleteMessageRequest(qUrl.getQueueUrl(), message.getReceiptHandle()));
        }*/

	}

}
