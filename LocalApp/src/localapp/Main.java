package localapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import dal.Configuration;
import dal.NodesMgmt;
import dal.Queue;
import dal.Storage;
import dal.NodesMgmt.NodeType;
import messages.Command;
import messages.Conclusion;
import messages.Command.CommandTypes;
import messages.JobResult.ImageSize;
import messages.GenericMessage;
import messages.Summary;


public class Main {
	private static Logger logger = Logger.getLogger("Local App");

	public static void main(String[] args) {
		File imageFile;
		int jobsPerWorker;
		boolean terminate = false;
		
		if (args.length < 3 || args.length > 4)
			throw new IllegalArgumentException("Invalid number of arguments entered");
		
		try {
		    String inputFile = args[0];
		    String outputFile = args[1];
		    jobsPerWorker = Integer.parseInt(args[2]);
		    if (args.length == 4 && args[3].equals("terminate"))
		    	terminate = true;
		    imageFile = new File(inputFile);
		    start(imageFile, outputFile, jobsPerWorker, terminate);
		} catch (IllegalArgumentException e) {
		    System.err.println("Arguments " + args[0] + " and " + args[1] + " must be strings.");
		    System.err.println("Argument " + args[2] + " must be an integer.");
		    System.exit(1);
		} catch (Exception e) {e.printStackTrace();} 
	}
	
	
	public static void start (File images, String outputFile, int jobsPerWorker, boolean termination) throws Exception {
		Queue<Command> manageQueue = new Queue<Command>(Configuration.QUEUE_MANAGE, Command.class);
		UUID key = UUID.randomUUID();
		Storage s3_files = new Storage(Configuration.FILES_BUCKET_NAME);
		logger.info("Uploads images files to S3");
		String s3_key = s3_files.putFile(images);
		logger.info("Creating command object");
		Command start = createCommand(s3_key, jobsPerWorker, termination, key);
		manageQueue.enqueueMessage(start);
		logger.info("Starting Manager");
		runManager();
		logger.info("Waiting for conclusion key");
		String summaryKey = waitForConclusionKey(key);
		BufferedReader s3Output = s3_files.get(summaryKey);
		logger.info("Fetching summary object");
		Summary summary = getSummary (s3Output);
		logger.info("Creating HTML output files");
		createHTMLs(summary, outputFile);
		logger.info("DONE!");
	}
	
	private static void runManager() throws Exception {
		NodesMgmt mgmt = new NodesMgmt(NodeType.MANAGEMENT);
		if (mgmt.getNumberOfRunningInstances() == 0)
			mgmt.runInstance();
	}
	
	private static Command createCommand(String s3_key, int jobsPerWorker, boolean termination, UUID key) {
		Command cmd = new Command();
		if (termination)
			cmd.type = CommandTypes.INITIATE_AND_TERMINATE;
		else
			cmd.type = CommandTypes.INITIATE;		
		cmd.fileKey = s3_key;
		cmd.jobsPerWorker = jobsPerWorker;
		cmd.key = key;
		return cmd;
	}
	
	private static String waitForConclusionKey(UUID key) throws Exception {
		Queue<Conclusion> mngRes = new Queue<Conclusion>(Configuration.QUEUE_MANAGE_RESULT+ "_" + key.toString(), Conclusion.class);
		Conclusion con = mngRes.waitForMessage();
		mngRes.deleteLastMessage();
		return con.fileKey;
	}
	
	private static Summary getSummary(BufferedReader s3Output) throws Exception {
		StringBuilder builder = new StringBuilder();
		String aux = "";
		while ((aux = s3Output.readLine()) != null)
		    builder.append(aux);
		String text = builder.toString();
		return (Summary)GenericMessage.fromXML(text).body;
	}
	
	
	private static void createHTMLs (Summary summary, String outputFile) throws IOException {
		final String htmlHeader = "<!DOCTYPE html>\n<html>\n<body>\n";
		final String htmlFooter = "\n</body>\n</html>";
		String mainHTMLBody = "";
		if (outputFile.endsWith(".html")) 
			outputFile = outputFile.substring(0, outputFile.length() - 5);
		File mainHTML = new File (outputFile+".html");
		BufferedWriter bw = new BufferedWriter(new FileWriter(mainHTML));	
		
		for (int i = 0; i < 5; i++) {
			ImageSize curSize = ImageSize.values()[i];
			logger.info("Processing " + curSize + " picture list to HTML output file");
			if (summary.getListOfSize(curSize) == null) {
				mainHTMLBody += "<h1>No pictures exists in size " + curSize.toString() + "</h1>\n";
			} else {
				String fileName = outputFile + "_" +curSize.toString()+".html";
				File sizeHTML = new File (fileName);
				String sizedBody = "<h1>"+curSize.toString()+"</h1>\n";
				mainHTMLBody += "<h1><a href=\" "+ fileName + "\">" + curSize.toString() +" Pictures</a></h1>\n";
				BufferedWriter sizedBW = new BufferedWriter(new FileWriter(sizeHTML));
				List<String> urls = summary.getListOfSize(curSize);
				for (int j = 0; j < urls.size() ; j++)
					sizedBody += "<a href=\"" + urls.get(j) + "\">\n<img src=\"" + urls.get(j) + "\"></a><br> \n";
				sizedBW.write(htmlHeader+sizedBody+htmlFooter);
				sizedBW.close();
			}
		}
		bw.write(htmlHeader+mainHTMLBody+htmlFooter);
		bw.close();
	}

	
	
	
	
	
}
