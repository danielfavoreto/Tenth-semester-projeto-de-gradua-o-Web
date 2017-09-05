package util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

public class ConexaoServidor extends AsyncTask<String, Void, String> {

	private Context mContext;
	
	public ConexaoServidor(Context mContext){
		
		this.mContext = mContext;
		
	}
	
	@Override
	public void onPostExecute(String result) {
		
	    super.onPostExecute(result);
	    
	}
	    
    private String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		
		StringBuilder sb = new StringBuilder();

		String line;
		
		try {

			br = new BufferedReader(new InputStreamReader(is));
			
			while ((line = br.readLine()) != null) {
				
				sb.append(line);
				
			}

		} catch (IOException e) {
			
			e.printStackTrace();
			
		} finally {
			
			if (br != null) {
				
				try {
					
					br.close();
					
				} catch (IOException e) {
					
					e.printStackTrace();
					
				}
			}
		}

		return sb.toString();

	}

	@Override
	public String doInBackground(String... urls) {
				
		try {
			
			URL url = new URL( urls[0] );
			
			String urlParameters  = urls[1];

        	if (urls[1].equals("testarConexao")){
        		
        		HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
        		httpUrlConnection.setConnectTimeout(7 * 1000);
        		httpUrlConnection.connect();
        		
        		if (httpUrlConnection.getResponseCode() == 200){
        			
        			return "Ok";
        			
        		}
        	}
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream caInput = new BufferedInputStream(mContext.getAssets().open("alerta.crt"));
			Certificate ca;
			
			try {
				
			    ca = cf.generateCertificate(caInput);
			    
			} finally {
				
			    caInput.close();
			    
			}

			String keyStoreType = KeyStore.getDefaultType();
			
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);

			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);
			
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), null);

			HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
			urlConnection.setRequestMethod( "POST" );
			urlConnection.setDoOutput( true );
			urlConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
			urlConnection.setRequestProperty( "Accept-Charset", "utf-8");
			
			urlConnection.setSSLSocketFactory(context.getSocketFactory());

			OutputStreamWriter request1 = new OutputStreamWriter(urlConnection.getOutputStream());
            request1.write(urlParameters);
            request1.flush();
            request1.close();
            
            InputStream input;
            
            int status = urlConnection.getResponseCode();
            
            if (status != 200){
            	
            	input = urlConnection.getErrorStream();
            	
            	throw (new Exception("Código de resposta http inválido: " + status));
            	
            }
            else {
            	
            	input = urlConnection.getInputStream();
    				    				            	
            }
            
           	String resposta = getStringFromInputStream (input);
			
        	JSONObject json = new JSONObject(resposta);
			
			if (urls[2].equals("enviarRequisicao")){

				String id = json.getString("id");
				
            	if (json.getString("status").equals("true")){
            		
            		return id;	
            		
            	}
            	else {
            		
            		return "false";
            		
            	}
				
			}
			else if (urls[2].equals("enviarAutenticacao")){
        		
            	if (json.getString("status").equals("true")){
            		
            		return json.getString("nomePessoa");
            		            		
            	}
            	else {
            		
            		return "false";
            		
            	}
			}
			
		} catch (Exception e){
			
			return "Erro: " + e.getMessage();
			
		}
		
		return "Falha Conexão";
		
	}
}