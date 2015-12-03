package manager;


public class Server {
	public static void main(String[] args)  {
		JobsManager manager = new JobsManager(10);
		manager.run();
	}
}
