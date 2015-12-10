package dal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import common.Configuration;

public class Storage {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String _bucket;
	private AmazonS3Client _s3;
	
	public Storage (String name) throws FileNotFoundException, IOException {
		File creds = new File(Configuration.CREDS_FILE);
		if (creds.exists()) {
			_s3 = new AmazonS3Client(new PropertiesCredentials(creds));
		} else {
			_s3 = new AmazonS3Client();
		}
        _s3.setEndpoint(Configuration.S3_ENDPOINT);
        _bucket = name;
	}
	
	// Gets a file from s3 (dont forget to close the reader)
	public BufferedReader get(String key) {
		logger.info("Downloading an object");
        S3Object object = _s3.getObject(new GetObjectRequest(_bucket, key));
        logger.info("Content-Type: "  + object.getObjectMetadata().getContentType());
        return new BufferedReader(new InputStreamReader(object.getObjectContent()));
	}
	
	// Uploads a file and returns the key used in S3 
	public String put (File file) {        
		logger.info("Uploading a new object to S3 from a file\n");
        String key = file.getName().replace('\\', '_').replace('/','_').replace(':', '_');
        PutObjectRequest req = new PutObjectRequest(_bucket, key, file);
        _s3.putObject(req);
        return key;
	}
}
