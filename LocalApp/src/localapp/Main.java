package localapp;

import java.io.BufferedReader;
import java.io.File;
import java.util.UUID;

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

	public static void main(String[] args) {
		File imageFile;
		int jobsPerWorker;
		boolean terminate;
		
		if (args.length != 3)
			throw new IllegalArgumentException("Exactly 3 parameters required!");
		
		    try {
		        String fileLocation = args[0];
		        jobsPerWorker = Integer.parseInt(args[1]);
		        terminate = Boolean.parseBoolean(args[2]);
		        imageFile = new File(fileLocation);
		        start(imageFile, jobsPerWorker, terminate);
		    } catch (IllegalArgumentException e) {
		        System.err.println("Argument" + args[0] + " must be a string.");
		        System.err.println("Argument" + args[1] + " must be an integer.");
		        System.err.println("Argument" + args[2] + " must be a boolean.");
		        System.exit(1);
		    } catch (Exception e) {e.printStackTrace();} 
	}
	
	
	public static void start (File images, int jobsPerWorker, boolean termination) throws Exception {
		Queue<Command> manageQueue = new Queue<Command>(Configuration.QUEUE_MANAGE, Command.class);
		UUID key = UUID.randomUUID();
		Storage s3_files = new Storage(Configuration.FILES_BUCKET_NAME);
		String s3_key = s3_files.putFile(images);
		Command start = createCommand(s3_key, jobsPerWorker, termination, key);
		manageQueue.enqueueMessage(start);
		NodesMgmt mgmt = new NodesMgmt(NodeType.MANAGEMENT);
		mgmt.runInstance();
		String summaryKey = waitForConclusionKey();
		BufferedReader s3Output = s3_files.get(summaryKey);
		Summary summary = getSummary (s3Output);
		createHTMLs(summary);
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
	
	private static String waitForConclusionKey() throws Exception {
		Queue<Conclusion> mngRes = new Queue<Conclusion>(Configuration.QUEUE_MANAGE_RESULT, Conclusion.class);
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
	
	
	private static void createHTMLs (Summary summary) {
		if (summary.getListOfSize(ImageSize.HUGE) != null)
			System.out.println("HUGE: " + summary.getListOfSize(ImageSize.HUGE));
		else
			System.out.println("there are no huge pics");
		if (summary.getListOfSize(ImageSize.LARGE) != null)
			System.out.println("LARGE: " + summary.getListOfSize(ImageSize.LARGE));
		else
			System.out.println("there are no large pics");
		if (summary.getListOfSize(ImageSize.MEDIUM) != null)
			System.out.println("MEDIUM: " + summary.getListOfSize(ImageSize.MEDIUM));
		else
			System.out.println("there are no medium pics");
		if (summary.getListOfSize(ImageSize.SMALL) != null)
			System.out.println("SMALL: " + summary.getListOfSize(ImageSize.SMALL));
		else
			System.out.println("there are no small pics");
		if (summary.getListOfSize(ImageSize.THUMBNAIL) != null)
			System.out.println("THUMBNAIL: " + summary.getListOfSize(ImageSize.THUMBNAIL));
		else
			System.out.println("there are no thumbnail pics");
		if (summary.getListOfSize(ImageSize.DEAD) != null)
			System.out.println("DEAD: " + summary.getListOfSize(ImageSize.DEAD));
		else
			System.out.println("there are no dead pics");
	}

	
	
	
	
	
}
