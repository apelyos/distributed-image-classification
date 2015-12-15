package dal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import messages.GenericMessage;

public class Queue<T>{ 
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String _queueName;
	private String _queueURL;
	private Message _lastMessage;
	private AmazonSQS _sqs;
	private final Class<T> _msgClass;
	
	private String getQueueUrl(String queueName) {
		try {
			return createQueue(queueName);
		} catch (QueueNameExistsException e) {
			return 	_sqs.getQueueUrl(queueName).getQueueUrl();
		}
	}
		
	public Queue (String queueName, Class<T> msgClass) throws FileNotFoundException, IOException {
        // init SQS
		File creds = new File(Configuration.CREDS_FILE);
		if (creds.exists()) {
			_sqs = new AmazonSQSClient(new PropertiesCredentials(creds));
		} else {
			_sqs = new AmazonSQSClient();
		}
        _sqs.setEndpoint(Configuration.SQS_ENDPOINT);
        _queueName = queueName;
        _queueURL = getQueueUrl(queueName);
        logger.info("got queue url: " + _queueURL);
        _msgClass = msgClass;
	}
	
	// returns URL of the new queue (or URL of an existing one)
	private String createQueue (String queueName) {
		logger.info("Creating/Getting SQS queue called: " + queueName);
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
        return _sqs.createQueue(createQueueRequest).getQueueUrl();
	}
	
	// don't use the queue object after this command
	public void deleteQueue () {
		logger.info("Deleting queue: " + _queueName);
        _sqs.deleteQueue(new DeleteQueueRequest(_queueURL));
        _sqs = null;
	}
	
	public void enqueueMessage (T message) throws AmazonServiceException, AmazonClientException, JAXBException {
        logger.info("Sending a message to queue: " + _queueName);
        GenericMessage msg = new GenericMessage(message);
        _sqs.sendMessage(new SendMessageRequest(_queueURL, msg.toXML()));        
	} 
	
	public T peekMessage() throws Exception {
		return peekMessage(0);
	}
	
	// this func only get a msg from q (does not remove it - you have to do it)
	@SuppressWarnings("unchecked")
	public T peekMessage(int waitFor) throws Exception {
        // Receive messages
		logger.info("Trying to recieve message from: " + _queueName);
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(_queueURL);
        receiveMessageRequest.setMaxNumberOfMessages(1);
        receiveMessageRequest.setWaitTimeSeconds(waitFor);
        
        List<Message> messages = _sqs.receiveMessage(receiveMessageRequest).getMessages();
        
        for (Message message : messages) {
        	logger.info("  Got Message");
        	logger.info("    Body:          " + message.getBody());
            
            _lastMessage = message;
            GenericMessage msg =  GenericMessage.fromXML(message.getBody());
    		if (!msg.type.equals(_msgClass.getName()))
    			throw new Exception("Invalid message type recieved.");
    		
            return (T) msg.body;
        }
		return null;
	}
	
	public T waitForMessage() throws Exception {
		T msg = null;
		
		while (msg == null) {
			msg = peekMessage(Configuration.DEFAULT_POLL_INTERVAL); //num of seconds to wait between polls
		}
		
		return msg;
	}
	
	// only delete
	public void deleteLastMessage() {
		if (_lastMessage != null) {
	        // Deletes a message
			logger.info("Deleting the last message.\n");
	        _sqs.deleteMessage(new DeleteMessageRequest(_queueURL, _lastMessage.getReceiptHandle()));
	        _lastMessage = null;
		}
	}
	
	// get + delete
	public T dequeueMessage() throws Exception {
		T msg = peekMessage();
		deleteLastMessage();
		return msg;
	}
	
	// returns number of messages in Q
	public int getNumberOfItems() {
		String key = "ApproximateNumberOfMessages";
		List<String> attrib = Arrays.asList(key);
		GetQueueAttributesResult res = _sqs.getQueueAttributes(new GetQueueAttributesRequest(_queueURL,attrib));
		return Integer.parseInt(res.getAttributes().get(key));
	}
}
