package com.acgnu.tiktik;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MergeListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<DataHolder> items = new ArrayList<>();
    private final View.OnClickListener deleteListener;
    private String selected;
    public MergeListAdapter(Context context, List<DataHolder> data, View.OnClickListener listener) {
        inflater = LayoutInflater.from(context);
        items.addAll(data);
        deleteListener = listener;
    }
    public static class DataHolder {
        public String title;
        public int bpm;
        public LinearLayout rootView;
    }
    private static class Holder {
        TextView title, bpm, delete;
        View content;
    }
    @Override public int getCount() { return items.size(); }
    @Override public DataHolder getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }
    @Override public View getView(int position, View view, ViewGroup parent) {
        Holder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.scroll_del_listview_item, parent, false);
            holder = new Holder();
            holder.title = view.findViewById(R.id.title);
            holder.bpm = view.findViewById(R.id.preset_bpm);
            holder.delete = view.findViewById(R.id.del);
            holder.content = view.findViewById(R.id.preset_content);
            view.setTag(holder);
        } else holder = (Holder) view.getTag();
        DataHolder item = items.get(position);
        holder.title.setText(item.title);
        holder.bpm.setText(item.bpm + " BPM");
        holder.content.setBackgroundColor(Color.parseColor(item.title.equals(selected) ? "#DFEDE2" : "#F6F5F1"));
        holder.content.setContentDescription(item.title + "，" + item.bpm + " BPM" + (item.title.equals(selected) ? "，已选中" : ""));
        item.rootView = (LinearLayout) view;
        item.rootView.scrollTo(0, 0);
        holder.delete.setTag(item.title);
        holder.delete.setOnClickListener(deleteListener);
        return view;
    }
    public DataHolder find(String name) {
        for (DataHolder item : items) if (item.title.equals(name)) return item;
        return null;
    }
    public void setSelected(String name) { selected = name; notifyDataSetChanged(); }
    public void remove(String name) {
        DataHolder item = find(name);
        if (item != null) items.remove(item);
        notifyDataSetChanged();
    }
    public void addItem(String name, int bpm) {
        DataHolder item = new DataHolder();
        item.title = name; item.bpm = bpm;
        items.add(item);
        notifyDataSetChanged();
    }
}
