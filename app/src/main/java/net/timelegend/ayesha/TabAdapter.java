package net.timelegend.ayesha;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class TabAdapter extends ArrayAdapter<MyWebView> {
    private Context context;
    private int resource_id;
    private List<MyWebView> dataSet;

    private static class ViewHolder {
        TextView bookTitle; 
        ImageView bookClose;
    }

    public TabAdapter(Context context, int resource_id, List<MyWebView> dataSet) {
        super(context, resource_id, dataSet);
        this.context = context;
        this.resource_id = resource_id;
        this.dataSet = dataSet;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            row = inflater.inflate(resource_id, null, false);
            viewHolder = new ViewHolder();
            viewHolder.bookTitle = (TextView) row.findViewById(R.id.booktitle);
            viewHolder.bookClose = (ImageView) row.findViewById(R.id.bookclose);
            row.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) row.getTag();
        }
        
        viewHolder.bookClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).onTabClose(position);
                notifyDataSetChanged();
            }
        });

        MyWebView data = dataSet.get(position);
        viewHolder.bookTitle.setText(data.toString());
        viewHolder.bookClose.setImageResource(R.drawable.ic_close);
        return row;
    }
}
