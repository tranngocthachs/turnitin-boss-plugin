package boss.turnitin.comm;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import boss.turnitin.TIIWSConfig;

import uk.ac.warwick.dcs.boss.model.dao.beans.Person;
import uk.ac.warwick.dcs.boss.plugins.PluginNotConfigurableException;

public class TurnItInComm {

	public static final Logger logger = Logger.getLogger(TurnItInComm.class);
	
	// silencing the httpclient logger
	static {
		Logger temp = Logger.getLogger("org.apache.http");
		if (temp != null)
			temp.setLevel(Level.INFO);
	}
	
	/**
	 * This method will communicate with TurnItIn web services to log a person
	 * in as instructor. This instructor account will be created in TurnItIn's
	 * record if it is not found. This method essentially uses FID1 and FCMD2 of
	 * TurnItIn API
	 * 
	 * @param person
	 * @return result of the communication
	 */
	public static TIICommResult createInstructorAndLogin(Person person) {
		TIICommResult result = null;

		// Create a Turnitin API object
		TurnitinAPI tii_api = new TurnitinAPI();
		Properties prop = null;
		try {
			prop = TIIWSConfig.getConfiguration("boss.turnitin");
		} catch (PluginNotConfigurableException e) {
			logger.fatal("shouldn't happen");
			return null;
		} catch (IOException e) {
			logger.error("io error", e);
			return null;
		}
		// Set the remote host
		tii_api.remoteHost = prop.getProperty(TIIWSConfig.APIURL_PROP_KEY);

		// Set the account information
		tii_api.aid = prop.getProperty(TIIWSConfig.AID_PROP_KEY);
		tii_api.shared_secret_key = prop
				.getProperty(TIIWSConfig.SECRETKEY_PROP_KEY);

		// Set the required information. MD5 and GMT are created automatically.
		tii_api.diagnostic = "0";
		tii_api.encrypt = "0";
		tii_api.uem = person.getEmailAddress();
		String[] names = person.getChosenName().split("\\s+");
		tii_api.ufn = names[0];
		tii_api.uln = names[names.length - 1];
		tii_api.utp = "2";

		tii_api.fid = "1";
		tii_api.fcmd = "2";
		tii_api.idsync = "1";

		// send the request
		String url = null;
		try {
			url = tii_api.getRedirectUrl();
		} catch (Exception e) {
			logger.error("error computing md5", e);
			return null;
		}
		logger.info("sending get request: " + url);
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = new TIICommResult(entity.getContent());
			}
		} catch (ClientProtocolException e) {
			logger.error("error sending request", e);
		} catch (IOException e) {
			logger.error("error sending request", e);
		}
		return result;
	}

	/**
	 * This will submit a paper to TurnItIn using quick submit function (FID5,
	 * FCMD2, and assignid=quicksubmit). The request has to be multi-part HTTP
	 * POST in order to "upload" the file to TurnItIn system.
	 * 
	 * @param instructor
	 * @param paperAuthor
	 * @param submittingFile
	 * @param paperTitle
	 * @return result of the communication
	 */
	public static TIICommResult quickSubmitAPaper(Person instructor,
			Person paperAuthor, File submittingFile, String paperTitle,
			boolean internetCheck, boolean submittedPaperCheck,
			boolean journalCheck) {
		TIICommResult result = null;
		// Create a Turnitin API object
		TurnitinAPI tii_api = new TurnitinAPI();
		Properties prop = null;
		try {
			prop = TIIWSConfig.getConfiguration("boss.turnitin");
		} catch (PluginNotConfigurableException e) {
			logger.fatal("shouldn't happen");
			return null;
		} catch (IOException e) {
			logger.error("io error", e);
			return null;
		}
		// Set the remote host
		tii_api.remoteHost = prop.getProperty(TIIWSConfig.APIURL_PROP_KEY);

		// Set the account information
		tii_api.aid = prop.getProperty(TIIWSConfig.AID_PROP_KEY);
		tii_api.shared_secret_key = prop
				.getProperty(TIIWSConfig.SECRETKEY_PROP_KEY);

		// Set the required information. MD5 and GMT are created automatically.
		tii_api.diagnostic = "0";
		tii_api.encrypt = "0";
		tii_api.uem = instructor.getEmailAddress();
		String[] names = instructor.getChosenName().split("\\s+");
		tii_api.ufn = names[0];
		tii_api.uln = names[names.length - 1];
		tii_api.utp = "2";

		tii_api.fid = "5";
		tii_api.fcmd = "2";
		tii_api.assignid = "quicksubmit";
		tii_api.ptl = paperTitle;
		String[] pNames = paperAuthor.getChosenName().split("\\s+");
		tii_api.pfn = pNames[0];
		tii_api.pln = pNames[pNames.length - 1];
		tii_api.ptype = "2";

		// building a multi-part http post request
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(tii_api.remoteHost);
		MultipartEntity reqEntity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
		try {
			reqEntity.addPart("aid", new StringBody(tii_api.aid));
			reqEntity.addPart("assignid", new StringBody(tii_api.assignid));
			reqEntity.addPart("diagnostic", new StringBody(tii_api.diagnostic));
			reqEntity.addPart("encrypt", new StringBody(tii_api.encrypt));
			reqEntity.addPart("fcmd", new StringBody(tii_api.fcmd));
			reqEntity.addPart("fid", new StringBody(tii_api.fid));
			reqEntity.addPart("gmtime", new StringBody(tii_api.gmt));
			FileBody fileBody = new FileBody(submittingFile);
			reqEntity.addPart("pdata", fileBody);
			reqEntity.addPart("pfn", new StringBody(tii_api.pfn));
			reqEntity.addPart("pln", new StringBody(tii_api.pln));
			reqEntity.addPart("ptl", new StringBody(tii_api.ptl));
			reqEntity.addPart("ptype", new StringBody(tii_api.ptype));
			reqEntity.addPart("uem", new StringBody(tii_api.uem));
			reqEntity.addPart("ufn", new StringBody(tii_api.ufn));
			reqEntity.addPart("uln", new StringBody(tii_api.uln));
			reqEntity.addPart("utp", new StringBody(tii_api.utp));
			reqEntity.addPart("md5", new StringBody(tii_api.getMD5()));
			if (internetCheck)
				reqEntity.addPart("internet_check", new StringBody("1"));
			if (submittedPaperCheck)
				reqEntity.addPart("s_paper_check", new StringBody("1"));
			if (journalCheck)
				reqEntity.addPart("journal_check", new StringBody("1"));
		} catch (Exception e) {
			logger.error("error constructing multipart post request", e);
			return null;
		}
		httppost.setEntity(reqEntity);
		try {
			logger.info("sending multipart post request: " + httppost.getURI());
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = new TIICommResult(entity.getContent());
			}
		} catch (ClientProtocolException e) {
			logger.error("error sending request", e);
		} catch (IOException e) {
			logger.error("error sending request", e);
		}
		return result;
	}

	/**
	 * Get originality report from TurnItIn for a given paper id. Since this
	 * call uses FID6 and FCMD2, the originality score will be returned as part
	 * of the returning xml
	 * 
	 * @param instructor
	 * @param oid
	 * @return result of the communication
	 */
	public static TIICommResult getOriginalityScore(Person instructor,
			String oid) {
		TIICommResult result = null;
		// Create a Turnitin API object
		TurnitinAPI tii_api = new TurnitinAPI();
		Properties prop = null;
		try {
			prop = TIIWSConfig.getConfiguration("boss.turnitin");
		} catch (PluginNotConfigurableException e) {
			logger.fatal("shouldn't happen");
			return null;
		} catch (IOException e) {
			logger.error("io error", e);
			return null;
		}
		// Set the remote host
		tii_api.remoteHost = prop.getProperty(TIIWSConfig.APIURL_PROP_KEY);

		// Set the account information
		tii_api.aid = prop.getProperty(TIIWSConfig.AID_PROP_KEY);
		tii_api.shared_secret_key = prop
				.getProperty(TIIWSConfig.SECRETKEY_PROP_KEY);

		// Set the required information. MD5 and GMT are created automatically.
		tii_api.diagnostic = "0";
		tii_api.encrypt = "0";
		tii_api.uem = instructor.getEmailAddress();
		String[] names = instructor.getChosenName().split("\\s+");
		tii_api.ufn = names[0];
		tii_api.uln = names[names.length - 1];
		tii_api.utp = "2";

		tii_api.fid = "6";
		tii_api.fcmd = "2";
		tii_api.oid = oid.trim();

		// send the request
		String url = null;
		try {
			url = tii_api.getRedirectUrl();
		} catch (Exception e) {
			logger.error("error computing md5", e);
			return null;
		}
		logger.info("sending get request: " + url);
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = new TIICommResult(entity.getContent());
			}
		} catch (ClientProtocolException e) {
			logger.error("error sending request", e);
		} catch (IOException e) {
			logger.error("error sending request", e);
		}
		return result;
	}

	/**
	 * Get the url to the originality report of a paper (using FID6 and FCMD1).
	 * To make sure such report is ready, one can call getOriginalityScore and
	 * check if the result is successful. Otherwise, if the report is not yet
	 * generated, it will be automatically redirect to the error callback url.
	 * 
	 * @see #getOriginalityScore(Person, String)
	 * @param instructor
	 * @param oid
	 * @return the url to GET the originality report
	 */
	public static String getOriginalityReportURL(Person instructor, String oid) {
		String url = null;
		// Create a Turnitin API object
		TurnitinAPI tii_api = new TurnitinAPI();

		Properties prop = null;
		try {
			prop = TIIWSConfig.getConfiguration("boss.turnitin");
		} catch (PluginNotConfigurableException e) {
			logger.fatal("shouldn't happen");
			return null;
		} catch (IOException e) {
			logger.error("io error", e);
			return null;
		}
		// Set the remote host
		tii_api.remoteHost = prop.getProperty(TIIWSConfig.APIURL_PROP_KEY);

		// Set the account information
		tii_api.aid = prop.getProperty(TIIWSConfig.AID_PROP_KEY);
		tii_api.shared_secret_key = prop
				.getProperty(TIIWSConfig.SECRETKEY_PROP_KEY);

		// Set the required information. MD5 and GMT are created automatically.
		tii_api.diagnostic = "0";
		tii_api.encrypt = "0";
		tii_api.uem = instructor.getEmailAddress();
		String[] names = instructor.getChosenName().split("\\s+");
		tii_api.ufn = names[0];
		tii_api.uln = names[names.length - 1];
		tii_api.utp = "2";

		tii_api.fid = "6";
		tii_api.fcmd = "1";
		tii_api.oid = oid.trim();

		try {
			url = tii_api.getRedirectUrl();
		} catch (Exception e) {
			logger.error("error computing md5", e);
			return null;
		}
		return url;
	}

	/**
	 * Delete a paper from TurnItIn for a given paper id. This call uses FID8
	 * and FCMD2.
	 * 
	 * @param instructor
	 * @param oid
	 * @return result of the communication
	 */
	public static TIICommResult deleteAPaper(Person instructor, String oid) {
		TIICommResult result = null;
		// Create a Turnitin API object
		TurnitinAPI tii_api = new TurnitinAPI();
		Properties prop = null;
		try {
			prop = TIIWSConfig.getConfiguration("boss.turnitin");
		} catch (PluginNotConfigurableException e) {
			logger.fatal("shouldn't happen");
			return null;
		} catch (IOException e) {
			logger.error("io error", e);
			return null;
		}
		// Set the remote host
		tii_api.remoteHost = prop.getProperty(TIIWSConfig.APIURL_PROP_KEY);

		// Set the account information
		tii_api.aid = prop.getProperty(TIIWSConfig.AID_PROP_KEY);
		tii_api.shared_secret_key = prop
				.getProperty(TIIWSConfig.SECRETKEY_PROP_KEY);

		// Set the required information. MD5 and GMT are created automatically.
		tii_api.diagnostic = "0";
		tii_api.encrypt = "0";
		tii_api.uem = instructor.getEmailAddress();
		String[] names = instructor.getChosenName().split("\\s+");
		tii_api.ufn = names[0];
		tii_api.uln = names[names.length - 1];
		tii_api.utp = "2";

		tii_api.fid = "8";
		tii_api.fcmd = "2";
		tii_api.oid = oid.trim();

		// send the request
		String url = null;
		try {
			url = tii_api.getRedirectUrl();
		} catch (Exception e) {
			logger.error("error computing md5", e);
			return null;
		}
		logger.info("sending get request: " + url);
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = new TIICommResult(entity.getContent());
			}
		} catch (ClientProtocolException e) {
			logger.error("error sending request", e);
		} catch (IOException e) {
			logger.error("error sending request", e);
		}
		return result;
	}

	public static void main(String[] args) {
		Person instructor = new Person();
		instructor.setChosenName("Firstname Lastname");
		instructor.setEmailAddress("user@email.com");
		System.out.println(getOriginalityReportURL(instructor, "3260"));
	}
}
