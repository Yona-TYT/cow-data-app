package com.example.cow_data;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

public class FilesManager extends AppCompatActivity {
    Context context2;

    @SuppressLint("NotConstructor")
    public void FilesManager(){
    }

    public String getImage(String sImage, ImageView mImgPrev) {
        if (!sImage.isEmpty()) {
            Uri mUri = Uri.fromFile(new File(sImage));
            try {
                if (isBlockedPath(this, sImage)) {
                    mImgPrev.setImageURI(mUri);
                    return  sImage;
                }
                else {
                    Log.d("PhotoPicker", "noooooo hayyyyyyyyyy: " + sImage);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sImage;
    }

    public String SavePhoto(Bitmap bmp, String fName, Uri oldFile, Context contex, ContentResolver resolver){

        //Creamos el directorio para los archivos
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/");
        boolean isDiralloway = true;
        if(!path.exists()){
            isDiralloway = path.mkdir();
        }
        //------------------------------------------

        //Si se crea correctamente entonces procede a escribir
        if(isDiralloway) {
            File file = new File(path, fName);
            FileOutputStream stream = null;

//            Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy 11100------------------------: " );

            try {
                stream = new FileOutputStream(file);

                // Use the compress method on the BitMap object to write image to the OutputStream
                if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)){
                    throw new RuntimeException("Could Save Bit map");
                }
                else {
                    return file.getAbsolutePath();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                try {
                    stream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    public File csvExport(List<String[]> list) throws IOException {
        // Definimos la class
        CsvWriterSimple write = new CsvWriterSimple();

        //Creamos el directorio para los archivos
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/");
        boolean isDiralloway = true;
        if(!path.exists()){
            isDiralloway = path.mkdir();
        }
        //------------------------------------------

        //Si se crea correctamente entonces procede a escribir
        if(isDiralloway) {
            LocalDate currdate = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                currdate = LocalDate.now();
            }
            String name = (currdate == null? "CowData_Save.csv" : "CowData_"+currdate.toString()+".csv" );
            File file = new File(path, name);
            write.writeToCsvFile(list, file);
            return file;
        }
        //-----------------------------------------------------------
        return null;
    }

    public boolean csvImport(String dir) throws IOException, CsvValidationException {
        Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: "+ dir );

        //Se detecta si el archivo existe
        File file = new File(dir);
        if(file.exists()){
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                //System.out.println(nextLine[0] + nextLine[1] + "etc...");
                Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: "+ nextLine[0] );
            }
        }
        //-----------------------------------------------------------
        return true;
    }

    public static void DeleteFile(File file) {

        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                File currFile = new File(file, children[i]);
                String name = currFile.getName();
                if(name.endsWith(".csv")) {
                    //Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: " + name);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        boolean threis = currFile.exists();
                        if(threis) {
                            currFile.delete();
                        }
                    }
                }
            }
        }
    }


    public void RemoveFile(String dir, ContentResolver resolver) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
           // Log.d("PhotoPicker", " =======================Aquiiiiiiiiii Hayyyyyy 11100------------------------: " );
            File file = new File(dir);
            boolean threis = file.exists();
            if(threis) {
                file.delete();
            }
        }
    }
    public boolean nameCompare(String a, String b) {
        // Paths that should rarely be exposed
        if (a.startsWith(b)){
            return true;
        }
        return false;
    }

    boolean isBlockedPath(Context ctx, String dir) {
        // Paths that should rarely be exposed
        return dir.startsWith("content://media/" + MediaStore.VOLUME_EXTERNAL_PRIMARY) || dir.startsWith("/storage/emulated/0/Documents/");
    }
}
