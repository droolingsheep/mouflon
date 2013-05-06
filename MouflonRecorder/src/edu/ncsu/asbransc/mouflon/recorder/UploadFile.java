package edu.ncsu.asbransc.mouflon.recorder;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Base64OutputStream;

public class UploadFile extends Service {

	private boolean mManual = false;
	private IBinder mBinder = new UploadBinder(); 
	
	public class UploadBinder extends Binder {
		UploadBinder getService() {
			return UploadBinder.this;
		}
	}

	@Override
	public void onCreate() {
		
		super.onCreate();
		
		
		
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mManual = intent.getBooleanExtra("edu.ncsu.asbransc.mouflon.recorder.ManualUpload", false);
		upload();
		return START_STICKY;
	}

	protected void upload() {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				UploadFile.this.doUpload();
			}
		});
		t.start();
		
	}

	/*@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id) {
		case DIALOG_CLEAR_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Would you like to clear the log database?")
					.setCancelable(false)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							DbAdapter dba = new DbAdapter(UploadFile.this);
							dba.open();
							dba.clearDB();
							dba.close();
						}
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							
						}
					});
			Looper.prepare();
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
		
	}*/

	protected void doUpload() {
		DbAdapter dba = new DbAdapter(this);
        dba.open();
        Cursor allLogs = dba.fetchAll();
        StringBuilder sb = new StringBuilder();
        allLogs.moveToFirst();
        sb.append("DateTime");
    	sb.append(",");
    	sb.append("Process");
    	sb.append(",");
    	sb.append("Type");
    	sb.append(",");
    	sb.append("Component");
    	sb.append(",");
    	sb.append("ActionString");
    	sb.append(",");
    	sb.append("Category");
    	sb.append("\n");
        while (!allLogs.isAfterLast()) {
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_TIME)));
        	sb.append(",");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_PROCESSTAG)));
        	sb.append(",");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_1)));
        	sb.append(",");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_2)));
        	sb.append(",");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_3)));
        	sb.append(",");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_4)));
        	sb.append("\n");
        	allLogs.moveToNext();
        }
        dba.close();
        File appDir = getDir("toUpload", MODE_PRIVATE);
        UUID uuid;
        uuid = MainScreen.getOrCreateUUID(this);
        long time = System.currentTimeMillis();
        String basename = uuid.toString() + "_AT_" +time;
        String filename = basename + ".zip.enc";
        File file = new File(appDir, filename);
        FileOutputStream out = null;
        ZipOutputStream outzip = null;
        CipherOutputStream outcipher = null;
        Cipher datac = null;
        
        File keyfile = new File(appDir, basename + ".key.enc");
        //Log.i("sb length", Integer.toString(sb.length()));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String email = prefs.getString(MainScreen.EMAIL_KEY, "");
        String emailFilename = "email.txt";
        String csvFilename = "mouflon_log_" + time + ".csv";
        try {
        	SecretKey aeskey = generateAESKey();
        	//Log.i("secret key", bytearrToString(aeskey.getEncoded()));
        	encryptAndWriteAESKey(aeskey, keyfile);
			datac = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			byte[] ivbytes = genIV();
			IvParameterSpec iv = new IvParameterSpec(ivbytes);
			datac.init(Cipher.ENCRYPT_MODE, aeskey,iv);
			out = new FileOutputStream(file);
			out.write(ivbytes);
			//Log.i("iv bytes", bytearrToString(ivbytes));
			outcipher = new CipherOutputStream(out, datac);
			outzip = new ZipOutputStream(outcipher);
			outzip.setMethod(ZipOutputStream.DEFLATED);
			//write the first file (e-mail address)
			String androidVersion = android.os.Build.VERSION.RELEASE;
			String deviceName = android.os.Build.MODEL;
			ZipEntry infoEntry = new ZipEntry("info.txt");
			outzip.putNextEntry(infoEntry);
			outzip.write((androidVersion + "\n" + deviceName).getBytes());
			outzip.closeEntry();
			ZipEntry emailEntry = new ZipEntry(emailFilename);
			outzip.putNextEntry(emailEntry);
			outzip.write(email.getBytes());
			outzip.closeEntry();
			ZipEntry csvEntry = new ZipEntry(csvFilename);
			outzip.putNextEntry(csvEntry);
			outzip.write(sb.toString().getBytes());
			outzip.closeEntry();
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try {
				outzip.close();
				outcipher.close();
				out.close();
			} catch (IOException e) {
				//ignore
			} catch (NullPointerException ne) {
				//ignore
			}
		}
        //here we actually upload the files 
        String containerFilename = basename + "_container.zip";
        File containerFile = new File(appDir, containerFilename);
        zipUp(containerFile, new File[] {file, keyfile});
        boolean success = uploadFile(containerFile);
        containerFile.delete();
		file.delete();
		keyfile.delete();
		if (success && prefs.getBoolean(MainScreen.DELETE_KEY, true)) {
			DbAdapter dba2 = new DbAdapter(this);
			dba2.open();
			dba2.clearDB();
			dba2.close();
		}
		if (!success && prefs.getBoolean(MainScreen.UPLOAD_KEY, false)) {
			Editor e = prefs.edit();
			e.putInt(MainScreen.DAY_KEY, 6); //reset it to run tomorrow if it fails
			e.commit();
		}
		String s = success ? "Upload complete. Thanks!" : "Upload Failed"; 
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(UploadFile.this)
																	.setSmallIcon(R.drawable.ic_launcher_bw)
																	.setContentTitle("Mouflon Recorder")
																	.setContentText(s)
																	.setAutoCancel(true)
																	.setOngoing(false); 
		
		
		if (mManual) { //only show a notification if we manually upload the file.
			Intent toLaunch = new Intent(UploadFile.this, MainScreen.class);
			//The notification has to go somewhere.
			PendingIntent pi = PendingIntent.getActivity(UploadFile.this, 0, toLaunch, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(pi);
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(1, mBuilder.build());
		}
		stopSelf();
	}

	

	private void zipUp(File out, File[] in) {
		FileOutputStream fout = null;
		ZipOutputStream zout = null;
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		try {
			fout = new FileOutputStream(out);
			zout = new ZipOutputStream(fout);
			zout.setMethod(ZipOutputStream.DEFLATED);
			
			for (File currFile : in) {
				FileInputStream fin = new FileInputStream(currFile);
				ZipEntry currEntry = new ZipEntry(currFile.getName());
				zout.putNextEntry(currEntry);
				while ((bytesRead = fin.read(buffer)) > 0 ) {
					zout.write(buffer, 0, bytesRead);
				}
				zout.closeEntry();
				fin.close();
			}
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		} finally {
			try {
				zout.close();
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			
		}
		

	}

	private byte[] genIV() {
		SecureRandom r = new SecureRandom();
		byte[] iv = new byte[16];
		r.nextBytes(iv);
		return iv;
	}

	private void encryptAndWriteAESKey(SecretKey aeskey, File dest) throws IOException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeySpecException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Cipher keyc;
		AssetManager am = getAssets();
		InputStream in = am.open("mouflon_key.pub"); 
		byte[] readFromFile = new byte[in.available()]; 
		//TODO check that this is 294 bytes and replace with a constant. in.available is not guaranteed to return a useful value
		in.read(readFromFile);
		keyc = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC"); 
		//ECB and CBC etc don't make sense for RSA, but the way this API is designed you have to specify something.
		KeyFactory kf = KeyFactory.getInstance("RSA");
		KeySpec ks = new X509EncodedKeySpec(readFromFile);
		RSAPublicKey key = (RSAPublicKey) kf.generatePublic(ks); 
		keyc.init(Cipher.ENCRYPT_MODE, key);
		//byte[] encrpytedKey = keyc.doFinal(aeskey.getEncoded());
		FileOutputStream out = new FileOutputStream(dest);
		CipherOutputStream outcipher = new CipherOutputStream(out, keyc);
		outcipher.write(aeskey.getEncoded());
		outcipher.close();
		out.close();
	}

	private SecretKey generateAESKey() throws NoSuchAlgorithmException {
		KeyGenerator aeskeygen = KeyGenerator.getInstance("AES");
		SecretKey aeskey = aeskeygen.generateKey();
		return aeskey;
	}
	
	/*private String bytearrToString(byte[] arr) {
		StringBuilder sb = new StringBuilder();
		//byte[] arr = sk.getEncoded();
		sb.append('[');
		sb.append(Integer.toHexString(arr[0] < 0 ? arr[0]+256:arr[0]));
		for(int i = 1; i < arr.length; i++){
			sb.append(',');
			sb.append(Integer.toHexString(arr[i] < 0 ? arr[i]+256:arr[i]));
		}
		sb.append(']');
		return sb.toString();
	}*/
	
	protected boolean uploadFile(File fileToUpload) {
		String lineEnding = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		boolean success = true;
		HttpURLConnection connection = null;
		try {
			URL dest = new URL("http://mouflon.csc.ncsu.edu/cgi-bin/upload.cgi");
	    	connection = (HttpURLConnection) dest.openConnection();
			
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			connection.setChunkedStreamingMode(0);
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			out.writeBytes(twoHyphens + boundary + lineEnding);
			//Log.i("uploadFile", fileToUpload.getName());
			out.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"" + fileToUpload.getName() + "\"" + lineEnding);
			out.writeBytes("Content-Type: application/octet-stream" + lineEnding);
			out.writeBytes("Content-Transfer-Encoding: base64" + lineEnding);
			out.writeBytes(lineEnding);
			encodeFileBase64(fileToUpload, out); //TODO this works fine for small files but for some file size between 256k and 2M it begins failing.
			
			out.writeBytes(lineEnding);
			out.writeBytes(twoHyphens + boundary + twoHyphens + lineEnding);
			//Log.i("UploadTest", "File uploaded");
			out.flush();
			out.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		}
		try {
			BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
			byte[] resp = new byte[80];
			int read = 0;
			if ((read = in.read(resp)) > 0) {
				String responseString = new String(resp, 0 , read);
				//Log.i("uploadFile", responseString);
				if(!responseString.equals(fileToUpload.getName())) {
					//Log.e("Upload", "File upload failed");
				}
			}
			//else 
				//Log.e("Upload", "No response received from server");
			
		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		}
		return success;
	}
	
	private void encodeFileBase64(File file, DataOutputStream out) {
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		Base64OutputStream out64 = new Base64OutputStream(out, Base64.NO_CLOSE);
		try{
			while ((bytesRead = in.read(buffer)) > 0) {				//this loop grabs more of the file and uploads it 4KB  at a time
				out64.write(buffer, 0, bytesRead);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
				out64.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		
		return mBinder;
	}
	
	

	
	
}
