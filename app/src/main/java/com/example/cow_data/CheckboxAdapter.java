package com.example.cow_data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CheckboxAdapter extends BaseAdapter implements Filterable, View.OnClickListener{
    //Test------------------------------------------------------------
    private Context mContex;

    private List<String[]> textList = new ArrayList<>();
    private List<String[]>  currList = new ArrayList<>();// Original Values
    private List<CheckBox>  checkList = new ArrayList<>();// Original Values

    String mapID = "";
    private ArrayList<String> myArray;

    private boolean oneValue = false;

    private ArrayList<Integer> newList = new ArrayList<>();    // Values to be displayed

    public  CheckboxAdapter(Context mContex, List<String[]> textList, boolean oneValue, String mapID){
        this.mContex = mContex;
        this.textList = textList;
        this.currList = textList;
        this.oneValue = oneValue;
        this.mapID = mapID;

        myArray = StartVar.arrayMap.get(mapID);
    }

    @Override
    public int getCount(){
        return newList.size();
    }

    @Override
    public Object getItem(int pos){
        return newList;
    }

    @Override
    public long getItemId(int i) {
        return newList.get(i);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int pos, View convertView, ViewGroup parent){
        Log.d("PhotoPicker", "Ya hay ? 11111------------------------: "+ textList.size());
        CheckBox check = new CheckBox(mContex);
        //check.setChecked((Boolean)textList.get(pos)[1]);
        LinearLayout layout = new LinearLayout(mContex);

        int mIdx = newList.get(pos);
        check.setId(R.id.check_boxlist);
        check.setTag(textList.get(mIdx)[0]);
        if(!oneValue) {
            if (isBoxValue(textList.get(mIdx)[0])) {
                check.setChecked(true);
            }
        }

        // Se ajustan los parametros del Texto ----------------------------------
        check.setText(textList.get(mIdx)[1]);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        check.setGravity(Gravity.CENTER);
        check.setWidth(R.dimen.spinner_w1);
        check.setMaxLines(1);
        //check.setTextColor(ContextCompat.getColor(check.getContext(), R.color.text_color1));
        check.setBackgroundColor(ContextCompat.getColor(check.getContext(), R.color.text_background2));
        check.setPadding(10,5,10,5);
        check.setOnClickListener(this);
        checkList.add(check);
        layout.addView(check);

        //-----------------------------------------------------------------------

        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setVisibility(View.VISIBLE);

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
                        String data = currList.get(i)[1];
                        if (data.toLowerCase().contains(constraint.toString())){
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

    @Override
    public void onClick(View v) {
        int itemId = v.getId();
        if(itemId == R.id.check_boxlist){
            CheckBox check =  (CheckBox)v;
            boolean isCheck = check.isChecked();
            String mId = v.getTag().toString();

            if(myArray == null){
                Basic.msg("Error in Array Map!.");
                return;
            }
            if (isCheck) {
                if (myArray.isEmpty()) {
                    myArray.add(mId);
                    StartVar.arrayMap.put(mapID, myArray);
                }
                else {
                    if(oneValue) {
                        for (CheckBox mCheck : checkList){
                            if(mCheck != null && mCheck.getTag() != v.getTag()){
                                mCheck.setChecked(false);
                            }
                        }
                        Objects.requireNonNull(StartVar.arrayMap.get(mapID)).clear();
                        Objects.requireNonNull(StartVar.arrayMap.get(mapID)).add(mId);
                    }
                    else {
                        if (!isBoxValue(mId)) {
                            Objects.requireNonNull(StartVar.arrayMap.get(mapID)).add(mId);
                        }
                    }
                }
            }
            else{
                int i = 0;
                for (String s : myArray) {
                    if (s.equals(mId)) {
                        Objects.requireNonNull(StartVar.arrayMap.get(mapID)).remove(i);
                        break;
                    }
                    i++;
                }
            }
        }

    }

    private boolean isBoxValue(String mId){
        for (String s : myArray) {
            if (s.equals(mId)) {
                return true;
            }
        }
        return false;
    }
}