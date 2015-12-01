package manager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import javax.xml.bind.*;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import common.Job;
import common.JobResult;
import common.GenericMessage;
import common.Command;
import common.Conclusion;

public class DistManager {

	public static void main(String[] args) throws JAXBException, IOException {
		// create test object
		GenericMessage message1 = new GenericMessage();
		message1.from = InetAddress.getLocalHost().getHostName();
		message1.type = "job";
		Job job = new Job();
		job.content = "job content";
		job.name = "job name";
		message1.body = job; // object as body of msg
		
		// format outgoing message
		String xml = message1.toXML();
        System.out.println(xml);
        
        // init SQS
        AmazonSQS sqs = new AmazonSQSClient(new PropertiesCredentials(
        		//DistManager.class.getResourceAsStream("/AwsCredentials.properties")));
        		new FileInputStream("./AwsCredentials.properties")));
        sqs.setEndpoint("sqs.us-west-2.amazonaws.com");
        
        // get q url
        GetQueueUrlResult qUrl = sqs.getQueueUrl("Ass1_Manage");
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
            Job j = (Job) incoming.body;
            System.out.println(j.name);
            System.out.println(j.content);
            
            
            // Deletes a message
            System.out.println("Deleting the message.\n");
            sqs.deleteMessage(new DeleteMessageRequest(qUrl.getQueueUrl(), message.getReceiptHandle()));
        }

	}

}
