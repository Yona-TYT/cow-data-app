package com.example.cow_data;

//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.assertEquals;

import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CsvWriterSimpleTest {
    private String indy = "----------- Aqui Hay Aqui hay Aqui hayyyyyyyyyyyyy!!!! : ";
    private String indn = "----------- Aqui no hay :(  :";

    private CsvWriterSimple writer = new CsvWriterSimple();

//    @TestOnly
    void test_convert_csv_line_default(List<String[]> list) throws IOException {
        String[] record = {"1", "apple", "10", "9.99"};
        String expected = "\"1\",\"apple\",\"10\",\"9.99\"";
        String result = writer.convertToCsvFormat(record);
        Log.d(indy+expected, result);

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
            File file = new File(path, "CowData_Save.csv");
            write.writeToCsvFile(list, file);
        }
        //-----------------------------------------------------------
    }

//    @TestOnly
    void test_convert_csv_line_empty() {
        String[] record = {"1", "", "10", ""};
        String expected = "\"1\",\"\",\"10\",\"\"";

        String result = writer.convertToCsvFormat(record);
        Log.d(indy+expected, result);
    }

//    @TestOnly
    void test_convert_csv_line_custom_separator() {
        String[] record = {"1", "apple", "10", "9.99"};
        String expected = "\"1\";\"apple\";\"10\";\"9.99\"";


        String result = writer.convertToCsvFormat(record, ";");
        Log.d(indy+expected, result);
    }

//    @TestOnly
    void test_convert_csv_line_no_quoted() {
        String[] record = {"1", "apple", "10", "9.99"};
        String expected = "1,apple,10,9.99";

        String result = writer.convertToCsvFormat(record, ",", false);
        Log.d(indy+expected, result);
    }

//    @TestOnly
    void test_convert_csv_line_contains_comma() {
        String[] record = {"1", "apple,orange", "10", "9.99"};
        String expected = "\"1\",\"apple,orange\",\"10\",\"9.99\"";

        String result = writer.convertToCsvFormat(record);
        Log.d(indy+expected, result);
    }

//    @TestOnly
    void test_convert_csv_line_contains_double_quotes() {
        String[] record = {"1", "12\"apple", "10", "9.99"};
        String expected = "\"1\",\"12\"\"apple\",\"10\",\"9.99\"";

        String result = writer.convertToCsvFormat(record);
        Log.d(indy+expected, result);
    }

//    @TestOnly
    void test_convert_csv_line_contains_newline() {
        String[] record = {"1", "promotion!\napple", "10", "9.99"};
        String expected = "\"1\",\"promotion!\napple\",\"10\",\"9.99\"";

        String result = writer.convertToCsvFormat(record);
        Log.d(indy+expected, result);
    }

}