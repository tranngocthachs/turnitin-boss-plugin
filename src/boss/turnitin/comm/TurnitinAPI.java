package boss.turnitin.comm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.TimeZone;

public class TurnitinAPI {

	public boolean proxyEnable = false;
	public String gmt = new String();
	public String uem = new String();
	public String ufn = new String();
	public String uln = new String();
	public String md5 = new String();
	public String shared_secret_key = new String();
	public String upw = new String();
	public String ctl = new String();
	public String cpw = new String();
	public String tem = new String();
	public String pfn = new String();
	public String pln = new String();
	public String ptl = new String();
	public String pdata = new String();
	public String assign = new String();
	public String dtstart = new String();
	public String dtdue = new String();
	public String ainst = new String();
	public String aid = new String();
	public String diagnostic = new String();
	public String encrypt = new String();
	public String fcmd = new String();
	public String fid = new String();
	public String url = new String();
	public String utp = new String();
	public String said = new String();
	public String ptype = new String();
	public String oid = new String();
	public String newassign = new String();
	public String newupw = new String();
	public String dis = new String();
	public String uid = new String();
	public String cid = new String();
	public String assignid = new String();
	public String username = new String();
	public String sub = new String();
	public String proxyHost = new String();
	public String proxyLogin = new String();
	public String proxyPwd = new String();
	public String remoteHost = new String();
	public String idsync = new String();

	public TurnitinAPI() {
		this.gmt = this.getGMT();
	}

	public static String urlEnc(String temp) {
		String newVar = temp;
		try {
			newVar = URLEncoder.encode(newVar, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println("Could not encode! " + e);
		}
		return newVar;
	}

	public String getMD5() throws Exception {
		String md5string = this.aid + this.assign + this.assignid + this.cid + this.cpw + this.ctl + this.diagnostic + this.dis + this.dtdue + this.dtstart
				+ this.encrypt + this.fcmd + this.fid + this.gmt + this.newassign + this.newupw + this.oid + this.pfn + this.pln + this.ptl + this.ptype
				+ this.said + this.tem + this.uem + this.ufn + this.uid + this.uln + this.upw + this.username + this.utp + this.shared_secret_key;
		java.security.MessageDigest md;
		md = MessageDigest.getInstance("MD5");
		md.update(md5string.getBytes());
		byte[] digest = md.digest();
		return this.byteArrayToHexString(digest);
	}

	public String getParams() throws Exception {
		String retval = new String();
		retval += "gmtime=" + this.gmt;
		retval += "&fid=" + this.fid;
		retval += "&fcmd=" + this.fcmd;
		retval += "&encrypt=" + this.encrypt;
		retval += "&md5=" + this.getMD5();
		retval += "&aid=" + this.aid;
		retval += "&said=" + this.said;
		retval += "&diagnostic=" + this.diagnostic;
		retval += "&uem=" + this.uem;
		retval += "&upw=" + TurnitinAPI.urlEnc(this.upw);
		retval += "&ufn=" + TurnitinAPI.urlEnc(this.ufn);
		retval += "&uln=" + TurnitinAPI.urlEnc(this.uln);
		retval += "&utp=" + this.utp;
		retval += "&ctl=" + TurnitinAPI.urlEnc(this.ctl);
		retval += "&cpw=" + TurnitinAPI.urlEnc(this.cpw);
		retval += "&tem=" + this.tem;
		retval += "&oid=" + this.oid;
		retval += "&newupw=" + TurnitinAPI.urlEnc(this.newupw);
		retval += "&assign=" + TurnitinAPI.urlEnc(this.assign);
		retval += "&dis=" + this.dis;
		retval += "&uid=" + TurnitinAPI.urlEnc(this.uid);
		retval += "&cid=" + TurnitinAPI.urlEnc(this.cid);
		retval += "&assignid=" + TurnitinAPI.urlEnc(this.assignid);
		retval += "&username=" + TurnitinAPI.urlEnc(this.username);
		retval += "&sub=" + TurnitinAPI.urlEnc(this.sub);
		retval += "&idsync=" + this.idsync;

		return retval;
	}

	public String getRedirectUrl() throws Exception {
		return this.remoteHost + "?" + this.getParams();
	}

	private String byteArrayToHexString(byte in[]) {
		byte ch = 0x00;
		int i = 0;

		if (in == null || in.length <= 0)
			return null;

		String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

		StringBuffer out = new StringBuffer(in.length * 2);
		
		while (i < in.length) {
			ch = (byte) (in[i] & 0xF0);  // Strip off
			ch = (byte) (ch >>> 4);
			// shift the bits down

			ch = (byte) (ch & 0x0F);
			// must do this is high order bit is on
		
			out.append(pseudo[(int)ch]); 
			// convert the nibble to a String Character

			ch = (byte) (in[i] & 0x0F);  
			// Strip off low nibble

			out.append(pseudo[(int)ch]); 
			// conver the nibble to a String Charager
			i++;
		}
		
		String rslt = new String(out);
		return rslt;

	}
		
	private String getGMT() {
		StringBuffer gmtime = new StringBuffer();
		TimeZone tz = TimeZone.getTimeZone("GMT");
		Calendar rightnow = Calendar.getInstance(tz);
		int gm_month = rightnow.get(Calendar.MONTH) + 1;
		int gm_year = rightnow.get(Calendar.YEAR);
		int gm_day = rightnow.get(Calendar.DAY_OF_MONTH);
		int gm_hour = rightnow.get(Calendar.HOUR_OF_DAY);
		int gm_min = rightnow.get(Calendar.MINUTE);
	
		gmtime = gmtime.append( gm_year );
		if(gm_month < 10) {
			gmtime = gmtime.append( "0" );
		} 
		gmtime = gmtime.append( gm_month );
		if(gm_day < 10) {
			gmtime = gmtime.append( "0" );
		}
		gmtime = gmtime.append( gm_day );
		if(gm_hour < 10) {
			gmtime = gmtime.append( "0" );
		}
		gmtime = gmtime.append( gm_hour );
		if(gm_min < 10) {
			gmtime = gmtime.append( "0" );
		} 
		gmtime = gmtime.append( gm_min );

		String gmt = gmtime.substring(0, 11);

		return gmt;
	}
	public static void main(String[] args) {
		TurnitinAPI tii = new TurnitinAPI();
		tii.remoteHost = "http://localhost:8080/Dummy/api";
		tii.ctl = "Programming for Dummies";
		try {
			System.out.println(tii.getRedirectUrl());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
