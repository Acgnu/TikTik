package com.acgnu.dikdick;

import android.content.Context;
import android.util.AtomicFile;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** New presets store BPM directly, keyed by name. */
public final class DataUtil {
    private static final DataUtil INSTANCE = new DataUtil();
    private JSONObject data = new JSONObject();
    private DataUtil() {}
    public static DataUtil instance() { return INSTANCE; }
    private AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), "presets_bpm.json"));
    }
    public JSONObject readJson(Context context) {
        AtomicFile file = file(context);
        try {
            if (file.getBaseFile().exists()) {
                data = new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Toast.makeText(context, "无法读取预设", Toast.LENGTH_SHORT).show();
        }
        return data;
    }
    public boolean addNew(Context context, String title, int bpm) { return write(context, title, bpm); }
    public boolean del(Context context, String title) { return write(context, title, null); }
    public boolean rename(Context context, String oldName, String newName) {
        try {
            JSONObject next = new JSONObject(data.toString());
            int bpm = next.getInt(oldName);
            next.remove(oldName);
            next.put(newName, bpm);
            return persist(context, next);
        } catch (Exception e) {
            Toast.makeText(context, "修改失败，请重试", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private boolean write(Context context, String title, Integer bpm) {
        try {
            JSONObject next = new JSONObject(data.toString());
            if (bpm == null) next.remove(title);
            else next.put(title, bpm);
            return persist(context, next);
        } catch (Exception e) {
            Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private boolean persist(Context context, JSONObject next) {
        AtomicFile file = file(context);
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            stream.write(next.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
            data = next;
            return true;
        } catch (Exception e) {
            if (stream != null) file.failWrite(stream);
            Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
