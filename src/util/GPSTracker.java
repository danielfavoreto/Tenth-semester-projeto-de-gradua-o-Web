package util;

import org.json.JSONObject;

import com.ufes.alertaufes.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class GPSTracker extends Service implements LocationListener {

    private final Context mContext;
    
    private String usuario;
    
    private String senha;
    
    private String nomePessoa;
    
    private ProgressDialog progressSearchLocation;
    
    private ImageButton buttonAlertar;
    
    public String id = "false";
    
    // flag for GPS Status
    public boolean isGPSEnabled = false;

    // flag for network status
    public boolean isNetworkEnabled = false;

    // flag for GPS Tracking is enabled 
    boolean isGPSTrackingEnabled = false;

    Location location;
    double latitude;
    double longitude;

    // The minimum distance to change updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; 

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0; 

    // Declaring a Location Manager
    protected LocationManager locationManager = null;

    // Store LocationManager.GPS_PROVIDER or LocationManager.NETWORK_PROVIDER information
    private String provider_info = null;

    // guardar o json da resposta do alerta
    public JSONObject jsonResposta;
    
    public GPSTracker(Context context, String usuario, String senha, String nomePessoa, ImageButton buttonAlertar) throws Exception {
    	
        this.mContext = context;
        this.usuario = usuario;
        this.senha = senha;
        this.nomePessoa = nomePessoa;
        this.buttonAlertar = buttonAlertar;
        getLocation();

    }
    
    /**
     * Try to get my current location by GPS or Network Provider
     * @throws Exception 
     */
    
    public void getLocation() throws Exception {

        try {
        	
        	progressSearchLocation = ProgressDialog.show(mContext, "Enviando alerta", " Aguarde",true);
        	
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // Capturando status do gps
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Capturando status da rede
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            if (isNetworkEnabled) {
            	
            	// Tenta obter localização baseada na rede
            	
                this.isGPSTrackingEnabled = true;
                
                provider_info = LocationManager.NETWORK_PROVIDER;

            } else if (isGPSEnabled) {
            	
            	// Tenta obter localização baseada no gps
            	
                this.isGPSTrackingEnabled = true;
                
                provider_info = LocationManager.GPS_PROVIDER;                               
                
            }

            if (provider_info != null) {
            	
            	locationManager.requestLocationUpdates(provider_info, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null) {
                	
                    location = locationManager.getLastKnownLocation(provider_info);
                    
                    updateGPSCoordinates();
                    
                }
            }
            // Usuário está utilizando 3G/4G, basta ativar o GPS
            else {
            	
            	showSettingsAlert("Habilite o GPS para enviar o alerta");
            	
            }
        }
        catch (Exception e)
        {
        	
        	progressSearchLocation.dismiss();
        	buttonAlertar.setClickable(true);
        	
        	throw new Exception("Não foi possível achar sua localização, tente novamente : " + e.getMessage());
        	
        }
    }

    /**
     * Update GPSTracker latitude and longitude
     */
    
    public void updateGPSCoordinates() {
    	
        if (location != null) {
        	
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            
        }
    }

    /**
     * GPSTracker latitude getter and setter
     * @return latitude
     */
    
    public double getLatitude() {
    	
        if (location != null) {
            latitude = location.getLatitude();
        }

        return latitude;
    }

    /**
     * GPSTracker longitude getter and setter
     * @return
     */
    
    public double getLongitude() {
    	
        if (location != null) {
        	
            longitude = location.getLongitude();
            
        }

        return longitude;
    }

    /**
     * GPSTracker isGPSTrackingEnabled getter.
     * Check GPS/wifi is enabled
     */
    
    public boolean getIsGPSTrackingEnabled() {

        return this.isGPSTrackingEnabled;
        
    }

    /**
     * Stop using GPS listener
     * Calling this method will stop using GPS in your app
     */
    
    public void stopUsingGPS() {
    	
        if (locationManager != null) {
        	
            locationManager.removeUpdates(GPSTracker.this);
            
        }
    }

    /**
     * Function to show settings alert dialog
     */
    public DialogInterface.OnClickListener showSettingsAlert(String message) {

        if (!isGPSEnabled){
        	
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
            
            //Setting Dialog Title
            alertDialog.setTitle("Habilitar GPS");

            //Setting Dialog Message
            alertDialog.setMessage(message);

            //On Pressing Setting button
            alertDialog.setPositiveButton("Habilitar", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) 
                {
                	
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    mContext.startActivity(intent);
                    
                }
            });

            //On pressing cancel button
            alertDialog.setNegativeButton("Fechar", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) 
                {
                    dialog.cancel();
                }
            });

            alertDialog.show();
    		return null;
       	       	
        }
        else {
        	
        	return null;
        	
        }
        
    }

	@Override
	public void onLocationChanged(Location location) {
	
		if (LocationManager.NETWORK_PROVIDER.equals(provider_info)){
						
			if (!isGPSEnabled){
				
				// enviar pela rede
								
				try {
					
					String resposta = new ConexaoServidor(mContext).execute("https://alerta.ufes.br/web/index.php?r=alerta/alertar&", "lat=" + String.valueOf(location.getLatitude()) + "&" + "lng=" + 
						String.valueOf(location.getLongitude()) + "&" + "usr=" + usuario + "&" + "psw=" + senha ,"enviarRequisicao").get();
				
				if (resposta.equals("false")){
					
					progressSearchLocation.dismiss();
					buttonAlertar.setClickable(true);
					
					new AlertDialog.Builder(mContext)
					.setTitle("Erro")
					.setMessage(resposta)
					.setNeutralButton("Fechar", showSettingsAlert("Habilite o GPS para uma localização mais exata"))
					.show();
					
					stopUsingGPS();
					return;
					
				}
				else {
					
					id = resposta;
					
					isNetworkEnabled = false;
					
					progressSearchLocation.dismiss();
					buttonAlertar.setClickable(true);
					
					hideAlertButton(mContext);
					
					showSuccessMessage(mContext);
					
					showBackButton(mContext);
					
					showSettingsAlert("Habilite o GPS para uma localização mais exata");
													
				}
				
				} catch (Exception e){
					
					progressSearchLocation.dismiss();
					buttonAlertar.setClickable(true);
					
					new AlertDialog.Builder(mContext)
					.setTitle("Erro")
					.setMessage("Erro: " + e.getMessage() + ", tente novamente")
					.setNeutralButton("Fechar", null)
					.show();
					
				}
				
				stopUsingGPS();
				
				// tem que mandar esperar pelo GPS
				
				provider_info = LocationManager.GPS_PROVIDER;
				
				// procurar por gps
				locationManager.requestLocationUpdates(provider_info, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null) {
                	
                    location = locationManager.getLastKnownLocation(provider_info);
                    updateGPSCoordinates();
                    
                }
				
			}
			else {
				
				// enviar pela rede
				
				try {
					
					String resposta = new ConexaoServidor(mContext).execute("https://alerta.ufes.br/web/index.php?r=alerta/alertar&", "lat=" + String.valueOf(location.getLatitude()) + "&" + "lng=" + 
							String.valueOf(location.getLongitude()) + "&" + "usr=" + usuario + "&" + "psw=" + senha ,"enviarRequisicao").get();
				
				if (resposta.equals("false")){
					
					progressSearchLocation.dismiss();
					buttonAlertar.setClickable(true);
					
					new AlertDialog.Builder(mContext)
					.setTitle("Erro")
					.setMessage(resposta)
					.setNeutralButton("Fechar", showSettingsAlert("Habilite o GPS para uma localização mais exata"))
					.show();
					
					stopUsingGPS();
					return;
					
				}
				else {
					
					id = resposta;
					
					isNetworkEnabled = false;
					
					progressSearchLocation.dismiss();
					buttonAlertar.setClickable(true);
										
					hideAlertButton(mContext);
					
					showSuccessMessage(mContext);
					
					showBackButton(mContext);				
					
					showSettingsAlert("Habilite o GPS para uma localização mais exata");
					
				}
				
				} catch (Exception e){
					
					progressSearchLocation.dismiss();
					buttonAlertar.setClickable(true);
					
					new AlertDialog.Builder(mContext)
					.setTitle("Erro")
					.setMessage("Erro: " + e.getMessage() + ", tente novamente")
					.setNeutralButton("Fechar", null)
					.show();
					
				}
				
				provider_info = LocationManager.GPS_PROVIDER;
				
				// procurar por gps
				locationManager.requestLocationUpdates(provider_info, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null) {
                	
                    location = locationManager.getLastKnownLocation(provider_info);
                    updateGPSCoordinates();
                    
                }
			}
		}
		else if (LocationManager.GPS_PROVIDER.equals(provider_info)){
			
			isGPSEnabled = true;
			
			try {
				
				String resposta = new ConexaoServidor(mContext).execute("https://alerta.ufes.br/web/index.php?r=alerta/update-precisao&" , "lat=" + String.valueOf(location.getLatitude()) + 
						"&" + "lng=" + String.valueOf(location.getLongitude()) + "&" + "usr=" + usuario + "&" + "psw=" + senha + "&" + "id=" + id ,"enviarRequisicao").get();
			
			if (resposta.equals("false")){
				
				progressSearchLocation.dismiss();
				buttonAlertar.setClickable(true);
				
				new AlertDialog.Builder(mContext)
				.setTitle("Erro")
				.setMessage("Falha no envio do alerta!")
				.setNeutralButton("Fechar", showSettingsAlert("Habilite o GPS para uma localização mais exata"))
				.show();
				
				stopUsingGPS();
				return;
				
			}
			else {
				
				progressSearchLocation.dismiss();
				buttonAlertar.setClickable(true);
								
			}
			
			} catch (Exception e){
				
				progressSearchLocation.dismiss();
				buttonAlertar.setClickable(true);
				
				new AlertDialog.Builder(mContext)
				.setTitle("Erro")
				.setMessage("Erro: " + e.getMessage() + ", tente novamente")
				.setNeutralButton("Fechar", null)
				.show();
				
			}
						
			stopUsingGPS();
			
		}
	}

	private void hideAlertButton (Context context){
		
		ImageButton buttonAlert = (ImageButton) ((Activity)context).findViewById(R.id.buttonAlerta);
		buttonAlert.setVisibility(ImageButton.GONE);
		
		TextView textViewMainCentralMonitoramento = (TextView) ((Activity)context).findViewById(R.id.textViewMainCentralMonitoramento);
		textViewMainCentralMonitoramento.setVisibility(TextView.GONE);
		
		TextView textViewMainTelefoneCentral = (TextView) ((Activity)context).findViewById(R.id.textViewMainTelefoneCentral);
		textViewMainTelefoneCentral.setVisibility(TextView.GONE);
		
	}
	
	private void showBackButton(Context context){
		
		Button backButton = (Button) ((Activity)context).findViewById(R.id.buttonVoltar);
		backButton.setVisibility(Button.VISIBLE);
		
	}
	
	private void showSuccessMessage(Context context){
		
		TextView textViewSuccess = (TextView) ((Activity)context).findViewById(R.id.textViewSuccess);
		textViewSuccess.setText(nomePessoa + ", seu ALERTA foi enviado com sucesso");
		textViewSuccess.setVisibility(TextView.VISIBLE);
		
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
	
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {

		// TODO Auto-generated method stub
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
