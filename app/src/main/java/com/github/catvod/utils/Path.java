package com.github.catvod.utils;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.github.catvod.Init;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Path {

    private static final String TAG = Path.class.getSimpleName();

    private static File check(File file) {
        if (!file.exists()) file.mkdirs();
        return file;
    }

    public static boolean exists(String path) {
        return new File(path.replace("file://", "")).exists();
    }

    public static File root() {
        return Environment.getExternalStorageDirectory();
    }

    public static File download() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public static File cache() {
        return Init.context().getCacheDir();
    }

    public static File thunderCache() {
        File internal = Init.context().getCacheDir();
        String dir = Prefers.getString("thunder_cache_dir", internal.getAbsolutePath());
        if (dir.equals(internal.getAbsolutePath())) return internal;
        File cache = new File(dir);
        if (!cache.exists()) return internal;
        return cache;
    }

    public static File files() {
        return Init.context().getFilesDir();
    }

    public static File externalFiles() {
        return Init.context().getExternalFilesDir("");
    }

    public static File externalCache() {
        return Init.context().getExternalCacheDir();
    }

    public static String downloadPath() {
        return download().getAbsolutePath();
    }

    public static String rootPath() {
        return root().getAbsolutePath();
    }

    public static File so() {
        return check(new File(files() + File.separator + "so"));
    }

    public static File js() {
        return check(new File(cache() + File.separator + "js"));
    }

    public static File py() {
        return check(new File(cache() + File.separator + "py"));
    }

    public static File jar() {
        return check(new File(cache() + File.separator + "jar"));
    }

    public static File doh() {
        return check(new File(cache() + File.separator + "doh"));
    }

    public static File exo() {
        return check(new File(cache() + File.separator + "exo"));
    }

    public static File epg() {
        return check(new File(cache() + File.separator + "epg"));
    }

    public static File jpa() {
        return check(new File(cache() + File.separator + "jpa"));
    }

    public static File thunder() {
        return check(new File(thunderCache() + File.separator + "thunder"));
    }

    public static File restore() {
        return check(new File(cache() + File.separator + "restore"));
    }

    public static File epg(String name) {
        return new File(epg(), name);
    }

    public static File root(String name) {
        return new File(root(), name);
    }

    public static File root(String child, String name) {
        return new File(check(new File(root(), child)), name);
    }

    public static File cache(String name) {
        return new File(cache(), name);
    }

    public static File files(String name) {
        return new File(files(), name);
    }

    public static File so(String name) {
        if (name.startsWith("http")) return new File(so(), Uri.parse(name).getLastPathSegment());
        return new File("mitv".equals(name) ? cache() : so(), "lib".concat(name).concat(".so"));
    }

    public static File js(String name) {
        return new File(js(), name);
    }

    public static File jar(String name) {
        return new File(jar(), Util.md5(name).concat(".jar"));
    }

    public static File thunder(String name) {
        return check(new File(thunder(), name));
    }

    public static File local(String path) {
        File file1 = new File(path.replace("file:/", ""));
        File file2 = new File(path.replace("file:/", rootPath()));
        return file2.exists() ? file2 : file1.exists() ? file1 : new File(path);
    }

    public static String asset(String fileName) {
        try {
            return read(Init.context().getAssets().open(fileName));
        } catch (Exception e) {
            return "";
        }
    }

    public static String read(File file) {
        try {
            return read(new FileInputStream(file));
        } catch (Exception e) {
            return "";
        }
    }

    public static String read(String path) {
        try {
            return read(new FileInputStream(local(path)));
        } catch (Exception e) {
            return "";
        }
    }

    public static String read(InputStream is) {
        try {
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();
            return new String(data, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static byte[] readToByte(File file) {
        try {
            FileInputStream is = new FileInputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static File write(File file, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(create(file));
            fos.write(data);
            fos.flush();
            fos.close();
            chmod(file);
            return file;
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return file;
        }
    }

    public static File utf8(File file) {
        return write(cache(file.getName()), Util.utf8(readToByte(file)));
    }

    public static void move(File in, File out) {
        copy(in, out);
        clear(in);
    }

    public static void copy(File in, File out) {
        try {
            copy(new FileInputStream(in), out);
        } catch (Exception ignored) {
        }
    }

    public static void copy(InputStream in, File out) {
        try {
            int read;
            byte[] buffer = new byte[8192];
            FileOutputStream fos = new FileOutputStream(create(out));
            while ((read = in.read(buffer)) != -1) fos.write(buffer, 0, read);
            fos.close();
            in.close();
        } catch (Exception ignored) {
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int amountRead;
        while ((amountRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, amountRead);
        }
    }

    public static List<File> list(File dir) {
        File[] files = dir.listFiles();
        return files == null ? Collections.emptyList() : Arrays.asList(files);
    }

    public static void clear(File dir) {
        if (dir == null) return;
        if (dir.isDirectory()) for (File file : list(dir)) clear(file);
        if (dir.delete()) Log.d(TAG, "Deleted:" + dir.getAbsolutePath());
    }

    public static void unzip(File target, File path) {
        try (ZipFile zip = new ZipFile(target.getAbsolutePath())) {
            Enumeration<?> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File out = new File(path, entry.getName());
                if (entry.isDirectory()) out.mkdirs();
                else copy(zip.getInputStream(entry), out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File chmod(File file) {
        try {
            Process process = Runtime.getRuntime().exec("chmod 777 " + file);
            process.waitFor();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return file;
        }
    }
    public static File create(File file) throws Exception {
        try {
            if (!file.canWrite()) file.setWritable(true);
            if (!file.exists()) file.createNewFile();
            Shell.exec("chmod 777 " + file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return file;
        }
    }
}
