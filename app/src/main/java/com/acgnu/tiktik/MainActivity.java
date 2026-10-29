package com.acgnu.tiktik;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.Activity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private MergeListAdapter adapter;
    private TempoDialView dial;
    private View panel;
    private ImageButton save;
    private int bpm = Tempo.DEFAULT;
    private boolean playing;
    private String selected;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        panel = findViewById(R.id.tempo_panel);
        dial = findViewById(R.id.tempo_dial);
        save = findViewById(R.id.save_preset);
        ScrollDelListView list = findViewById(R.id.listview);
        JSONObject data = DataUtil.instance().readJson(this);
        List<String> names = new ArrayList<>();
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) names.add(keys.next());
        Collections.sort(names);
        List<MergeListAdapter.DataHolder> items = new ArrayList<>();
        for (String name : names) {
            MergeListAdapter.DataHolder item = new MergeListAdapter.DataHolder();
            item.title = name;
            item.bpm = Tempo.clamp(data.optInt(name, Tempo.DEFAULT));
            items.add(item);
        }
        adapter = new MergeListAdapter(this, items, new View.OnClickListener() {
            @Override public void onClick(View view) { deletePreset((String) view.getTag()); }
        });
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.empty_list));
        list.setOnItemClickListener((parent, view, position, id) -> {
            MergeListAdapter.DataHolder item = adapter.getItem(position);
            selected = item.title;
            bpm = item.bpm;
            dial.setBpm(bpm);
            adapter.setSelected(selected);
            playing = true;
            updatePlayback();
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            showNameDialog(adapter.getItem(position).title);
            return true;
        });
        if (state != null) {
            bpm = Tempo.clamp(state.getInt("bpm", Tempo.DEFAULT));
            selected = state.getString("selected");
            if (adapter.find(selected) == null) selected = null;
        }
        playing = DickPlayService.isPlaying;
        if (playing) bpm = DickPlayService.currentBpm;
        adapter.setSelected(selected);
        dial.setBpm(bpm);
        dial.setListener(value -> {
            bpm = value;
            if (playing) startPlayback();
            renderState();
        });
        dial.setOnClickListener(view -> {
            playing = !playing;
            updatePlayback();
        });
        findViewById(R.id.add_preset).setOnClickListener(view -> showNameDialog(null));
        findViewById(R.id.tempo_help).setOnClickListener(view -> {
            dial.stopRotation();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.tempo_help_title)
                    .setMessage(R.string.tempo_help_message)
                    .setPositiveButton("知道了", null)
                    .show();
        });
        save.setOnClickListener(view -> savePreset());
        renderState();
    }
    private void startPlayback() {
        startService(new Intent(this, DickPlayService.class).putExtra("bpm", bpm));
    }
    private void updatePlayback() {
        if (playing) startPlayback();
        else stopService(new Intent(this, DickPlayService.class));
        renderState();
    }
    private void renderState() {
        dial.setPlaying(playing);
        int color = getColor(playing ? R.color.tempo_playing : R.color.tempo_paused);
        panel.setBackgroundColor(color);
        getWindow().setStatusBarColor(color);
        MergeListAdapter.DataHolder item = adapter.find(selected);
        boolean dirty = item != null && item.bpm != bpm;
        save.setVisibility(dirty ? View.VISIBLE : View.GONE);
    }
    private void savePreset() {
        dial.stopRotation();
        MergeListAdapter.DataHolder item = adapter.find(selected);
        if (item != null && DataUtil.instance().addNew(this, selected, bpm)) {
            item.bpm = bpm;
            adapter.notifyDataSetChanged();
            renderState();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        }
    }
    private void showNameDialog(String oldName) {
        dial.stopRotation();
        final int newBpm = oldName == null ? bpm : adapter.find(oldName).bpm;
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("节拍名称");
        input.setText(oldName == null ? String.valueOf(newBpm) : oldName);
        input.selectAll();
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle((oldName == null ? "新增节拍 · " : "修改名称 · ") + newBpm + " BPM").setView(container)
                .setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) { input.setError("请填写名称"); return; }
                if (name.equals(oldName)) { dialog.dismiss(); return; }
                if (adapter.find(name) != null) { input.setError("名称已存在，请使用其他名称"); return; }
                if (oldName == null) {
                    if (!DataUtil.instance().addNew(this, name, newBpm)) return;
                } else {
                    if (!DataUtil.instance().rename(this, oldName, name)) return;
                    adapter.remove(oldName);
                }
                adapter.addItem(name, newBpm);
                if (oldName == null || oldName.equals(selected)) selected = name;
                adapter.setSelected(selected);
                renderState();
                dialog.dismiss();
            });
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        dialog.show();
    }
    private void deletePreset(String name) {
        if (!DataUtil.instance().del(this, name)) return;
        adapter.remove(name);
        if (name.equals(selected)) { selected = null; adapter.setSelected(null); }
        renderState();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("bpm", bpm);
        out.putString("selected", selected);
        super.onSaveInstanceState(out);
    }
    @Override protected void onPause() { dial.stopRotation(); super.onPause(); }
}
