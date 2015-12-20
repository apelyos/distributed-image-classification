package manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dal.Configuration;
import dal.NodesMgmt;
import dal.Queue;
import dal.NodesMgmt.NodeType;
import messages.Command;
import messages.Job;
import messages.Command.CommandTypes;

public class Server {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public static void main(String[] args)  {
		Server srv = new Server();
		try {
			srv.startServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void startServer() throws Exception {
		ExecutorService taskExecutor = Executors.newCachedThreadPool();
		
		while (true) {
			Command cmd = waitForCommand();
			taskExecutor.execute(new JobsManager(cmd));
			
			if (cmd.type == CommandTypes.INITIATE_AND_TERMINATE) {
				logger.info ("Got termination command, wating for termination & terminating");
				taskExecutor.shutdown();
				taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				
				// terminate all workers
				terminateAllWorkers();
				
				// terminate myself
				NodesMgmt mgmt = new NodesMgmt(NodeType.MANAGEMENT);
				mgmt.commitSuicide();
				break;
			}
		}
	}
	
	private void terminateAllWorkers() throws  Exception {
		NodesMgmt workers = new NodesMgmt(NodeType.WORKER);
		int num = workers.getNumberOfRunningInstances();
		Queue<Job> jobsQueue = new Queue<Job>(Configuration.QUEUE_JOBS, Job.class);
		
		for (int i = 0; i < num; i++) {
			jobsQueue.enqueueMessage(Job.createTerminationJob());
		}
	}

	private Command waitForCommand() throws Exception {
		Queue<Command> manageQueue = new Queue<Command>(Configuration.QUEUE_MANAGE, Command.class);
		Command cmd = manageQueue.waitForMessage();
		manageQueue.deleteLastMessage();
		return cmd;
	}
}