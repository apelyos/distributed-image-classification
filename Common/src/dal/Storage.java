package dal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

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
	
	// Gets a file from s3 (don't forget to close the reader)
	public BufferedReader get(String key) {
		logger.info("Downloading an object with key: " + key);
        S3Object object = _s3.getObject(new GetObjectRequest(_bucket, key));
        logger.info("Content-Type: "  + object.getObjectMetadata().getContentType());
        return new BufferedReader(new InputStreamReader(object.getObjectContent()));
	}
	
	public String putFile (File file) {
		return putFile(file, false);
	}
	
	public String putStream (String key, InputStream stream) throws UnsupportedEncodingException {
		key = key.replace('\\', '_').replace('/','_').replace(':', '_');
		ObjectMetadata metadata = new ObjectMetadata();
		_s3.putObject(_bucket, key, stream, metadata);
		return key;
	}
	
	// Uploads a file and returns the key used in S3 
	public String putFile (File file, boolean isPublic) {        
		logger.info("Uploading a new object to S3 from a file\n");
        String key = file.getName().replace('\\', '_').replace('/','_').replace(':', '_');
        PutObjectRequest req;
        if (isPublic) {
        	req = new PutObjectRequest(_bucket, key, file).withCannedAcl(CannedAccessControlList.PublicRead);
        } else {
        	req = new PutObjectRequest(_bucket, key, file);
        }
        _s3.putObject(req);
        return key;
	}
}
