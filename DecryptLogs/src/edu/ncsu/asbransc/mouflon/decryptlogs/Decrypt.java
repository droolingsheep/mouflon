/*Mouflon: an Android app for collecting and reporting application usage.
    Copyright (C) 2013 Andrew Branscomb

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package edu.ncsu.asbransc.mouflon.decryptlogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Decrypt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//TODO make this handle fixing zip, unzipping, before decrypting
		//TODO clean up (delete encrypted intermediates) when done
		System.out.println(System.getProperty("java.class.path"));
		String keyFileName = args[0];
		String zipFileName = args[1];
		String outFileName = zipFileName.substring(0, zipFileName.length()-4);
		File f = new File(keyFileName); //the key file
		File outFile = new File(outFileName);
		Security.addProvider(new BouncyCastleProvider());
		System.out.println(f.getAbsolutePath());
		FileOutputStream fout = null;
		CipherInputStream cin = null;
		FileInputStream fin = null;
		try {
			SecretKey aeskey = readAndDecryptAESKey(f);
			//System.out.println(bytearrToString(aeskey.getEncoded()));
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			fin = new FileInputStream(new File(zipFileName)); //the zip file
			byte[] ivbytes = new byte[16];
			fin.read(ivbytes, 0, 16);
			//System.out.println(bytearrToString(ivbytes));
			IvParameterSpec iv = new IvParameterSpec(ivbytes);
			c.init(Cipher.DECRYPT_MODE, aeskey, iv);
			
			cin = new CipherInputStream(fin, c);
			//GZIPInputStream gzin = new GZIPInputStream(cin);
			fout = new FileOutputStream(outFile);
			byte[] buffer = new byte[4096];
			int bytesRead = 0;
			//System.out.println(fin.available());
			while ((bytesRead = cin.read(buffer)) > 0) {				//this loop grabs more of the file and uploads it 4KB  at a time
				System.out.println(bytesRead);
				fout.write(buffer, 0, bytesRead);
			}
		} catch (Exception e) {

			e.printStackTrace();
		} finally {
			try {
				cin.close();
				fin.close();
				fout.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
			
		try {
			FixZip.fixInvalidZipFile(outFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

	private static String bytearrToString(byte[] arr) {
		StringBuilder sb = new StringBuilder();
		// byte[] arr = sk.getEncoded();
		sb.append('[');
		sb.append(Integer.toHexString(arr[0] < 0 ? arr[0] + 256 : arr[0]));
		for (int i = 1; i < arr.length; i++) {
			sb.append(',');
			sb.append(Integer.toHexString(arr[i] < 0 ? arr[i] + 256 : arr[i]));
		}
		sb.append(']');
		return sb.toString();
	}

	private static SecretKey readAndDecryptAESKey(File src) throws IOException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeySpecException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException,
			NoSuchProviderException {
		Cipher keyc;
		FileInputStream in = new FileInputStream(new File("../keys/mouflon_key.pkcs8"));
		byte[] readFromFile = new byte[in.available()];
		in.read(readFromFile);
		keyc = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
		KeyFactory kf = KeyFactory.getInstance("RSA");
		KeySpec ks = new PKCS8EncodedKeySpec(readFromFile);
		RSAPrivateKey key = (RSAPrivateKey) kf.generatePrivate(ks);
		keyc.init(Cipher.DECRYPT_MODE, key);
		FileInputStream keyin = new FileInputStream(src);
		// CipherInputStream keyincipher = new CipherInputStream(keyin, keyc);
		byte[] encrpytedKey = new byte[keyin.available()];
		keyin.read(encrpytedKey);
		// System.out.println(bytearrToString(encrpytedKey));
		byte[] clearKey = keyc.doFinal(encrpytedKey);
		return new SecretKeySpec(clearKey, "AES");

	}
}
