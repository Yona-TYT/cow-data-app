package com.example.cow_data.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.cow_data.R;

import java.util.ArrayList;
import java.util.List;

public class SelecAdapter extends BaseAdapter {
    //Test------------------------------------------------------------
    private Context mContex;
    private List<String> textList = new ArrayList<>();

    private ArrayList<Integer> newList = new ArrayList<>();    // Values to be displayed

    public  SelecAdapter(Context mContex, List<String> textList){
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
        return i;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent){

        Log.d("PhotoPicker", "Ya hay ? 11111------------------------: "+ textList.size());
        TextView text = new TextView(mContex);
        LinearLayout layout = new LinearLayout(mContex);

        // Se ajustan los parametros del Texto ----------------------------------
        text.setText(textList.get(pos));
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setGravity(Gravity.CENTER);
        text.setWidth(R.dimen.spinner_w1);
        text.setMaxLines(1);
        text.setTextColor(ContextCompat.getColor(text.getContext(), R.color.text_color1));
        text.setBackgroundColor(ContextCompat.getColor(text.getContext(), R.color.text_background2));
        text.setPadding(10,5,10,5);
        text.setTag(pos);
        layout.addView(text);

        //-----------------------------------------------------------------------

        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setVisibility(View.VISIBLE);

        return layout;
    }
}