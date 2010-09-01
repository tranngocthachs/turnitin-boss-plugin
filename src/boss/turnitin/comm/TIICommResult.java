package boss.turnitin.comm;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TIICommResult {
	private Document returnXML;
	private int rcode = -1;
	private String rmessage = "";
	public static final String RCODE = "rcode";
	public static final String RMESSAGE = "rmessage";
	public static final String OBJECT_ID = "objectid";
	public static final int SUCCESS_CODE_LOWER_BOUND = 1;
	public static final int SUCCESS_CODE_UPPER_BOUND = 99;
	public static final int CREATE_USER_SUCCESS_CODE_FCMD2 = 11;
	public static final int SUBMIT_PAPER_SUCCESS_CODE_FCMD2 = 51;
	public static final int GET_ORI_SCORE_SUCCESS_CODE_FCMD2 = 61;
	public static final int GET_ORI_SCORE_ERROR_CODE_FCMD2_NOT_YET_AVAI = 415;
	/**
	 * Construct an TurnItIn communication result from the return message (in
	 * XML Document)
	 * 
	 * @param returnXML
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public TIICommResult(InputStream in) throws IOException {
		try {
			returnXML = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		} catch (SAXException e) {
			e.printStackTrace();
			return;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		}
		
		if (returnXML != null) {
			NodeList rcodeList = returnXML.getElementsByTagName(RCODE);
			if (rcodeList.getLength() == 1) {
				String rcodeStr = rcodeList.item(0).getTextContent().trim();
				try {
					int rcode = Integer.parseInt(rcodeStr);
					this.rcode = rcode;
				} catch (NumberFormatException e) {
					e.printStackTrace();
					return;
				}
			}
			
			NodeList rmsgList = returnXML.getElementsByTagName(RMESSAGE);
			if (rmsgList.getLength() == 1) {
				rmessage = rmsgList.item(0).getTextContent().trim();
			}
		}
	}
	
	public boolean isSuccessful() {
		return rcode >= SUCCESS_CODE_LOWER_BOUND && rcode <= SUCCESS_CODE_UPPER_BOUND;
	}

	public int getReturnCode() {
		return rcode;
	}
	
	public String getReturnMessage() {
		return rmessage;
	}
	
	public String getSingleValue(String tagName) {
		NodeList nodeList = returnXML.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0) {
			return nodeList.item(0).getTextContent().trim();
		}
		else {
			return null;
		}
	}
	
	public Document getReturnXML() {
		return returnXML;
	}

	public void setReturnXML(Document returnXML) {
		this.returnXML = returnXML;
	}
	
	public static void main(String args[]) throws ClientProtocolException, IOException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("https://submit.ac.uk/api.asp?encrypt=0&aid=2000");
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
		    TIICommResult result = new TIICommResult(entity.getContent());
		    System.out.println("Success: " + result.isSuccessful());
		    System.out.println("Return code: " + result.getReturnCode());
		    System.out.println("Return message: " + result.getReturnMessage());
		}
	}
}
