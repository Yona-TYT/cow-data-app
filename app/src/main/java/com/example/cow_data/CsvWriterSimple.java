package com.example.cow_data;

import android.os.Build;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvWriterSimple {

    private static final String COMMA = ",";
    private static final String DEFAULT_SEPARATOR = COMMA;
    private static final String DOUBLE_QUOTES = "\"";
    private static final String EMBEDDED_DOUBLE_QUOTES = "\"\"";
    private static final String NEW_LINE_UNIX = "\n";
    private static final String NEW_LINE_WINDOWS = "\r\n";

    public static void main(String[] args) throws IOException {
        //CsvWriterSimple writer = new CsvWriterSimple();
        //writer.writeToCsvFile(createCsvDataSpecial(), new File("c:\\test\\monitor.csv"));
    }

//    public String convertToCsvFormat(final String[] line) {
//        return convertToCsvFormat(line, DEFAULT_SEPARATOR);
//    }

//    public String convertToCsvFormat(final String[] line, final String separator) {
//        return convertToCsvFormat(line, separator, true);
//    }
//
//    // if quote = true, all fields are enclosed in double quotes
//    public String convertToCsvFormat(
//            final String[] line,
//            final String separator,
//            final boolean quote) {
//
//        return Stream.of(line)                              // convert String[] to stream
//                .map(l -> formatCsvField(l, quote))         // format CSV field
//                .collect(Collectors.joining(separator));    // join with a separator
//
//    }


    public String convertToCsvFormat(final String[] line) {
        return convertToCsvFormat(line, DEFAULT_SEPARATOR, true);
    }

    public String convertToCsvFormat(final String[] line, final String separator) {
        return convertToCsvFormat(line, separator, true);
    }

    public String convertToCsvFormat(
            final String[] line,
            final String separator,
            final boolean quote) {

        if (line == null) {
            throw new IllegalArgumentException("Fila CSV null");
        }

        return Stream.of(line)
                .map(field -> formatCsvField(field, quote))  // también lambda, más claro
                .collect(Collectors.joining(separator));
    }

    private String formatCsvField(final String field, final boolean quote) {
        //Log.d("PhotoPicker", " malayaaa!!------------------------: "+ field);

        String result = (field == null) ? "" : field;

        if (result.contains(COMMA)
                || result.contains(DOUBLE_QUOTES)
                || result.contains(NEW_LINE_UNIX)
                || result.contains(NEW_LINE_WINDOWS)) {

            result = result.replace(DOUBLE_QUOTES, EMBEDDED_DOUBLE_QUOTES);
            result = DOUBLE_QUOTES + result + DOUBLE_QUOTES;
        } else if (quote) {
            result = DOUBLE_QUOTES + result + DOUBLE_QUOTES;
        }

        return result;
    }

    // put your extra login here
//    private String formatCsvField(final String field, final boolean quote) {
//
//        String result = field;
//        //Log.d("PhotoPicker", " malayaaa!!------------------------: "+ field);
//
//        if (result.contains(COMMA)
//                || result.contains(DOUBLE_QUOTES)
//                || result.contains(NEW_LINE_UNIX)
//                || result.contains(NEW_LINE_WINDOWS)) {
//
//            // if field contains double quotes, replace it with two double quotes \"\"
//            result = result.replace(DOUBLE_QUOTES, EMBEDDED_DOUBLE_QUOTES);
//
//            // must wrap by or enclosed with double quotes
//            result = DOUBLE_QUOTES + result + DOUBLE_QUOTES;
//
//        } else {
//            // should all fields enclosed in double quotes
//            if (quote) {
//                result = DOUBLE_QUOTES + result + DOUBLE_QUOTES;
//            }
//        }
//
//        return result;
//
//    }

    // a standard FileWriter, CSV is a normal text file
    public void writeToCsvFile(List<String[]> list, File file) throws IOException {
        if (list == null || list.isEmpty()) {
            throw new IOException("CSV vacío o null: no se escribe el archivo");
        }

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) {
                throw new IOException("CSV corrupto: fila null en índice " + i);
            }
        }

        List<String> collect = list.stream()
                .map(this::convertToCsvFormat)
                .collect(Collectors.toList());

        File temp = new File(file.getAbsolutePath() + ".tmp");
        if (temp.exists() && !temp.delete()) {
            throw new IOException("No se pudo borrar tmp previo");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
            for (String line : collect) {
                bw.write(line);
                bw.newLine();
            }
            bw.flush();
        }

        if (file.exists() && !file.delete()) {
            throw new IOException("No se pudo reemplazar el CSV anterior");
        }

        // renameTo puede fallar en algunos dispositivos
        if (!temp.renameTo(file)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.move(
                            temp.toPath(),
                            file.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            } catch (IOException e) {
                throw new IOException("No se pudo mover el CSV temporal a destino", e);
            }
        }
    }

//    public String convertToCsvFormat(final String[] line, final String separator, final boolean quote) {
//        if (line == null) {
//            throw new IllegalArgumentException("Fila CSV null");
//        }
//        return Stream.of(line)
//                .map(l -> formatCsvField(l == null ? "" : l, quote))
//                .collect(Collectors.joining(separator));
//    }

    public static List<String[]> createCsvDataSpecial(List<String[]> list ) {


        //String[] header = new ArrayList<>().toArray( new String[1]); // {"Make", "Model", "Description", "Price"};
        String[] record1 = {"Dell", "P3421W", "Dell 34, Curved, USB-C Monitor", "2499.00"};
        String[] record2 = {"Dell", "", "Alienware 38 Curved \"Gaming Monitor\"", "6699.00"};
        String[] record3 = {"Samsung", "", "49\" Dual QHD, QLED, HDR1000", "6199.00"};
        String[] record4 = {"Samsung", "", "Promotion! Special Price\n49\" Dual QHD, QLED, HDR1000", "4999.00"};

        return list;

    }

}
