package com.example.cow_data.adapters;

import static android.widget.GridLayout.spec;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.cow_data.utls.CalendUtls;
import com.example.cow_data.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends BaseAdapter {
    //Test------------------------------------------------------------
    private Context mContex;
    private List<String[]>textList = new ArrayList<>();

    public  GalleryAdapter(Context mContex, List<String[]> textList){
        this.mContex = mContex;
        this.textList = textList;
    }

    @Override
    public int getCount(){
        return textList.size();
    }

    @Override
    public Object getItem(int pos){
        return textList.get(pos);
    }

    @Override
    public long getItemId(int i) {
        return Long.parseLong(textList.get(i)[7]);
    }

    // 1. Estructura que mantendrá las referencias en memoria para el reciclaje
    public static class ViewHolder {
        LinearLayout layoutH;
        ImageView mimgView;
        CardView cardView;
        LinearLayout layoutV;
        TextView text1;
        TextView text2; // Litros
        TextView text3; // Preñada
        TextView text4; // Edad
        public String userId;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        if (pos < 0) {
            return convertView != null ? convertView : new LinearLayout(mContex);
        }

        ViewHolder holder;

        // 2. CONDICIONAL DE REUTILIZACIÓN: Si es null, creamos la estructura por ÚNICA vez
        if (convertView == null) {
            holder = new ViewHolder();

            // Inicializar contenedor horizontal
            holder.layoutH = new LinearLayout(mContex);
            holder.layoutH.setOrientation(LinearLayout.HORIZONTAL);
            holder.layoutH.setPadding(5, 5, 5, 5);

            // Inicializar ImageView y CardView
            holder.mimgView = new ImageView(mContex);
            holder.mimgView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(200, 200);
            imgParams.gravity = Gravity.CENTER;
            holder.mimgView.setLayoutParams(imgParams);

            holder.cardView = new CardView(mContex);
            holder.cardView.setLayoutParams(imgParams);
            holder.cardView.setRadius(20f);
            holder.cardView.addView(holder.mimgView);
            holder.layoutH.addView(holder.cardView);

            // Inicializar contenedor vertical para los textos
            holder.layoutV = new LinearLayout(mContex);
            holder.layoutV.setOrientation(LinearLayout.VERTICAL);
            holder.layoutV.setPadding(5, 5, 5, 5);

            // Creamos los 4 TextViews base una sola vez
            holder.text1 = setTextView("");
            holder.text2 = setTextView("");
            holder.text3 = setTextView("");
            holder.text4 = setTextView("");

            holder.layoutV.addView(holder.text1);
            holder.layoutV.addView(holder.text2);
            holder.layoutV.addView(holder.text3);
            holder.layoutV.addView(holder.text4);

            holder.layoutH.addView(holder.layoutV);

            // Guardamos el contenedor principal en convertView y le asociamos el holder
            convertView = holder.layoutH;
            convertView.setTag(holder);
        } else {
            // ¡Aquí ocurre la magia del rendimiento! Reutilizamos los objetos existentes instantáneamente
            holder = (ViewHolder) convertView.getTag();
        }

        // 3. ASIGNACIÓN DE DATOS (Se ejecuta en microsegundos al hacer scroll)
        String[] currentItem = textList.get(pos);

        // Ajustar color de fondo de la celda
        if (currentItem[6].equals("1")) {
            holder.layoutH.setBackgroundColor(ContextCompat.getColor(mContex, R.color.highlight_background));
        } else {
            holder.layoutH.setBackgroundColor(ContextCompat.getColor(mContex, R.color.text_background));
        }

        // Procesar la Imagen de manera eficiente
        String dir = currentItem[0];
        if (!dir.isEmpty() && !dir.equals("null")) {
            File file = new File(dir);
            Uri mUri = file.exists() ? Uri.fromFile(file) : Uri.parse(dir);
            holder.mimgView.setImageURI(mUri);
        } else {
            holder.mimgView.setImageResource(R.drawable.image_icon);
        }

        // Texto 1: Nombre
        holder.text1.setText(currentItem[1]);

        // Texto 2: Litros (Control de visibilidad estricto para evitar duplicados)
        if (currentItem[5].equals("0")) {
            holder.text2.setText("Litros: " + currentItem[2] + " (diarios)");
            holder.text2.setVisibility(View.VISIBLE);
        } else {
            holder.text2.setVisibility(View.GONE); // Si se recicla una celda vieja, esto la limpia por completo
        }

        // Texto 3: Preñada (Control de visibilidad)
        if (currentItem[6].equals("1")) {
            String txCount = CalendUtls.dateDaysCount(currentItem[3]);
            holder.text3.setText("Preñada (faltan " + txCount + " dias)");
            holder.text3.setVisibility(View.VISIBLE);
        } else {
            holder.text3.setVisibility(View.GONE); // Limpia el residuo visual del reciclaje
        }

        // Texto 4: Edad
        String txCountAge = currentItem[4];
        holder.text4.setText("Edad: " + CalendUtls.getBrithDateText(txCountAge));

        // =========================================================================
        // 4. SOLUCIÓN AL CRASH ANTERIOR (Guardado seguro del ID de usuario)
        // =========================================================================
        // Usamos un ID interno del sistema (android.R.id.text1) para almacenar el String.
        // Esto evita pisar o borrar el ViewHolder que guardamos en la línea 48.

        // Guardamos el ID dentro del ViewHolder reciclable
        holder.userId = currentItem[8];

        return convertView;
    }

    private TextView setTextView(String mText){
        TextView mView = new TextView(mContex);

        // Se ajustan los parametros del Texto ----------------------------------
        mView.setText(mText);
        mView.setTypeface(Typeface.DEFAULT_BOLD);
        mView.setGravity(Gravity.START);
        mView.setTextSize(18);
        mView.setMaxLines(1);
        mView.setPadding(8,2,8,2);
        //-----------------------------------------------------------------------

        return mView;
    }
}
