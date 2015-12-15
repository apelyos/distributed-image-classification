package worker;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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
	private Queue<JobResult> resultQueue;
	private String id;
	private Date workerStartTime;
	private Date workerFinishTime;
	private long averageRunTime;
	private List<String> handledURLs = new ArrayList<String>();
	private List<String> failedURLs = new ArrayList<String>();
	private static final int POLL_INTERVAL = 5;

	
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
		resultQueue = new Queue<JobResult>(Configuration.QUEUE_COMPLETED_JOBS, JobResult.class);
		averageRunTime = 0;
		workerStartTime = new Date();
    }

    
    public void work() throws Exception {
    	int loopCount = 0;
    	while(true) {
			logger.info("fetching new task");
			Job task = fetchTask();
			if (task == null)
				break;
			long startTaskTime = new Date().getTime();
			doJob(task);
			jobsQueue.deleteLastMessage();
			long endTaskTime = new Date().getTime();
			averageRunTime += (endTaskTime-startTaskTime);
			loopCount++;
    	}
    	workerFinishTime = new Date();
    	averageRunTime = averageRunTime/loopCount;
		logger.info("Finished working, Sending statistics...");
		sendStatistics();
		logger.info("Statistics sent, farewell *BOOM*");
		NodesMgmt workerNode = new NodesMgmt(NodeType.WORKER);
		workerNode.commitSuicide();
		logger.info("Worker is dead.");
    }
    
    private Job fetchTask() throws Exception {
    	Job task = null;
    	for(int i = 0; i < 3 && task == null ; i++)
    		task = jobsQueue.peekMessage(POLL_INTERVAL);
    	return task;
    }
    
    
    public void doJob(Job task) throws Exception { 
		
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();  
			HttpGet httpget = new HttpGet(task.imageUrl);
			CloseableHttpResponse response = httpclient.execute(httpget);
			InputStream stream = response.getEntity().getContent(); 
			BufferedImage image = ImageIO.read(stream);
			int pixSum = image.getHeight()*image.getWidth();
			ImageSize size = calculateSize(pixSum);
			JobResult result = new JobResult(task.imageUrl, task.serialNumber, task.managerUuid, size);
			logger.info("Created jobResult number " + task.serialNumber + ", with url: " + task.imageUrl + ",sized: "+ size.toString());
			resultQueue.enqueueMessage(result);
			handledURLs.add(task.imageUrl);
		} catch (IOException e) {
			JobResult result = new JobResult(task.imageUrl, task.serialNumber, task.managerUuid, ImageSize.DEAD);
			resultQueue.enqueueMessage(result);
			failedURLs.add(task.imageUrl);
		}
    }
    
    public void sendStatistics() throws Exception { 
		Storage s3_stats = new Storage(Configuration.STATISTICS_BUCKET_NAME);
		File statFile = new File ("statistics.txt");
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
    	statistics += "\tTotal URLs: " + (handledURLs.size()+failedURLs.size()) + "\n";
    	statistics += "\tURLs Handled: " + handledURLs.size() + "\n";
    	for (int i = 0; i < handledURLs.size(); i++)
    		statistics += "\t\t"+handledURLs.get(i) + "\n";
    	statistics += "\tURLs Failed (IOException): " + failedURLs.size() + "\n";
    	for (int i = 0; i < failedURLs.size(); i++)
    		statistics += "\t\t"+failedURLs.get(i) + "\n";
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
