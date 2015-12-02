package manager;


import java.io.BufferedReader;
import java.util.logging.Logger;
import common.Command;
import common.GenericMessage;
import common.Job;
import common.Command.CommandTypes;
import common.Configuration;
import dal.Queue;
import dal.Storage;

public class JobsManager implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private int _jobsPerWorker; 
	private int _jobCounter;
	
	public JobsManager(int jobsPerWorker) {
		_jobsPerWorker = jobsPerWorker;
		_jobCounter = 0;
	}
	
	@Override
	public void run() {
//		If the message is that of a new task it:
//		Downloads the input file from S3.
//		Distributes the operations to be performed on the images to the workers using SQS queue/s.
//		Checks the SQS message count and starts Worker processes (nodes) accordingly.
//		The manager should create a worker for every n messages, if there are no running workers.
//		If there are k active workers, and the new job requires m workers, then the manager should create m-k new workers, if possible.
//		Note that while the manager creates a node for every n messages, it does not delegate messages to specific nodes. All of the worker nodes take their messages from the same SQS queue; so it might be the case that with 2n messages, hence two worker nodes, one node processed n+(n/2) messages, while the other processed only n/2.
//		After the manger receives response messages from the workers on all the files on an input file, then it:
//		Creates a summary output file accordingly,
//		Uploads the output file to S3,
//		Sends a message to the application with the location of the file.
//
//	If the message is a termination message, then the manager:
//		Does not accept any more input files from local applications. However, it does serve the local application that sent the termination message.
//		Waits for all the workers to finish their job, and then terminates them.
//		Creates response messages for the jobs, if needed.
//		Terminates.
		
		try {
			Queue manageQueue = new Queue(Configuration.QUEUE_MANAGE);
			String rawMsg = manageQueue.waitForMessage();
			logger.info ("Got msg: " + rawMsg);
			GenericMessage msg =  GenericMessage.fromXML(rawMsg);
			if (!msg.type.equals("common.Command"))
				throw new Exception("Invalid message type recieved.");
			
			Command cmd = (Command) msg.body;
			
			manageQueue.deleteLastMessage();
			
			if (cmd.type == CommandTypes.INITIATE) {
				logger.info("Got initiate command");
				String fileKey = cmd.payload;
				logger.info("Got file Key: " + fileKey);
				
				Storage storage = new Storage(Configuration.FILES_BUCKET_NAME);
				
				BufferedReader reader = storage.get(fileKey);
				
				Queue jobsQueue = new Queue(Configuration.QUEUE_JOBS);
				
				// create jobs & send to queue
		        while (true) {
		            String line = reader.readLine();
		            if (line == null) break;
		            
		            if (!line.isEmpty()) {
		            	msg = new GenericMessage(createJob(line,_jobCounter));
		            	jobsQueue.enqueueMessage(msg.toXML());
		            	_jobCounter++;
		            }
		        }
		        
		        reader.close();
		        
		        int numQueued = jobsQueue.getNumberOfItems();
		        
		        logger.info("Number of queued items: " + numQueued);
		        
		        // to be continued...
				
			} else { //this is termination message
				logger.info("Got terminate command");
				
				
				
				
				
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private Job createJob(String imgURL, int serial) {
		logger.info("Creating job number " + serial + ", with url: " + imgURL);
		return new Job(imgURL, serial);
	}
}
