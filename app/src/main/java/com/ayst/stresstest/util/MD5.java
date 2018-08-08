package com.ayst.stresstest.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class MD5 {

	private static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F' };
	
	public static String toHexString(byte[] b) {  
		 StringBuilder sb = new StringBuilder(b.length * 2);  
		 for (int i = 0; i < b.length; i++) {  
		     sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);  
		     sb.append(HEX_DIGITS[b[i] & 0x0f]);  
		 }  
		 return sb.toString();  
	}

	public static String md5ForFile(String filename) {
		InputStream fis = null;
		byte[] buffer = new byte[1024];
		int numRead = 0;
		MessageDigest md5;
		try{
			fis = new FileInputStream(filename);
			md5 = MessageDigest.getInstance("MD5");
			long total = 0;
			while((numRead=fis.read(buffer)) > 0) {
				md5.update(buffer,0,numRead);
				total+=numRead;
			}
			Log.d("MD5", "md5sum.total="+total);
			fis.close();
			return toHexString(md5.digest());	
		} catch (Exception e) {
			Log.e("MD5", "md5sum.exception="+e.getMessage());
			if (fis != null) {
			    try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
			}
			return null;
		}
	}
	
    public static String md5ForString(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(
                    string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }
}
