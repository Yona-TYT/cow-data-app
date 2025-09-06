package com.example.cow_data;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatEditText;

public class ClearFocusEditText extends AppCompatEditText {

    public ClearFocusEditText(Context context) {
        super(context);
    }

    public ClearFocusEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClearFocusEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        Basic.msg("HAy?");

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            clearFocus();  // Limpia el foco del EditText
        }
        return super.onKeyPreIme(keyCode, event);  // Deja que el IME maneje el evento para ocultar el teclado
    }


    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        Log.d("ClearFocusEditText", "Foco cambiado. Tiene foco: " + focused);
        if (!focused) {
            // Ejecuta lógica cuando el EditText pierde el foco
            Log.d("ClearFocusEditText", "EditText perdió el foco");
            // Puedes agregar aquí acciones adicionales, como guardar datos
            // Ejemplo: moreSaveCanc();
        }
        else {
        }
    }
}