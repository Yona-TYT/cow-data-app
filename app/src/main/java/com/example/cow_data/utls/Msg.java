package com.example.cow_data.utls;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.cow_data.R;

import java.util.LinkedList;
import java.util.Queue;

public class Msg {
    private static final String TAG = "Msg";

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Queue<MsgItem> messageQueue = new LinkedList<>();
    private static boolean isShowing = false;
    private static Context mContext;
    private static String lastMessageShown = "";

    // Clase interna para los items de la cola
    private static class MsgItem {
        String msg;
        boolean isClipboard;
        long timestamp;

        MsgItem(String msg, boolean isClipboard) {
            this.msg = msg;
            this.isClipboard = isClipboard;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Inicializar contexto (llamar esto una vez, por ejemplo en Application o MainActivity)
    public static void init(Context context) {
        mContext = context.getApplicationContext(); // Mejor usar ApplicationContext
    }

    public static void m(String msg) {
        msgInternal(msg, false, 0);
    }

    public static void m(String msg, int length) {
        msgInternal(msg, false, length);
    }

    public static void m(String msg, boolean isClipboard) {
        msgInternal(msg, isClipboard, 0);
    }

    //Test mark
    public static void m() {
        msgInternal("Aqui Hay Aqui Hay !!", true, 1);
    }
    public static void d(String tag, String msg) {
        msgInternal(tag+": "+msg, true, 1);
    }


    // ====================== MÉTODO PRINCIPAL ======================
    public static void msgInternal(String msg, boolean isClipboard, int length) {
        String claseExterna = "Desconocido";
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[3];

            // Obtenemos el nombre simple de la clase (ej: "SetDb" en lugar de "com.example...SetDb")
            claseExterna = caller.getClassName();
            if (claseExterna.contains(".")) {
                claseExterna = claseExterna.substring(claseExterna.lastIndexOf(".") + 1);
            }
        }
        Log.e(TAG, claseExterna+" Message: "+ msg);

        if (msg == null || msg.trim().isEmpty() || mContext == null) return;

        mainHandler.post(() -> {
            // BLOQUEO: Si es igual al que se está mostrando justo ahora, ignorar.
            if (isShowing && msg.equals(lastMessageShown)) return;

            // Limpiar la cola de duplicados y mensajes viejos
            long now = System.currentTimeMillis();
            messageQueue.removeIf(item -> (now - item.timestamp > 5000) || item.msg.equals(msg));

            messageQueue.offer(new MsgItem(msg, isClipboard));

            if (!isShowing) {
                showNextToast(length);
            }
        });
    }

    // ====================== MOSTRAR SIGUIENTE ======================
    private static void showNextToast(int length) {
        if (messageQueue.isEmpty() || isShowing || mContext == null) return;

        MsgItem item = messageQueue.poll();
        if (item == null) return;

        isShowing = true;

        lastMessageShown = item.msg;

        TextView text = new TextView(mContext);
        text.setText(item.msg);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setGravity(Gravity.CENTER);
        text.setMaxLines(2);
        text.setTextColor(ContextCompat.getColor(mContext, R.color.text_color1));
        text.setBackgroundColor(ContextCompat.getColor(mContext, R.color.text_background2));
        text.setPadding(16, 12, 16, 12);

        CardView cardView = new CardView(mContext);
        cardView.setRadius(12f);
        cardView.setCardElevation(8f);
        cardView.addView(text);

        Toast toast = new Toast(mContext);
        toast.setView(cardView);
        toast.setDuration(length);
        toast.show();

        // Copiar al portapapeles
        if (item.isClipboard) {
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Texto Extraído", item.msg);
                clipboard.setPrimaryClip(clip);
            }
        }

        // Programar siguiente toast
        mainHandler.postDelayed(() -> {
            isShowing = false;
            // Opcional: lastMessageShown = "";
            showNextToast(length);
        }, 2000);
    }
}