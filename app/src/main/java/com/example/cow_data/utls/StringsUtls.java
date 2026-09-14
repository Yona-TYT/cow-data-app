package com.example.cow_data.utls;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringsUtls {

    // Constructor privado para evitar que la clase se instancie
    private StringsUtls() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String capitalize(String text) {
        if (text == null || text.isBlank()) return "";

        // Dividimos el texto por cualquier cantidad de espacios en blanco
        return Arrays.stream(text.strip().split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
