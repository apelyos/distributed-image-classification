package common;



public class Configuration {
	public static String SQS_ENDPOINT = "sqs.us-west-2.amazonaws.com";
	public static String S3_ENDPOINT = "s3-us-west-2.amazonaws.com";
	public static String EC2_ENDPOINT = "ec2.us-west-2.amazonaws.com";
	public static String CREDS_FILE = "./AwsCredentials.properties";
	public static int DEFAULT_POLL_INTERVAL = 20; // seconds - max 20 - min 0
	public static String FILES_BUCKET_NAME = "dsps161-ass1-files";
	public static String QUEUE_MANAGE = "Ass1_Manage";
	public static String QUEUE_JOBS = "Ass1_Jobs";
	public static String EC2_IMAGE_ID = "ami-f0091d91"; // amazon image
	public static String EC2_INSTANCE_TYPE = "t2.micro";
	public static String EC2_TAG_PREFIX = "DSPS_Ass1_";
	public static String EC2_IAM_PROFILE = "DSPS_Ass1";
}
