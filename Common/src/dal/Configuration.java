package dal;



public class Configuration {
	public static final String S3_ENDPOINT = "s3-us-west-2.amazonaws.com";
	public static final String SQS_ENDPOINT = "sqs.us-west-2.amazonaws.com";
	public static final String EC2_ENDPOINT = "ec2.us-west-2.amazonaws.com";
	public static final String CREDS_FILE = "./AwsCredentials.properties";
	public static final int DEFAULT_POLL_INTERVAL = 20; // seconds - max 20 - min 0
	public static final String FILES_BUCKET_NAME = "dsps161-ass1-files";
	public static final String BINARIES_BUCKET_NAME = "dsps161-ass1-binaries";
	public static final String QUEUE_MANAGE = "Ass1_Manage";
	public static final String QUEUE_JOBS = "Ass1_Jobs";
	public static final String QUEUE_COMPLETED_JOBS = "Ass1_JobResults";
	public static final String QUEUE_MANAGE_RESULT = "Ass1_ManageResults";
	public static final String EC2_IMAGE_ID = "ami-f0091d91"; // amazon image
	public static final String EC2_INSTANCE_TYPE = "t2.micro";
	public static final String EC2_TAG_PREFIX = "DSPS_Ass1_";
	public static final String EC2_IAM_PROFILE = "DSPS_Ass1";
	public static final String EC2_KEYPAIR_NAME = "DSPS_Ass1_key";
}
