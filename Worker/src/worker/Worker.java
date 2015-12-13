package worker;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import dal.Configuration;
import dal.Queue;
import messages.Job;
import messages.JobResult;
import messages.JobResult.ImageSize;

public class Worker {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Queue<Job> jobsQueue;
	private Queue<JobResult> resultQueue;
	
    public static void main(String[] args) {
    	try {
			Worker worker = new Worker();
			while (true)
				worker.work();
			
			} catch (Exception e) {e.printStackTrace();}	
    }
    
    
    public Worker () throws Exception {
		jobsQueue = new Queue<Job>(Configuration.QUEUE_JOBS, Job.class);
		resultQueue = new Queue<JobResult>(Configuration.QUEUE_COMPLETED_JOBS, JobResult.class);
    }
    
    
    
    public void work() throws Exception { 
		logger.info("Getting new task");
		Job task = jobsQueue.waitForMessage();
		
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();  
			HttpGet httpget = new HttpGet(task.imageUrl);
			CloseableHttpResponse response = httpclient.execute(httpget);
			InputStream stream = response.getEntity().getContent(); 
			BufferedImage image = ImageIO.read(stream);
					  
		
		
		//try {
		//	BufferedImage image = ImageIO.read(new URL(task.imageUrl));
			int pixSum = image.getHeight()*image.getWidth();
			ImageSize size = calculateSize(pixSum);
			JobResult result = new JobResult(task.imageUrl, task.serialNumber, task.managerUuid, size);
			logger.info("Created jobResult number " + task.serialNumber + ", with url: " + task.imageUrl + ",sized: "+ size.toString());
			resultQueue.enqueueMessage(result);
		} catch (IOException e) {
			JobResult result = new JobResult(task.imageUrl, task.serialNumber, task.managerUuid, ImageSize.DEAD);
			resultQueue.enqueueMessage(result);
		}
		jobsQueue.deleteLastMessage();

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
