package com.example.cow_data;

import static android.service.controls.ControlsProviderService.TAG;
import static android.widget.GridLayout.CENTER;
import static android.widget.GridLayout.spec;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

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
        return Long.parseLong(textList.get(i)[2]);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent){
        ImageView mimgView = new ImageView(mContex);
        TextView text = new TextView(mContex);
        LinearLayout layout = new LinearLayout(mContex);
        // Se ajustan los parametros del Texto ----------------------------------
        text.setText(textList.get(pos)[0]);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setGravity(Gravity.CENTER);
        text.setTextSize(12);
        text.setMaxLines(1);
        text.setPadding(2,2,2,2);
        text.setBackgroundColor(ContextCompat.getColor(text.getContext(), R.color.text_background));
        //-----------------------------------------------------------------------

        // Se ajustan los parametros de las imagenes-------------------------------
        String dir = textList.get(pos)[1];
        if(!dir.isEmpty()) {
             File file = new File(dir);
             boolean threis = file.exists();
             Uri mUri = null;
             if (threis){
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

        //------------------------------------------------------------------------------

        // Se ajustan los parametros del layout ---------------------------------------
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(ContextCompat.getColor(text.getContext(), R.color.text_background));
        layout.setPadding(5,5,5,5);
        layout.addView(text);
        layout.addView(cardView);
        //-------------------------------------------------------------------------------

        return layout;
    }
}
