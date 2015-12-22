package worker;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

import dal.Configuration;
import dal.NodesMgmt;
import dal.Queue;
import dal.Storage;
import dal.NodesMgmt.NodeType;
import messages.Job;
import messages.JobResult;
import messages.JobResult.ImageSize;

public class Worker {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Queue<Job> jobsQueue;
	private HashMap<UUID,Queue<JobResult>> resultQueues;
	private String id; 
	private Date workerStartTime;
	private Date workerFinishTime;
	private long averageRunTime;
	private HashMap<String, ArrayList<String>> urls;
	private int urlCounter = 0;
	
    public static void main(String[] args) {
    	try {
			Worker worker = new Worker();
			worker.work();
			} catch (Exception e) {e.printStackTrace();}	
    }
    
    
    public Worker () throws Exception {
       	id = NodesMgmt.getMyInstanceID();
       	if (id == null)
       		id = UUID.randomUUID().toString();
		jobsQueue = new Queue<Job>(Configuration.QUEUE_JOBS, Job.class);
		resultQueues = new HashMap<UUID,Queue<JobResult>>();
		urls = new HashMap<String,ArrayList<String>>();
		urls.put("completed", new ArrayList<String>());
		averageRunTime = 0;
		workerStartTime = new Date();
    }

    
    public void work() throws Exception {
    	while(true) {
			logger.info("fetching new task");
			Job task = jobsQueue.waitForMessage();
			if (task.terminate) {
				jobsQueue.deleteLastMessage();
				break;
			}
			UUID jobID = task.managerUuid;
			if (!resultQueues.containsKey(jobID))
				resultQueues.put(jobID, new Queue<JobResult>(Configuration.QUEUE_COMPLETED_JOBS+"_"+jobID.toString(), JobResult.class));
			long startTaskTime = new Date().getTime();
			doJob(task);
			jobsQueue.deleteLastMessage();
			long endTaskTime = new Date().getTime();
			averageRunTime += (endTaskTime-startTaskTime);
			urlCounter++;
    	}
    	workerFinishTime = new Date();
    	if (urlCounter != 0)
    		averageRunTime = averageRunTime/urlCounter;
		logger.info("Finished working, Sending statistics...");
		sendStatistics();
		logger.info("Statistics sent, farewell *BOOM*");
		NodesMgmt workerNode = new NodesMgmt(NodeType.WORKER);
		workerNode.commitSuicide();
		logger.info("Worker is dead.");
    }
    
    public void doJob(Job task) throws Exception  { 
    	CloseableHttpClient httpclient = null;
    	HttpGet httpget = null;
    	CloseableHttpResponse response = null;
    	InputStream stream = null;
    	BufferedImage image = null;
    	int pixSum;
    	JobResult result;
    	ImageSize size;
    	
		try {
			logger.info("Handling new job with id: " + task.serialNumber + " from manager: " + task.managerUuid);
			httpclient = HttpClients.createDefault();  
			httpget = new HttpGet(task.imageUrl);
			response = httpclient.execute(httpget);
			stream = response.getEntity().getContent(); 
			image = ImageIO.read(stream);
			pixSum = image.getHeight()*image.getWidth();
			size = calculateSize(pixSum);
			
			result = new JobResult(task.imageUrl, task.serialNumber, task.managerUuid, size);
			logger.info("Created jobResult number " + task.serialNumber + ", with url: " + task.imageUrl + ", sized: "+ size.toString());
			urls.get("completed").add(task.imageUrl);
		} catch (Exception e) {
			String strErr = e.getClass().toString();
			result = new JobResult(task.imageUrl, task.serialNumber, task.managerUuid, ImageSize.DEAD);
			logger.info("Created Failed job number ");
			if (!urls.containsKey(strErr))
				urls.put(strErr, new ArrayList<String>());
			urls.get(strErr).add(task.imageUrl);
		} finally {
			// freeing resources
			if (image != null)
				image.flush();
			
			if (stream != null)
				stream.close();
			
			if (response != null)
				response.close();
			
			if (httpclient != null)
				httpclient.close();
		}
		
		try {
			resultQueues.get(task.managerUuid).enqueueMessage(result);
		} catch (QueueDoesNotExistException e) {
			e.printStackTrace();
		}
    }
    
    public void sendStatistics() throws Exception { 
		Storage s3_stats = new Storage(Configuration.STATISTICS_BUCKET_NAME);
		File statFile = new File ("statistics_"+ id +".txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(statFile));	
		bw.write(collectStats());
		bw.close();
		s3_stats.putFile(statFile);
    }
    
    public String collectStats() {
    	String statistics = "Statistics: \n";
    	statistics += "\tID: " + id + "\n";
    	statistics += "\tStart Time: " + workerStartTime.toString() + "\n";
    	statistics += "\tAverage URL Run-Time (in milliseconds): " + averageRunTime + "\n";
    	statistics += "\tTotal URLs: " + urlCounter + "\n";
    	statistics += "\tURLs Handled: " + urls.get("completed").size() + "\n";
    	for (int i = 0; i < urls.get("completed").size(); i++)
    		statistics += "\t\t"+urls.get("completed").get(i) + "\n";
    	
		for (HashMap.Entry<String, ArrayList<String> > entry : urls.entrySet()) {
		    String key = entry.getKey();
		    ArrayList<String> failedURLs = entry.getValue();
		    if (!key.equals("completed")) {
		    	statistics += "\tURLs Failed ("+ key +"): " + failedURLs.size() + "\n";
		    	for (int i = 0; i < failedURLs.size(); i++)
		    		statistics += "\t\t"+failedURLs.get(i) + "\n";
		    }
		}

    	statistics += "\tFinish Time: " + workerFinishTime.toString() + "\n";
    	return statistics;   	
    }
	
	private ImageSize calculateSize (int size) {
		logger.info("Calculating image size");
		if (size <= 64*64) 
			return ImageSize.THUMBNAIL; 
		else if (size <= 256*256)
			return ImageSize.SMALL;
		else if (size <= 640*480)
			return ImageSize.MEDIUM;
		else if (size <= 1024*768)
			return ImageSize.LARGE;
		else 
			return ImageSize.HUGE;		
	}
	
	/*
	KEY TABLE:
		Thumbnail: up to 64x64
		Small: up to 256x256
		Medium: up to 640x480
		Large: up to 1024x768
		Huge: larger than 1024x768
		Dead: dead link
	 */
    
}
