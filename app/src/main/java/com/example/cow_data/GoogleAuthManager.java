package com.example.cow_data;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

public class GoogleAuthManager {
    private static final String TAG = "GoogleAuthManager";
    private final Context context;
    private final String serverClientId;
    private final AuthCallback callback;
    private final GoogleSignInClient signInClient;
    private final ActivityResultLauncher<Intent> signInLauncher;

    public interface AuthCallback {
        void onSuccess(String serverAuthCode);
        void onError(String errorMessage);
    }

    public GoogleAuthManager(AppCompatActivity activity, String serverClientId, AuthCallback callback) {
        this.context = activity;
        this.serverClientId = serverClientId;
        this.callback = callback;

        // Configurar Google Sign-In con alcance para Drive
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(serverClientId)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        signInClient = GoogleSignIn.getClient(activity, gso);

        // Registrar el launcher
        signInLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Resultado recibido: Código=" + result.getResultCode() + ", Intent=" + result.getData());
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data == null) {
                            String errorMessage = "Resultado del Intent es nulo";
                            Log.e(TAG, errorMessage);
                            copyToClipboard(errorMessage);
                            callback.onError(errorMessage);
                            return;
                        }
                        try {
                            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                                    .getResult(ApiException.class);
                            String serverAuthCode = account.getServerAuthCode();
                            if (serverAuthCode != null) {
                                Log.d(TAG, "Server Auth Code obtenido: [REDACTED]");
                                Log.d(TAG, "Alcances otorgados: " + account.getGrantedScopes());
                                Basic.msg("Codigo es valido!");
                                callback.onSuccess(serverAuthCode);
                            } else {
                                String errorMessage = "No se obtuvo el Server Auth Code";
                                Log.e(TAG, errorMessage);
                                copyToClipboard(errorMessage);
                                callback.onError(errorMessage);
                            }
                        } catch (ApiException e) {
                            String errorMessage = "Error al obtener credenciales: " + e.getMessage() + " (Código: " + e.getStatusCode() + ")";
                            Log.e(TAG, errorMessage, e);
                            copyToClipboard(errorMessage);
                            callback.onError(errorMessage);
                        }
                    } else {
                        String errorMessage = "Inicio de sesión cancelado por el usuario (Código de resultado: " + result.getResultCode() + ")";
                        Log.e(TAG, errorMessage);
                        copyToClipboard(errorMessage);
                        callback.onError(errorMessage);
                    }
                }
        );
    }

    public void startAuthentication() {
        try {
            Log.d(TAG, "Iniciando autenticación con Google Sign-In, serverClientId: " + serverClientId);
            Intent signInIntent = signInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        } catch (Exception e) {
            String errorMessage = "Error al iniciar autenticación: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            copyToClipboard(errorMessage);
            callback.onError(errorMessage);
        }
    }

    private void copyToClipboard(String errorMessage) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error de autenticación", errorMessage);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Error copiado al portapapeles: " + errorMessage, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al copiar al portapapeles: " + e.getMessage(), e);
        }
    }
}