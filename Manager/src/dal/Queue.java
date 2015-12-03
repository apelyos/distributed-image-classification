package dal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import common.Configuration;

public class Queue {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String _queueName;
	private String _queueURL;
	private Message _lastMessage;
	private AmazonSQS _sqs;
	
	private String getQueueUrl(String queueName) {
		return 	_sqs.getQueueUrl(queueName).getQueueUrl();
	}
	
	public Queue (String queueName) throws FileNotFoundException, IOException {
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
	}
	
	public void enqueueMessage (String message) {
        // send msg
        logger.info("Sending a message to queue: " + _queueName);
        _sqs.sendMessage(new SendMessageRequest(_queueURL, message));
	}
	
	public String peekMessage() {
		return peekMessage(0);
	}
	
	// only get
	public String peekMessage(int waitFor) {
        // Receive messages
		logger.info("Trying to recieve message from: " + _queueName);
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(_queueURL);
        receiveMessageRequest.setMaxNumberOfMessages(1);
        receiveMessageRequest.setWaitTimeSeconds(waitFor);
        
        List<Message> messages = _sqs.receiveMessage(receiveMessageRequest).getMessages();
        
        for (Message message : messages) {
        	logger.info("  Got Message");
        	logger.info("    MessageId:     " + message.getMessageId());
        	logger.info("    ReceiptHandle: " + message.getReceiptHandle());
        	logger.info("    Body:          " + message.getBody());
            
            _lastMessage = message;
            return message.getBody();
        }
		return null;
	}
	
	public String waitForMessage() {
		String msg = null;
		
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
	public String dequeueMessage() {
		String msg = peekMessage();
		deleteLastMessage();
		return msg;
	}
	
	public int getNumberOfItems() {
		String key = "ApproximateNumberOfMessages";
		List<String> attrib = Arrays.asList(key);
		GetQueueAttributesResult res = _sqs.getQueueAttributes(new GetQueueAttributesRequest(_queueURL,attrib));
		return Integer.parseInt(res.getAttributes().get(key));
	}
}
