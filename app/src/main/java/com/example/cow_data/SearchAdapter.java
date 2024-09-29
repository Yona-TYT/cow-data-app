package com.example.cow_data;

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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class SearchAdapter extends BaseAdapter implements Filterable {
    //Test------------------------------------------------------------
    private Context mContex;

    public ArrayList<String> textList = new ArrayList<>();
    private ArrayList<String> currList = new ArrayList<>(); // Original Values
    private ArrayList<Integer> newList = new ArrayList<>();    // Values to be displayed

    public  SearchAdapter(Context mContex, ArrayList<String> textList){
        this.mContex = mContex;
        this.textList = textList;
        this.currList = textList;
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
        return 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent){

       // Log.d("PhotoPicker", "Ya hay ? 11111------------------------: "+ newList.size());
        TextView text = new TextView(mContex);
        LinearLayout layout = new LinearLayout(mContex);
        // Se ajustan los parametros del Texto ----------------------------------
        layout.setVisibility(View.INVISIBLE);
        for(int i = 0; i < newList.size(); i++) {
            if(pos == newList.get(i)){
                //Log.d("PhotoPicker", "Name test ------------------------: " + pos + " -- "+ textList.get(pos));
                text.setText(textList.get(pos));
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setGravity(Gravity.CENTER);
                text.setTextSize(18);
                text.setPadding(10,5,10,5);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.addView(text);
                layout.setVisibility(View.VISIBLE);
                return layout;
            }
        }

        //-----------------------------------------------------------------------

//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
///       params.topMargin = 0;
////        params.bottomMargin = 0;
//        text.setLayoutParams(params);
//        layout.removeAllViews();
        return layout;
    }
    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                ArrayList<Integer> FilteredArrList = new ArrayList<Integer>();
                /********
                 *
                 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 *  else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 ********/
                //Log.d("PhotoPicker", "Constrain ------------------------: " + constraint);
                if (constraint == null || constraint.length() == 0) {
                    // set the Original result to return
                    results.count = newList.size();
                    results.values = newList;
                }
                else {
                    constraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < currList.size(); i++) {
                        String data = currList.get(i);
                        if (data.toLowerCase().startsWith(constraint.toString())) {
                            FilteredArrList.add(i);
                            //Log.d("PhotoPicker", "Constrain ------------------------: " + i);
                        }
                    }
                    // set the Filtered result to return
                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                }
               // Log.d("PhotoPicker", "11111------------------------: " + FilteredArrList.size());
                return results;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //Log.d("PhotoPicker", "2222------------------------: " +constraint);
                newList = (ArrayList<Integer>) results.values;   // has the filtered values
                notifyDataSetChanged();                         // notifies the data with new filtered values
            }
        };
        return filter;
    }
}