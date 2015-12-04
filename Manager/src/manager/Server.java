package manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import common.Command;
import common.Command.CommandTypes;
import common.Configuration;
import common.GenericMessage;
import dal.Queue;

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
				logger.info ("Got termination command, wating for termination & exiting");
				taskExecutor.shutdown();
				taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				break;
			}

		}
	}

	private Command waitForCommand() throws Exception {
		Queue manageQueue = new Queue(Configuration.QUEUE_MANAGE);
		String rawMsg = manageQueue.waitForMessage();
		logger.info ("Got msg: " + rawMsg);
		
		// parse command
		GenericMessage msg =  GenericMessage.fromXML(rawMsg);
		if (!msg.type.equals("common.Command"))
			throw new Exception("Invalid message type recieved.");
		
		manageQueue.deleteLastMessage();
		
		return (Command) msg.body;
	}
}