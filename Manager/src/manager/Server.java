package manager;


public class Server {
	public static void main(String[] args)  {
		JobsManager manager = new JobsManager(5);
		manager.run();
	}
}
