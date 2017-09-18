package view;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.ufes.alertaufes.LoginActivity;
import com.ufes.alertaufes.R;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import util.ConexaoServidor;
import util.GPSTracker;

public class MainActivity extends Activity{

	private SharedPreferences sharedPreferences;
	private String userLoginAndPasswordPreferences = "sharedPreferencesLoginAndPassword";
	private String lastRequestPreferences = "sharedPreferencesLastRequest";
	private String usuario = "usuario";
	private String senha = "senha";
	private String nome = "nome";
	private String hora = "hora";
	private String telefone = "telefone";
	protected LocationManager locationManager = null;
	private boolean exit = false;
	private GPSTracker gpsTracker = null;
	private Timer timerSearchForGpsLocation;
	private int limitCounterSecondsForGpsLocation = 0;  
	private static final String[] INITIAL_PERMS = {
			    Manifest.permission.ACCESS_FINE_LOCATION,
			    Manifest.permission.ACCESS_COARSE_LOCATION
			  };
	private static final String permissionFineLocation = Manifest.permission.ACCESS_FINE_LOCATION;
	private static final String permissionCoarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION;
	private ImageButton buttonAlertar;
	private Button buttonSair;
	private static final int INITIAL_REQUEST=1337;
	private final static long tempoToleranciaNovaRequisicao = 300000; // 5 minutos = 300 milisegundos 
	private final static int tempoToleranciaProcuraGps = 120; // 2 minutos
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	    	
    	buttonAlertar = (ImageButton) findViewById(R.id.buttonAlerta);
    	
    	buttonSair = (Button) findViewById(R.id.buttonExit);
    	
    	final Button buttonVoltar = (Button) findViewById(R.id.buttonVoltar);
    			
		// usado toda vez que o app é iniciado, pra saber se existe o arquivo contendo o usuario e senha
		
		sharedPreferences = getSharedPreferences(userLoginAndPasswordPreferences,Context.MODE_PRIVATE);
		
		if (!sharedPreferences.contains(usuario) && !sharedPreferences.contains(senha)){
			
			sharedPreferences = getSharedPreferences(lastRequestPreferences,Context.MODE_PRIVATE);
			
			if (!sharedPreferences.contains(hora)){
				
				SharedPreferences.Editor editor = sharedPreferences.edit();
			
				// escreve uma quantidade de milisegundos aleatória (01/10/2011) para a primeira autenticação
				
				editor.putString(hora, String.valueOf(1317427200));
				editor.commit();		
				
			}
		
			// chama login Activity
			
			Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
			startActivityForResult(intent,1);
			
		}
				
		if (!isNetworkAvailable()){
			
			// wifi/3G/4G não está ativado
			
			new AlertDialog.Builder(MainActivity.this)
			.setTitle("Atenção")
			.setMessage("Ative sua conexão com a internet ")
			.setNeutralButton("Fechar", null)
			.show();
			
		}
				
		if ((int) Build.VERSION.SDK_INT >= 23){
			
			if ((MainActivity.this.checkCallingOrSelfPermission(permissionFineLocation) != (int)PackageManager.PERMISSION_GRANTED) &&
					(MainActivity.this.checkCallingOrSelfPermission(permissionCoarseLocation) != (int)PackageManager.PERMISSION_GRANTED))
			{

				ActivityCompat.requestPermissions(MainActivity.this, INITIAL_PERMS, INITIAL_REQUEST);
				
			}
									
		}
		
		buttonSair.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				new AlertDialog.Builder(MainActivity.this)
				.setTitle("Atenção")
				.setMessage("Deseja realmente se deslogar ? ")
				.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						SharedPreferences.Editor editor = sharedPreferences.edit();
						editor.clear().commit();
						
						sharedPreferences = getSharedPreferences(userLoginAndPasswordPreferences,Context.MODE_PRIVATE);
						
						editor = sharedPreferences.edit();
						editor.clear().commit();
												
						Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
						startActivityForResult(intent,1);
						
					}
				})
				.setNegativeButton("Não", null)
				.show();
								
			}
		});
		
		buttonAlertar.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
												
				try {
					
					buttonAlertar.setClickable(false);
					
					sharedPreferences = getSharedPreferences(lastRequestPreferences,Context.MODE_PRIVATE);
					
					long horarioUltimaRequisicao = Long.parseLong(sharedPreferences.getString(hora, String.valueOf(Calendar.getInstance().getTimeInMillis())));
					
					long horarioAtual = Calendar.getInstance().getTimeInMillis();
					
					long horarioLiberado = horarioAtual - horarioUltimaRequisicao;
					
					if (horarioLiberado < tempoToleranciaNovaRequisicao){
						
						long segundos = TimeUnit.MILLISECONDS.toSeconds(tempoToleranciaNovaRequisicao - horarioLiberado);
						long minutos = TimeUnit.MILLISECONDS.toMinutes(tempoToleranciaNovaRequisicao - horarioLiberado); 
									
						if (segundos <= 60){
							
							buttonAlertar.setClickable(true);
							
							new AlertDialog.Builder(MainActivity.this)
							.setTitle("Atenção")
							.setMessage("Você poderá enviar um novo alerta em " + String.valueOf((int) segundos) + " segundos")
							.setNeutralButton("Fechar", null)
							.show();
							
						}
						else {
							
							buttonAlertar.setClickable(true);
							
							new AlertDialog.Builder(MainActivity.this)
							.setTitle("Atenção")
							.setMessage("Você poderá enviar um novo alerta em " + String.valueOf(minutos) + " minuto(s)")
							.setNeutralButton("Fechar", null)
							.show();
							
						}
												
						return;
						
					}
				
					if (!isNetworkAvailable()){
					
						// wifi/3G/4G não está ativado

						buttonAlertar.setClickable(true);
						
						new AlertDialog.Builder(MainActivity.this)
						.setTitle("Atenção")
						.setMessage("Dispositivo não está conectado à internet ")
						.setNeutralButton("Fechar", null)
						.show();
					
						return;
					
					}
									
					String testeResposta = new ConexaoServidor(MainActivity.this).execute("http://google.com","testarConexao").get();
				
					if (!testeResposta.equals("Ok")) {
					
						buttonAlertar.setClickable(true);
						
						new AlertDialog.Builder(MainActivity.this)
						.setTitle("Atenção")
						.setMessage("Dispositivo não está conectado à internet ")
						.setNeutralButton("Fechar", null)
						.show();
						
						return;
					
					}
				
					vibrateOnClickAlertButton(MainActivity.this,500);
					
					new AsyncTaskEx().execute();

				} catch (Exception e) {
					
					buttonAlertar.setClickable(true);
					
					Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
											
				}
			}
		});			
		
		buttonVoltar.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				TextView textViewSuccess = (TextView) findViewById(R.id.textViewSuccess);
				textViewSuccess.setVisibility(TextView.GONE);
				
				TextView textViewMainCentralMonitoramento = (TextView) MainActivity.this.findViewById(R.id.textViewMainCentralMonitoramento);
				textViewMainCentralMonitoramento.setVisibility(TextView.VISIBLE);
				
				TextView textViewMainTelefoneCentral = (TextView) MainActivity.this.findViewById(R.id.textViewMainTelefoneCentral);
				textViewMainTelefoneCentral.setVisibility(TextView.VISIBLE);
				
				buttonVoltar.setVisibility(Button.GONE);
				
				buttonAlertar.setVisibility(Button.VISIBLE);
				
			}
		});
		
	}
	
	private void vibrateOnClickAlertButton(Context mContext,long timeToVibrate){
		
		Vibrator vibrateOnClickAlertButton = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		vibrateOnClickAlertButton.vibrate(timeToVibrate);
		
	}
	
	private class AsyncTaskEx extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

			return null;
			
		}

		@Override
		protected void onPostExecute(String res) {

			try {
				
				sharedPreferences = getSharedPreferences(userLoginAndPasswordPreferences,Context.MODE_PRIVATE);
				
				gpsTracker = new GPSTracker(MainActivity.this,sharedPreferences.getString("usuario", "admin"),sharedPreferences.getString("senha", "admin"),
							sharedPreferences.getString("nome", "Usuário"), sharedPreferences.getString("telefone", "00000000000"),buttonAlertar);
				
				sharedPreferences = getSharedPreferences(lastRequestPreferences,Context.MODE_PRIVATE);
				
				SharedPreferences.Editor editor = sharedPreferences.edit();
		
				editor.putString(hora, String.valueOf(Calendar.getInstance().getTimeInMillis()));
				editor.commit();
								
				if (!gpsTracker.isGPSEnabled){
										
					timerSearchForGpsLocation = new Timer();
					
					timerSearchForGpsLocation.scheduleAtFixedRate(new TimerTask() {
						
				        @Override
				        public void run() {
				        	
				            runOnUiThread(new Runnable()
				            {
				                @Override
				                public void run()
				                {
				                					                	
				                    limitCounterSecondsForGpsLocation++;
				                    
				                    if (limitCounterSecondsForGpsLocation <= tempoToleranciaProcuraGps){
				                    	
					                    if (gpsTracker.isGPSEnabled){
					                    						                    						                    	
					                    	timerSearchForGpsLocation.cancel();
					                    	
					                    }
					                    
				                    }
				                    else {
				                    	
				                    	limitCounterSecondsForGpsLocation = 0;
				                    					                    				
				                    	gpsTracker.stopUsingGPS();
				                    	
				                    	timerSearchForGpsLocation.cancel();
				                    	
				                    	return;
				                    }
				                }
				            });
				        }
				    }, (long)1000, (long)1000);
						
				}
			} catch (Exception e) {

				Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
				
			}
			
		}
		
	}
	
	private boolean isNetworkAvailable() {
		
	    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	    
	}
	
	@Override
    public void onBackPressed() {
		
        if (exit) {
            	
			android.os.Process.killProcess(android.os.Process.myPid());
	        System.exit(1);
			
        } else {
  
        	if (gpsTracker != null){
        		
        		if (gpsTracker.isNetworkEnabled){
        			
					new AlertDialog.Builder(MainActivity.this)
					.setTitle("Atenção")
					.setMessage("Seu alerta ainda não foi enviado, deseja sair? ")
					.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {

							android.os.Process.killProcess(android.os.Process.myPid());
					        System.exit(1);
							
						}
					})
					.setNegativeButton("Não", null)
					.show();   			
        			
        		}
        		else {
        			
                	Toast toastConfirmacaoSaidaApp = Toast.makeText(this, "Pressione novamente para sair", Toast.LENGTH_SHORT);
    	        	
                	ViewGroup groupView = (ViewGroup) toastConfirmacaoSaidaApp.getView();
                	
                    TextView mensagemTextViewSaidaApp = (TextView) groupView.getChildAt(0);
                    mensagemTextViewSaidaApp.setTextSize(18);
                    
                    toastConfirmacaoSaidaApp.show();
                    
                    exit = true;
                    
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                        	
                            exit = false;
                            
                        }
                    }, 3 * 1000);
        			
        		}
        	}
        	else {
        		
            	Toast toastConfirmacaoSaidaApp = Toast.makeText(this, "Pressione novamente para sair", Toast.LENGTH_SHORT);
	        	
            	ViewGroup groupView = (ViewGroup) toastConfirmacaoSaidaApp.getView();
            	
                TextView mensagemTextViewSaidaApp = (TextView) groupView.getChildAt(0);
                mensagemTextViewSaidaApp.setTextSize(18);
                
                toastConfirmacaoSaidaApp.show();
                
                exit = true;
                
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    	
                        exit = false;
                        
                    }
                }, 3 * 1000);
        		
        	}

        }

    }
	
	@Override
	protected void onPause() {
		
		if (limitCounterSecondsForGpsLocation >= tempoToleranciaProcuraGps){
			
		    timerSearchForGpsLocation.cancel();			
			
		}
		
	    super.onPause();
	}
	
	@Override
	protected void onResume() {
				
	    super.onResume();
	}
		
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == 1) {

			if (resultCode == Activity.RESULT_OK) {

				Bundle bundle = data.getExtras();

				sharedPreferences = getSharedPreferences(userLoginAndPasswordPreferences,Context.MODE_PRIVATE);
				
				//usado quando usuario autentica pela primeira vez
				SharedPreferences.Editor editor = sharedPreferences.edit();
				
				editor.putString(usuario, bundle.getString(usuario));
				editor.putString(senha, bundle.getString(senha));
				editor.putString(nome, bundle.getString(nome));
				editor.putString(telefone, bundle.getString(telefone));
				editor.commit();

			}
			else if (resultCode == Activity.RESULT_CANCELED) {

				// fechar aplicação porque usuário cancelou autenticação
				finish();
				
			}
		}
	        
	}
	
	/*public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

	    switch(requestCode) {
	      case INITIAL_REQUEST:
	    	  
	        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
	        	
	        	
	        }

	    }
	  }*/
}
