package com.example.cow_data.ex;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;
import com.google.common.base.Strings;  // O usa TextUtils.isEmpty
import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class Dialogs {
    private static ProgressDialog simpleProgress;  // Para progress genérico

    // Formatea error para HTML (como en GPSLogger)
    protected static String getFormattedErrorMessageForDisplay(String message, Throwable throwable) {
        StringBuilder html = new StringBuilder();
        if (!Strings.isNullOrEmpty(message)) {
            html.append("<b>").append(message.replace("\n", "<br />")).append("</b><br /><br />");
        }
        while (throwable != null && !Strings.isNullOrEmpty(throwable.getMessage())) {
            html.append(throwable.getMessage().replace("\n", "<br />")).append("<br /><br />");
            throwable = throwable.getCause();
        }
        return html.toString();
    }

    // Muestra error (usa AlertDialog nativo por simplicidad)
    public static void showError(Context context, String title, String friendlyMessage, String errorMessage, Throwable throwable) {
        String fullMessage = getFormattedErrorMessageForDisplay(friendlyMessage + ": " + errorMessage, throwable);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(fullMessage)  // Si usas WebView para HTML, intégralo aquí
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

        // Opcional: Log
        //LOG.error("Error mostrado: " + fullMessage);
    }

    // Muestra error (usa AlertDialog nativo por simplicidad)
    public static void showError(Context context, String title, String friendlyMessage, String errorMessage, Throwable throwable, DialogInterface.OnClickListener positiveListener) {
        String fullMessage = getFormattedErrorMessageForDisplay(friendlyMessage + ": " + errorMessage, throwable);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(fullMessage)  // Si usas WebView para HTML real, intégralo aquí
                .setIcon(android.R.drawable.ic_dialog_alert);

        // Botón [Aceptar] con listener opcional
        if (positiveListener != null) {
            builder.setPositiveButton(android.R.string.ok, positiveListener);
        } else {
            builder.setPositiveButton(android.R.string.ok, null);
        }

        builder.show();

        // Opcional: Log
        // LOG.error("Error mostrado: " + fullMessage);
    }

    // Alerta simple de éxito/error (sobrecarga para compatibilidad)
    public static void alert(Context context, String title, String message) {
        alert(context, title, message, null);  // Llama a la versión con listener (null por defecto)
    }

    // Versión con listener para acción en [Aceptar]
    public static void alert(Context context, String title, String message, DialogInterface.OnClickListener positiveListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info);

        // Botón [Aceptar] con listener opcional
        if (positiveListener != null) {
            builder.setPositiveButton(android.R.string.ok, positiveListener);
        } else {
            builder.setPositiveButton(android.R.string.ok, null);
        }

        builder.show();

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();  // Complemento visual
    }

    // Progress dialog
    public static void progress(Context context, String title) {
        // 1. VALIDACIÓN PREVIA: Si el contexto es una actividad que está muriendo, no hacemos nada
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        try {
            // Aseguramos cerrar cualquier residuo previo antes de abrir uno nuevo
            hideProgress();

            simpleProgress = new ProgressDialog(context);
            simpleProgress.setTitle(title);
            simpleProgress.setMessage("Por favor espere...");
            simpleProgress.setCancelable(false);
            simpleProgress.show();
        } catch (Exception e) {
            android.util.Log.e("Dialogs", "Error al mostrar el progreso", e);
        }
    }
//    public static void progress(Context context, String title) {
//        simpleProgress = new ProgressDialog(context);
//        simpleProgress.setTitle(title);
//        simpleProgress.setMessage("Por favor espere...");
//        simpleProgress.setCancelable(false);
//        simpleProgress.show();
//    }

//    public static void hideProgress() {
//        if (simpleProgress != null && simpleProgress.isShowing()) {
//            simpleProgress.dismiss();
//        }
//    }

    public static void hideProgress() {
        try {
            if (simpleProgress != null && simpleProgress.isShowing()) {
                // 2. CORRECCIÓN DEL CRASH: Comprobamos el contexto de la ventana del diálogo
                Context context = simpleProgress.getContext();
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    // Si la pantalla ya murió, no podemos llamar a dismiss() o crasheará
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        simpleProgress = null; // Limpiamos la referencia estática y salimos
                        return;
                    }
                }

                // Si todo está en orden, cerramos de forma segura
                simpleProgress.dismiss();
            }
        } catch (IllegalArgumentException e) {
            // Captura el error específico del log: "not attached to window manager"
            android.util.Log.w("Dialogs", "El diálogo ya no estaba acoplado a la ventana.");
        } catch (Exception e) {
            android.util.Log.e("Dialogs", "Error al ocultar el progreso", e);
        } finally {
            simpleProgress = null; // Garantizamos liberar la memoria siempre
        }
    }

}
