package manager;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import com.amazonaws.util.StringInputStream;

import dal.Configuration;
import dal.NodesMgmt;
import dal.Queue;
import dal.Storage;
import dal.NodesMgmt.NodeType;
import messages.Command;
import messages.Conclusion;
import messages.GenericMessage;
import messages.Job;
import messages.JobResult;
import messages.Summary;

public class JobsManager implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private int _jobCounter;
	private Command _cmd;
	private UUID _uuid;
	
	public JobsManager(Command cmd) {
		_jobCounter = 0;
		_cmd = cmd;
		_uuid = cmd.key; 
	}
	
	@Override
	public void run() {
	//		If the message is that of a new task it:
	//		Downloads the input file from S3.
	//		Distributes the operations to be performed on the images to the workers using SQS queue/s.
	//		Checks the SQS message count and starts Worker processes (nodes) accordingly.
	//		The manager should create a worker for every n messages, if there are no running workers.
	//		If there are k active workers, and the new job requires m workers, then the manager should create m-k new workers, if possible.
	//		After the manger receives response messages from the workers on all the files on an input file, then it:
	//		Creates a summary output file accordingly,
	//		Uploads the output file to S3,
	//		Sends a message to the application with the location of the file.
	//
	//	If the message is a termination message, then the manager:
	//		Does not accept any more input files from local applications. 
	//		However, it does serve the local application that sent the termination message.
	//		Waits for all the workers to finish their job, and then terminates them.
	//		Creates response messages for the jobs, if needed.
	//		Terminates.
		
		try {
			logger.info("Got initiate command");
			String fileKey = _cmd.fileKey;
			logger.info("Got file Key: " + fileKey);
			
			// get file from storage
			Storage storage = new Storage(Configuration.FILES_BUCKET_NAME);
			BufferedReader reader = storage.get(fileKey);
			
			// read file by line, create jobs & send to queue
			Queue<Job> jobsQueue = new Queue<Job>(Configuration.QUEUE_JOBS, Job.class);
	        while (true) {
	            String line = reader.readLine();
	            if (line == null) break;

	            if (!line.isEmpty()) {
	            	jobsQueue.enqueueMessage(createJob(line,_jobCounter));
	            	_jobCounter++;
	            }
	        }
	        reader.close();
	        
	        int numJobsQueued = jobsQueue.getNumberOfItems();
	        logger.info("Number of queued items: " + numJobsQueued);
	        
	        // start required worker nodes
	        startWorkers(numJobsQueued);
	        
	        // wait for all job result messages
	        logger.info("Wating for jobs completions");
	        Queue<JobResult> jobsComplete = new Queue<JobResult>(Configuration.QUEUE_COMPLETED_JOBS, JobResult.class);
	        Summary sum = new Summary();
	        
	        while (_jobCounter > 0) {
	        	JobResult res = jobsComplete.waitForMessage();
	        	logger.info("Got job result with id " + res.serialNumber);
	        	//if (res.managerUUID.equals(_uuid)) {
	        		sum.addEntry(res.size, res.imageURL);
		        	jobsComplete.deleteLastMessage();
		        	_jobCounter--;
	        	//}
	        } 
	        
	        // send summary file
	        String conKey = uploadSummary(sum);
	        
	        // send conclusion message
	        sendConclusionMessage(conKey);

			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String uploadSummary(Summary sum)throws Exception {
		logger.info("Sending summary file to S3");
		GenericMessage sumM = new GenericMessage(sum);
		Storage resStore = new Storage(Configuration.FILES_BUCKET_NAME);
		String conKey = resStore.putStream("summary_" + _uuid, new StringInputStream(sumM.toXML()));
		return conKey;
	}
 
	private void sendConclusionMessage(String conKey) throws FileNotFoundException, IOException, JAXBException {
		logger.info("Sending conclusion message to client");
		Queue<Conclusion> mngRes = new Queue<Conclusion>(Configuration.QUEUE_MANAGE_RESULT, Conclusion.class);
		mngRes.enqueueMessage(new Conclusion(conKey));
	}

	private void startWorkers(int numJobsQueued) throws FileNotFoundException, IOException {
		NodesMgmt mgmt = new NodesMgmt(NodeType.WORKER);
		int numWorkersNeeded =  (numJobsQueued / _cmd.jobsPerWorker);
		if (numWorkersNeeded == 0)
			numWorkersNeeded = 1;
		numWorkersNeeded -= mgmt.getNumberOfRunningInstances();
		logger.info("Creating " + numWorkersNeeded + " workers");
		mgmt.runInstances(numWorkersNeeded);
	}
	
	private Job createJob(String imgURL, int serial) {
		logger.info("Creating job number " + serial + ", with url: " + imgURL);
		return new Job(imgURL, serial, _uuid);
	}
}
