package com.example.figurecollection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FigureAdapter adapter;
    private DBHelper dbHelper;
    private View emptyView;
    private final List<Object> displayItems = new ArrayList<>();
    private List<Figure> allFigures = new ArrayList<>();

    private static final int REQ_PICK_IMAGE = 1001;
    private static final int REQ_TAKE_PHOTO = 1002;
    private static final int REQ_CAMERA_PERM = 1003;

    private Figure editingFigure = null;
    private String pendingPhotoPath = null;
    private ImageView dialogImageView = null;
    private File cameraTempFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FigureAdapter(displayItems, figure -> showEditDialog(figure));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> showEditDialog(null));
        loadFigures();
    }

    private void loadFigures() {
        allFigures.clear();
        allFigures.addAll(dbHelper.getAllFigures());

        // 按系列分组
        Map<String, List<Figure>> grouped = new HashMap<>();
        for (Figure f : allFigures) {
            String cat = f.getSizeCategory();
            if (!grouped.containsKey(cat)) {
                grouped.put(cat, new ArrayList<>());
            }
            grouped.get(cat).add(f);
        }

        // 提取分类并按数值排序
        List<String> categories = new ArrayList<>(grouped.keySet());
        Collections.sort(categories, (s1, s2) -> {
            int n1 = extractNumber(s1);
            int n2 = extractNumber(s2);
            if (n1 == -1 && n2 == -1) return s1.compareTo(s2);
            if (n1 == -1) return 1; // 未分类放最后
            if (n2 == -1) return -1;
            return Integer.compare(n1, n2);
        });

        // 重组为 混合列表（标题 + 手办）
        displayItems.clear();
        for (String cat : categories) {
            displayItems.add(cat); // 添加标题
            displayItems.addAll(grouped.get(cat)); // 添加该分类下的手办
        }

        adapter.notifyDataSetChanged();
        emptyView.setVisibility(allFigures.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int extractNumber(String s) {
        Matcher m = Pattern.compile("\\d+").matcher(s);
        if (m.find()) {
            try { return Integer.parseInt(m.group()); } catch (Exception e) { }
        }
        return -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_stats) {
            showStatistics();
            return true;
        } else if (id == R.id.action_export) {
            exportAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStatistics() {
        if (allFigures.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("统计")
                    .setMessage("还没有手办数据哦～")
                    .setPositiveButton("知道了", null)
                    .show();
            return;
        }

        int total = allFigures.size();
        int withPhoto = 0;
        double sumL = 0, sumW = 0, sumH = 0;
        int countL = 0, countW = 0, countH = 0;
        Map<String, Integer> seriesMap = new HashMap<>();

        for (Figure f : allFigures) {
            if (f.photoPath != null && new File(f.photoPath).exists()) withPhoto++;

            Double l = parseNumber(f.length);
            Double w = parseNumber(f.width);
            Double h = parseNumber(f.height);
            if (l != null) { sumL += l; countL++; }
            if (w != null) { sumW += w; countW++; }
            if (h != null) { sumH += h; countH++; }

            String cat = f.getSizeCategory();
            seriesMap.put(cat, seriesMap.getOrDefault(cat, 0) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("手办总数：").append(total).append(" 个\n");
        sb.append("有照片：").append(withPhoto).append(" 个\n\n");

        sb.append("—— 系列统计 ——\n");
        for (String key : seriesMap.keySet()) {
            sb.append(key).append("：").append(seriesMap.get(key)).append(" 个\n");
        }

        sb.append("\n—— 尺寸合计 ——\n");
        sb.append("长：").append(countL == 0 ? "—" : fmt(sumL)).append("\n");
        sb.append("宽：").append(countW == 0 ? "—" : fmt(sumW)).append("\n");
        sb.append("高：").append(countH == 0 ? "—" : fmt(sumH)).append("\n");

        new AlertDialog.Builder(this)
                .setTitle("手办统计")
                .setMessage(sb.toString())
                .setPositiveButton("知道了", null)
                .show();
    }

    private Double parseNumber(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("-?\\d+(\\.\\d+)?").matcher(s);
        if (m.find()) {
            try { return Double.parseDouble(m.group()); } catch (Exception e) { return null; }
        }
        return null;
    }

    private String fmt(double d) {
        if (Math.abs(d - Math.round(d)) < 0.001) return String.valueOf((long) Math.round(d));
        return String.format("%.2f", d);
    }

    private void exportAll() {
        if (allFigures.isEmpty()) {
            Toast.makeText(this, "没有可导出的数据", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(getCacheDir(), "export");
            if (!dir.exists() && !dir.mkdirs()) return;
            File csv = new File(dir, "手办清单_" + System.currentTimeMillis() + ".csv");

            FileOutputStream fos = new FileOutputStream(csv);
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            writer.write("序号,名称,长,宽,高,照片路径\n");
            int i = 1;
            for (Figure f : allFigures) {
                writer.write(i++ + "," + escapeCsv(f.name) + "," + escapeCsv(f.length) + "," +
                        escapeCsv(f.width) + "," + escapeCsv(f.height) + "," + escapeCsv(f.photoPath) + "\n");
            }
            writer.flush(); writer.close(); fos.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", csv);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "手办清单");
            share.putExtra(Intent.EXTRA_TEXT, "共 " + allFigures.size() + " 个手办");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "导出到"));
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private void showEditDialog(Figure figure) {
        editingFigure = figure;
        pendingPhotoPath = (figure != null) ? figure.photoPath : null;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null);
        dialogImageView = view.findViewById(R.id.imagePreview);
        EditText etName = view.findViewById(R.id.etName);
        EditText etLength = view.findViewById(R.id.etLength);
        EditText etWidth = view.findViewById(R.id.etWidth);
        EditText etHeight = view.findViewById(R.id.etHeight);

        if (figure != null) {
            etName.setText(figure.name);
            etLength.setText(figure.length);
            etWidth.setText(figure.width);
            etHeight.setText(figure.height);
            showPhoto(pendingPhotoPath);
        }

        view.findViewById(R.id.btnPick).setOnClickListener(v -> pickImage());
        view.findViewById(R.id.btnCamera).setOnClickListener(v -> takePhoto());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(figure == null ? "添加手办" : "编辑手办");
        builder.setView(view);
        builder.setPositiveButton("保存", null);
        builder.setNegativeButton("取消", null);
        builder.setNeutralButton("删除", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入手办名称", Toast.LENGTH_SHORT).show();
                return;
            }
            String l = etLength.getText().toString().trim();
            String w = etWidth.getText().toString().trim();
            String h = etHeight.getText().toString().trim();

            if (editingFigure == null) dbHelper.insertFigure(name, pendingPhotoPath, l, w, h);
            else dbHelper.updateFigure(editingFigure.id, name, pendingPhotoPath, l, w, h);
            dialog.dismiss();
            loadFigures();
        });

        if (figure == null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
        } else {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("删除")
                        .setMessage("确定删除「" + figure.name + "」吗？")
                        .setPositiveButton("删除", (d, which) -> {
                            dbHelper.deleteFigure(figure.id);
                            dialog.dismiss();
                            loadFigures();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_PICK_IMAGE);
    }

    private void takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            File dir = new File(getCacheDir(), "images");
            if (!dir.exists() && !dir.mkdirs()) return;
            cameraTempFile = new File(dir, "camera_" + System.currentTimeMillis() + ".jpg");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", cameraTempFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQ_TAKE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开相机：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        try {
            if (requestCode == REQ_PICK_IMAGE && data != null && data.getData() != null) {
                String saved = saveImageToInternal(data.getData());
                if (saved != null) { pendingPhotoPath = saved; showPhoto(saved); }
            } else if (requestCode == REQ_TAKE_PHOTO) {
                if (cameraTempFile != null && cameraTempFile.exists()) {
                    File destDir = new File(getFilesDir(), "figures");
                    if (!destDir.exists()) destDir.mkdirs();
                    File dest = new File(destDir, "fig_" + System.currentTimeMillis() + ".jpg");
                    copyFile(cameraTempFile, dest);
                    pendingPhotoPath = dest.getAbsolutePath();
                    showPhoto(pendingPhotoPath);
                    cameraTempFile.delete();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "处理图片失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) launchCamera();
            else Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveImageToInternal(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "figures");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, "fig_" + System.currentTimeMillis() + ".jpg");
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close();
            return dest.getAbsolutePath();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private void copyFile(File src, File dst) throws Exception {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close(); out.close();
    }

    private void showPhoto(String path) {
        if (dialogImageView == null) return;
        if (path == null || !new File(path).exists()) {
            dialogImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        dialogImageView.setImageBitmap(decodeSampled(path, 600));
    }

    private Bitmap decodeSampled(String path, int reqSize) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        int scale = 1;
        while (opts.outWidth / scale > reqSize || opts.outHeight / scale > reqSize) scale *= 2;
        BitmapFactory.Options opts2 = new BitmapFactory.Options();
        opts2.inSampleSize = scale;
        return BitmapFactory.decodeFile(path, opts2);
    }
}
