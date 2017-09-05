package com.ufes.alertaufes;

import android.content.Context;
import android.widget.Toast;
import br.com.jansenfelipe.androidmask.MaskEditTextChangedListener;
import util.ConexaoServidor;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

	private String user = "usuario";
	private String password = "senha";
	private String name = "nome";
	private Boolean exit = false;
	private ProgressDialog progressDialogAutentication;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login);
		
		progressDialogAutentication = new ProgressDialog(LoginActivity.this);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			
		final EditText editTextPhone = (EditText) findViewById(R.id.inputCelular);
		final EditText editTextUsername = (EditText)findViewById(R.id.inputUsuario);
		final EditText editTextPassword = (EditText)findViewById(R.id.inputSenha);

		String phoneNumber = getNumber(); // pega o número do celular
		
		MaskEditTextChangedListener maskPhoneNumber = new MaskEditTextChangedListener("(##)#####-####",editTextPhone);
		
		// ativa a máscara de telefone celular
		editTextPhone.addTextChangedListener(maskPhoneNumber);

		// verifica se o número do telefone foi capturado
		if (phoneNumber != null && !phoneNumber.isEmpty() && !phoneNumber.startsWith("?")){
			
			// se o número capturado for sem o 9
			if (phoneNumber.length() == 10){
				
				String codigoArea = phoneNumber.substring(0, 2); // retorna o DDD do número
				
				phoneNumber = phoneNumber.substring(2); // retira o DDD do número
				phoneNumber = "9" + phoneNumber; // adiciona o 9 ao número
				phoneNumber = codigoArea + phoneNumber; // adiciona o DDD novamente ao número
								
			}
			
			editTextPhone.setText(phoneNumber);			
			
		}

		Button buttonLogin = (Button) findViewById(R.id.buttonAutenticar);
		
		buttonLogin.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				if (editTextUsername.getText().toString().isEmpty() || editTextPassword.getText().toString().isEmpty() || editTextPhone.getText().toString().isEmpty()){
					
					new AlertDialog.Builder(LoginActivity.this)
					.setTitle("Atenção")
					.setMessage("Campos inválidos")
					.setNeutralButton("Fechar", null)
					.show();
					
					return;
					
				}
				else if (editTextPhone.getText().toString().length() < 13){
					
					new AlertDialog.Builder(LoginActivity.this)
					.setTitle("Atenção")
					.setMessage("Número de celular inválido")
					.setNeutralButton("Fechar", null)
					.show();
					
					return;
					
				}
				
				String usuario = editTextUsername.getText().toString();
				String senha = editTextPassword.getText().toString();
				String phone = editTextPhone.getText().toString().replaceAll("[(),-]", ""); // número do telefone sem caracteres especiais
								
				try {
					
					if (!isNetworkAvailable()){
						
						// wifi/3G/4G não está ativado

						new AlertDialog.Builder(LoginActivity.this)
						.setTitle("Atenção")
						.setMessage("Dispositivo não está conectado à internet ")
						.setNeutralButton("Fechar", null)
						.show();
					
						return;
					}
					
					String testeResposta = new ConexaoServidor(LoginActivity.this).execute("http://google.com","testarConexao").get();
					
					if (!testeResposta.equals("Ok")) {
						
						new AlertDialog.Builder(LoginActivity.this)
						.setTitle("Atenção")
						.setMessage("Dispositivo não está conectado à internet ")
						.setNeutralButton("Fechar", null)
						.show();
						
						return;
						
					}
					
		        	progressDialogAutentication = ProgressDialog.show(LoginActivity.this, "Autenticando", " Aguarde",true);
		        	
					String resposta = new ConexaoServidor(LoginActivity.this).execute("https://alerta.ufes.br/web/index.php?r=usuario/autenticar&"
							,"usr=" + usuario + "&" + "psw=" + senha + "&" + "cel=" + phone, "enviarAutenticacao").get();
					
					if (resposta.equals("false")){

						progressDialogAutentication.dismiss();
						
						new AlertDialog.Builder(LoginActivity.this)
						.setTitle("Dados incorretos")
						.setMessage("Preencha novamente")
						.setNeutralButton("Fechar", null)
						.show();
						
						return;					
						
					}
					else if (resposta.equals("Falha Conexão") || resposta.startsWith("Erro: ")){
						
						progressDialogAutentication.dismiss();
						
						new AlertDialog.Builder(LoginActivity.this)
						.setTitle("Erro")
						.setMessage( resposta + ", tente novamente")
						.setNeutralButton("Fechar", null)
						.show();
						
						return;
												
					}
					else {
						
						progressDialogAutentication.dismiss();							
						
						Intent returnIntent = new Intent();
						
						Bundle returnBundle = new Bundle();
						
						returnBundle.putString(password, senha);
						returnBundle.putString(user, usuario);
						returnBundle.putString(name, resposta);
						
						returnIntent.putExtras(returnBundle);
						
						setResult(Activity.RESULT_OK,returnIntent);
						
						finish();
							
					}
					
				} catch (Exception e) {
				
					progressDialogAutentication.dismiss();
					
					new AlertDialog.Builder(LoginActivity.this)
					.setTitle("Erro")
					.setMessage("Erro: " + e.getMessage() + ", tente novamente")
					.setNeutralButton("Fechar", null)
					.show();
					
					return;
					
				}
				
			}
		});	
	}
	
	@Override
    public void onBackPressed() {
		
        if (exit) {
        	
			Intent returnIntent = new Intent();
			
			setResult(Activity.RESULT_CANCELED,returnIntent);
			
			finish();
			
        } 
         else if (progressDialogAutentication.isShowing()){
			
			new AlertDialog.Builder(LoginActivity.this)
			.setTitle("Atenção")
			.setMessage("Você ainda não foi autenticado, deseja sair? ")
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
	
    public String getNumber(){
    	
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE); 
        String number = telephonyManager.getLine1Number();
        return number.substring(3); // retorna numero de telefone sem +55
    	
    }
	
	private boolean isNetworkAvailable() {
		
	    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	    
	}
}