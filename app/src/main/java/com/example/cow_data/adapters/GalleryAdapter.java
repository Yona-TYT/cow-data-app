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

import com.example.cow_data.CalcCalendar;
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

    @Override
    public View getView(int pos, View convertView, ViewGroup parent){
        LinearLayout layoutH = new LinearLayout(mContex);
        // Se ajustan los parametros del layout ---------------------------------------
        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        if(textList.get(pos)[6].equals("1")) {
            layoutH.setBackgroundColor(ContextCompat.getColor(layoutH.getContext(), R.color.highlight_background));
        }
        else{
            layoutH.setBackgroundColor(ContextCompat.getColor(layoutH.getContext(), R.color.text_background));

        }
        layoutH.setPadding(5,5,5,5);
        //-------------------------------------------------------------------------------

        ImageView mimgView = new ImageView(mContex);

        // Se ajustan los parametros de las imagenes-------------------------------
        String dir = textList.get(pos)[0];
        if(!dir.isEmpty() && !dir.equals("null")) {
             File file = new File(dir);
             boolean thereIs = file.exists();
             Uri mUri = null;
             if (thereIs){
                 mUri = Uri.fromFile(file);
             }
             else{
                 mUri = Uri.parse(dir);
             }
             mimgView.setImageURI(mUri);
         }
         else{
            mimgView.setImageResource(R.drawable.image_icon);
        }
        mimgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mimgView.setLayoutParams(new GridLayout.LayoutParams(spec(140), spec(150)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.gravity = Gravity.CENTER;
        mimgView.setLayoutParams(params);

        CardView cardView = new CardView(mContex);

        cardView.setLayoutParams(new GridLayout.LayoutParams(spec(140), spec(150)));
        params.gravity = Gravity.CENTER;
        cardView.setLayoutParams(params);

        cardView.addView(mimgView);
        cardView.setRadius(20f);

        layoutH.addView(cardView);
        //------------------------------------------------------------------------------

        // Se ajustan los parametros de los TextView--------------------------------------

        LinearLayout layoutV = new LinearLayout(mContex);
        // Se ajustan los parametros del layout ---------------------------------------
        layoutV.setOrientation(LinearLayout.VERTICAL);
        layoutV.setPadding(5,5,5,5);
        //-------------------------------------------------------------------------------

        // Texto Name
        TextView text1 = setTextView(textList.get(pos)[1]);
        layoutV.addView(text1);

        //Litros Text
        if(textList.get(pos)[5].equals("0")) {
            TextView text2 = setTextView("Litros: "+textList.get(pos)[2]+" (diarios)");
            layoutV.addView(text2);
        }

        //Date Text
        if(textList.get(pos)[6].equals("1")) {
            String txCount = CalcCalendar.dateDaysCount(textList.get(pos)[3]);
            TextView text3 = setTextView("Preñada (faltan "+txCount+" dias)");
            layoutV.addView(text3);
        }

        //BrithDate Text
        String txCount = textList.get(pos)[4];
        TextView text4 = setTextView("Edad: "+ CalcCalendar.getBrithDateText(txCount));
        layoutV.addView(text4);

        layoutH.addView(layoutV);
        //-------------------------------------------------------------------------------

        return layoutH;
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
