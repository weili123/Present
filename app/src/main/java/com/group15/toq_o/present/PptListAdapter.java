package com.group15.toq_o.present;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.group15.toq_o.present.Presentation.Presentation;

import java.util.ArrayList;

/**
 * Created by weili on 12/6/14.
 */
public class PptListAdapter extends BaseAdapter implements OnClickListener {
    private Activity activity;
    private ArrayList data;
    private static LayoutInflater inflater;
    public Resources res;
    Presentation listItem;

    public PptListAdapter(Activity a, ArrayList d, Resources resLocal) {
        activity = a;
        data=d;
        res = resLocal;
        listItem = null;
        inflater = ( LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public int getCount() {

        if(data.size()<=0)
            return 1;
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public static class ViewHolder{
        public TextView text;
        //public ImageView image;
        public ImageView radioButton;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        ViewHolder holder;
        if(convertView == null) {
            vi = inflater.inflate(R.layout.pptitem, null);
            holder = new ViewHolder();
            holder.text = (TextView) vi.findViewById(R.id.name);
            holder.radioButton = (ImageView) vi.findViewById(R.id.radio_button);
            vi.setTag(holder);
        }
        else {
            holder = (ViewHolder) vi.getTag();
        }
        if(data.size()<=0) {
            holder.text.setText("No Data");
        } else {
            listItem = null;
            listItem = (Presentation) data.get(position);
            String name = listItem.getName();
            holder.text.setText(name);
            holder.radioButton.setImageResource(R.drawable.empty_radio);
            vi.setOnClickListener(new OnItemClickListener(position));
        }
        return vi;
    }

    @Override
    public void onClick(View v) {
        Log.v("CustomAdapter", "=====Row button clicked=====");
    }

    private class OnItemClickListener  implements OnClickListener{
        private int mPosition;

        OnItemClickListener(int position){
            mPosition = position;
        }

        @Override
        public void onClick(View arg0) {

            ViewFilesActivity sct = (ViewFilesActivity)activity;
            sct.onItemClick(mPosition, arg0);
        }
    }
}
