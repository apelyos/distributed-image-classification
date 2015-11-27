package manager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.List;
import javax.xml.bind.*;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import common.Job;
import common.Message;
import common.Command;
import common.Command.CommandTypes;

public class DistManager {

	public static void main(String[] args) throws JAXBException, IOException {
		// create jaxb context
		JAXBContext jc = JAXBContext.newInstance(Message.class, Job.class, Command.class);
		
		// create test object
		Message message1 = new Message();
		message1.from = InetAddress.getLocalHost().getHostName();
		message1.type = "job";
		Job job = new Job();
		job.content = "job content";
		job.name = "job name";
		message1.body = job; // object as body of msg
		
		// format outgoing message
		Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); //for nice indented output
        StringWriter sw = new StringWriter();
        marshaller.marshal(message1, sw);
        System.out.println(sw);
        
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
        sqs.sendMessage(new SendMessageRequest(qUrl.getQueueUrl(), sw.toString()));
        
        
        // Receive messages
        System.out.println("Receiving messages from MyQueue.\n");
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(qUrl.getQueueUrl());
        List<com.amazonaws.services.sqs.model.Message> messages = 
        		sqs.receiveMessage(receiveMessageRequest).getMessages();
        for (com.amazonaws.services.sqs.model.Message message : messages) {
            System.out.println("  Message");
            System.out.println("    MessageId:     " + message.getMessageId());
            System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
            System.out.println("    Body:          " + message.getBody());
            
            // process incoming message
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            StringReader sr = new StringReader(message.getBody());
            Message incoming = (Message) unmarshaller.unmarshal(sr);
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
