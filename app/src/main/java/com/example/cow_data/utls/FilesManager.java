package com.example.cow_data.utls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.example.cow_data.AppContextProvider;
import com.example.cow_data.CsvWriterSimple;
import com.example.cow_data.R;
import com.example.cow_data.StartVar;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class FilesManager {

    private final Context context;

    /**
     * Constructor vacío para tareas de fondo (como DriveManager, WorkManager o hilos pool).
     */
    public FilesManager() {
        this.context = null;
    }

    /**
     * Constructor con Context para cuando requieras interactuar con la UI o lógica local.
     */
    public FilesManager(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public String getImage(String sImage, ImageView mImgPrev) {
        // Si no hay contexto o el string está vacío, no procesamos la UI
        if (context == null || sImage.isEmpty()) return sImage;

        Uri mUri = Uri.fromFile(new File(sImage));
        try {
            if (isBlockedPath(sImage)) {
                mImgPrev.setImageURI(mUri);
                return sImage;
            } else {
                Log.d("PhotoPicker", "noooooo hayyyyyyyyyy: " + sImage);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sImage;
    }

    /**
     * Guarda un objeto Bitmap en el almacenamiento local dentro de la carpeta Documents de la app.
     */
    public String SavePhoto(Bitmap bmp, String fName) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + "/" + StartVar.dirAppName + "/");
        boolean isDiralloway = path.exists() || path.mkdir();

        if (isDiralloway) {
            File file = new File(path, fName);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw new RuntimeException("Could Not Save Bit map");
                } else {
                    return file.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }


    public static void setImageView(String path, ImageView view){
        File imgFile = new File(path);

        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            view.setImageBitmap(myBitmap);
        }
    }

    public File csvExport(List<String[]> list, String fileName) throws IOException {
        if (list == null || list.isEmpty()) {
            Log.e("FilesManager", "csvExport: lista vacía");
            return null;
        }

        File path = directoryCreate();
        if (path == null) {
            Log.e("FilesManager", "csvExport: no se pudo crear directorio");
            return null;
        }

        String myName = StartVar.csvAppName;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            myName = AppContextProvider.getContext().getString(R.string.app_name)
                    + LocalDate.now()
                    + "_"
                    + LocalTime.now().toString().replaceAll("\\D", "_")
                    + ".csv";
        }

        File file = new File(path, (fileName == null || fileName.isEmpty()) ? myName : fileName);

        // 1) Quitar el archivo anterior (mismo nombre = DataSave.bin siempre)
        if (file.exists() && !file.delete()) {
            Log.w("FilesManager", "No se pudo borrar previo: " + file.getAbsolutePath());
        }

        // 2) Escritura limpia (CsvWriterSimple debe crear el file de cero)
        CsvWriterSimple write = new CsvWriterSimple();
        write.writeToCsvFile(list, file);

        if (!file.exists() || file.length() == 0) {
            Log.e("FilesManager", "csvExport: archivo vacío o no creado");
            return null;
        }

        // 3) Sync REAL sobre el file ya escrito (append = false, solo para forzar flush a disco)
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            // no escribir bytes; solo obtener el FD del file existente
            fos.getFD().sync();
        } catch (Exception e) {
            Log.w("FilesManager", "sync opcional falló: " + e.getMessage());
        }

        file.setLastModified(System.currentTimeMillis());

        Log.d("FilesManager", "csvExport OK: " + file.getName()
                + " size=" + file.length());

        return file;
    }

    public File csvExport(List<String[]> list) throws IOException {
        CsvWriterSimple write = new CsvWriterSimple();


        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + "/" + StartVar.dirAppName + "/");
        boolean isDiralloway = path.exists() || path.mkdir();

        if (isDiralloway) {
            LocalDate currdate = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                currdate = LocalDate.now();
            }
            String name = (currdate == null ? StartVar.csvAppName : StartVar.fileName+"_" + currdate.toString() + ".csv");
            File file = new File(path, name);
            write.writeToCsvFile(list, file);
            return file;
        }
        return null;
    }
    public static File directoryCreate() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + "/" + StartVar.dirAppName + "/");
        boolean isDiralloway = path.exists() || path.mkdir();
        return isDiralloway ? path : null;
    }

    public boolean csvImport(String dir) throws IOException, CsvValidationException {
        Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: " + dir);
        File file = new File(dir);
        if (file.exists()) {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                String[] nextLine;
                while ((nextLine = reader.readNext()) != null) {
                    Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: " + nextLine[0]);
                }
            }
        }
        return true;
    }

    public static void DeleteFile(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            if (children != null) {
                for (String child : children) {
                    File currFile = new File(file, child);
                    if (currFile.getName().endsWith(".csv") && currFile.exists()) {
                        currFile.delete();
                    }
                }
            }
        }
    }

    public void RemoveFile(String dir) {
        File file = new File(dir);
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean nameCompare(String a, String b) {
        return a.startsWith(b);
    }

    private boolean isBlockedPath(String dir) {
        return dir.startsWith("content://media/" + MediaStore.VOLUME_EXTERNAL_PRIMARY) || dir.startsWith("/storage/emulated/0/Documents/");
    }

    /**
     * Copia un archivo original y lo guarda con un nuevo nombre en el directorio de la aplicación.
     */
    public static File getNewFile(String rutaOriginal, String newName) throws IOException {
        File originalFile = new File(rutaOriginal);
        if (!originalFile.exists()) {
            return null;
        }

        File destinationDir = directoryCreate();
        if (destinationDir == null) {
            return null;
        }

        File destFile = new File(destinationDir, newName);

        // Copiado de archivos rápido a nivel de canales de NIO
        try (FileChannel sourceChannel = new FileInputStream(originalFile).getChannel();
             FileChannel destChannel = new FileOutputStream(destFile).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }

        return destFile;
    }

    public static boolean isCsvSafeToUpload(File csvFile) {
        if (csvFile == null || !csvFile.exists() || csvFile.length() == 0) {
            return false;
        }

        boolean hasSection0 = false;
        boolean hasConf = false;
        boolean hasEnd = false;
        boolean confTimestampsOk = false;
        String firstDataLine = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String clean = line.replace("\"", "").trim();
                if (clean.isEmpty()) continue;

                if (firstDataLine == null) {
                    firstDataLine = clean;
                }

                if ("<0>".equals(clean)) {
                    hasSection0 = true;
                }
                if (clean.startsWith("confID0")) {
                    hasConf = true;
                    String[] spl = clean.split(",");
                    if (spl.length >= 8) {
                        try {
                            Long.parseLong(spl[6].trim());
                            Long.parseLong(spl[7].trim());
                            confTimestampsOk = true;
                        } catch (NumberFormatException ignored) {
                            confTimestampsOk = false;
                        }
                    }
                }
                if ("<end>".equals(clean)) {
                    hasEnd = true;
                }
            }
        } catch (IOException e) {
            return false;
        }

        // Inicio: la primera línea debería ser <0>
        if (firstDataLine == null || !firstDataLine.equals("<0>")) {
            Log.e("CSV", "Inicio inválido. Primera línea: " + firstDataLine);
            return false;
        }
        if (!hasSection0 || !hasConf || !confTimestampsOk) {
            Log.e("CSV", "Config inválida");
            return false;
        }
        if (!hasEnd) {
            Log.e("CSV", "Falta <end>");
            return false;
        }
        return true;
    }

    // Old -----------------------------------------------------------------
    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        // Verificar si la Uri es null
        if (uri == null) {
            throw new IOException("Uri es null");
        }

        // Obtener el ContentResolver
        String fileName = getFileName(context, uri);
        File file = new File(context.getCacheDir(), fileName != null ? fileName : "temp_file");

        // Copiar el contenido de la Uri a un archivo temporal
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }

        return file;
    }

    // Método para obtener el nombre del archivo desde la Uri (opcional)
    private static String getFileName(Context context, Uri uri) {
        String fileName = null;
        String[] projection = { android.provider.MediaStore.MediaColumns.DISPLAY_NAME };

        try (android.database.Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileName;
    }
}