package com.example.cow_data.utls;

import com.example.cow_data.AppContextProvider;
import com.example.cow_data.GlobalData;
import com.example.cow_data.StartVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class BitsOper {
    private GlobalData glData = GlobalData.getInstance(AppContextProvider.getContext());

    public BitsOper() {}

    public static int bitL(int val, int rota) {
        return val << rota;
    }

    public static int bitR(int val, int rota) {
        return (val >> rota) & 1;
    }

    public static int toHex(String value) {
        return Integer.decode(value);
    }

    public static List<Integer> getBits(int n) {
        List<Integer> list = new ArrayList<>();
        while (true) {
            if(n < 32){
                list.add(bitL(0x1, n));
                break;
            }
            else{
                list.add(0);
                n -= 32;
            }
        }
        return list;
    }

    public static List<Integer> getBits(String text) {
        if(!text.startsWith("0x")){
            text = "0x0000";
        }
        String[] sList = text.split("'");
        if(sList.length == 0){
            sList = new String[]{text};
        }
        List<Integer> list = new ArrayList<>();
        for (String val: sList){
            list.add(Integer.decode(val));
        }
        return list;
    }

    public static boolean isActiveBit(String hexString, int globalPos){

        List<Integer> bitList = BitsOper.getBits(hexString);

        if (bitList.isEmpty()) {
            bitList.add(0);  // Caso base: al menos un grupo de 0
        }

        if (globalPos < 0) {
            return false;
        }

        // Cálculo correcto: grupo y offset
        int group = globalPos / 32;
        int offset = globalPos % 32;

        // Padda con 0 si el grupo no existe
        while (group >= bitList.size()) {
            bitList.add(0);
        }

        int mByte = bitList.get(group);

        // Extracción del bit
        return BitsOper.bitR(mByte, offset) == 1;
    }

    public static String saveBits(List<Integer> list, int accNr, boolean b) {
        String text = "";
        for (Integer val: list){
            text = String.format("%x",val);
            if(!text.startsWith("0x")){
                text = "0x"+text;
            }
            //msg(text);
        }
        return text;
    }



//    public static String saveNewBit(int r){
//        String bit = "0x1";
//        for(int i = 0 ; i < r; i += 32) {
//            if(r < 32) {
//                bit = String.format("0x%x", bitL(0x1, r))+"'";
//            }
//            else{
//                r -= 32;
//            }
//        }
//        return bit;
//    }
    public static List<Integer> mergeBitList(List<Integer> intList) {
        if (intList == null || intList.isEmpty()) {
            return new ArrayList<>();
        }

        // Cambio clave: filtra r >= 0 para incluir r=0 en maxR
        OptionalInt maxR = intList.stream()
                .filter(r -> r >= 0)  // Cambiado: >=0 en lugar de >0
                .mapToInt(Integer::intValue)
                .max();
        if (!maxR.isPresent()) {
            return new ArrayList<>();  // Solo negativos: vacío
        }
        int numGroups = (maxR.getAsInt() / 32) + 1;

        List<Integer> result = new ArrayList<>(Collections.nCopies(numGroups, 0));

        // Setea bits para cada r >=0 (incluye r=0 como bit 0)
        for (int r : intList) {
            if (r >= 0) {
                int group = r / 32;
                int offset = r % 32;
                if (group < numGroups) {
                    result.set(group, result.get(group) | (1 << offset));
                }
            }
        }

        return result;
    }

    // Método modificado: combina todos los r's con OR y retorna String hex
    public static String mergeBitString(List<Integer> intList) {
        List<Integer> merged = mergeBitList(intList);

        if (merged.isEmpty()) {
            return "0x00000000'";
        }

        // Convierte a hex: 0x + 8 dígitos por grupo, unidos con espacio + "'"
        String hexStr = merged.stream()
                .map(w -> String.format("0x%08x", w))
                .collect(Collectors.joining(" ")) + "'";
        return hexStr;
    }

    public static String saveNewBit(int r) {

        if (r < 0) {
            return "0x00000000'";
        }
        int group = r / 32;  // Grupo donde va el bit (0-based)
        int offset = r % 32; // Posición dentro del grupo
        int numGroups = group + 1;  // Número total de grupos needed

        List<String> hexGroups = new ArrayList<>();
        for (int g = 0; g < numGroups; g++) {
            int word = 0;  // Todos en 0 por defecto
            if (g == group) {
                word = 1 << offset;  // Setea el bit en este grupo
            }
            // Formato: 0x + 8 dígitos hex (padded con ceros)
            String hex = String.format("0x%08x", word);
            hexGroups.add(hex);
        }

        // Une con espacios y agrega el "'" al final
        return String.join(" ", hexGroups) + "'";
    }

    /**
     * Cambia un bit específico en el string hex.
     * @param hexString El string como "0x00000001'" o "0x00000000 0x00000002'".
     * @param pos Posición del bit (0 = LSB del primer grupo).
     * @param activeBit true para setear a 1, false para a 0.
     * @return El string actualizado.
     */
    public static String setBitInHexString(String hexString, int pos, boolean activeBit) {
        if (hexString == null || hexString.trim().isEmpty()) {
            // Si inválido, inicia con 0
            hexString = "0x00000000'";
        }

        // Parsea a List<Integer>
        List<Integer> groups = extractBitGroups(hexString);

        if (groups.isEmpty()) {
            groups.add(0);  // Al menos un grupo de 0
        }

        int group = pos / 32;
        int offset = pos % 32;

        // Padda si el grupo no existe
        while (group >= groups.size()) {
            groups.add(0);
        }

        // Modifica el bit
        int mask = 1 << offset;
        int current = groups.get(group);
        if (activeBit) {
            groups.set(group, current | mask);  // Setea a 1
        } else {
            groups.set(group, current & ~mask);  // Clear a 0
        }

        // Formatea de vuelta a string
        String hexStr = groups.stream()
                .map(w -> String.format("0x%08x", w))
                .collect(Collectors.joining(" ")) + "'";
        return hexStr;
    }

    // Nuevo método: extrae grupos hex de string a List<Integer>
    public static List<Integer> extractBitGroups(String hexString) {
        List<Integer> groups = new ArrayList<>();
        if (hexString == null || hexString.trim().isEmpty()) {
            return groups;  // Vacía si input inválido
        }

        // Divide por espacios (\\s+ maneja uno o más)
        String[] tokens = hexString.trim().split("\\s+");

        for (String token : tokens) {
            if (token.startsWith("0x") && token.length() > 2) {
                try {
                    // Remueve "0x" y parsea hex (ignora el "'" si está al final)
                    String hexPart = token.substring(2).replace("'", "").toLowerCase();  // Normaliza a minúsculas
                    int value = Integer.parseInt(hexPart, 16);
                    groups.add(value);
                } catch (NumberFormatException e) {
                    // Ignora tokens inválidos (ej. hex mal formado)
                    System.err.println("Token hex inválido: " + token);
                }
            }
        }

        return groups;
    }


    public static List<Object> setBits(boolean b, int idx){

        //Basic.msg(""+mClt.cliente+" idx "+idx);

        List<Object> mList = new ArrayList<>();
        int add = 0;
        for (int i = 0; i <= idx; i+=32){

            int emptyByte = 0x0;
            int currBit = bitL(0x1, idx);

            //Basic.msg("siz "+siz+" bitList.size() "+bitList.size());

            if(b) {
                //Basic.msg(String.format("+ %x ", mByte | currBit));
                mList.add(currBit);
            }
            else {
                //Basic.msg(String.format("- %x ", mByte ^ currBit));
                mList.add(emptyByte);
            }

//                StartVar.setCltBit(BitsOper.saveBits(bitList));
//                daoClt.updateBits(mClt.cliente, StartVar.cltBit);

            add+=32;
        }
        return mList;
    }

//    public List<Object> setBits(boolean b, int idx, int cltId){
//        DaoClt daoClt = StartVar.appDBall.daoClt();
//        Cliente mClt = daoClt.getUsers("cltID"+cltId);
//
//        //Basic.msg(""+mClt.cliente+" idx "+idx);
//
//        List<Object> mList = new ArrayList<>();
//        int siz = 0;
//        int add = 0;
//        for (int i = 0; i <= idx; i+=32){
//
//            int mByte = 0x0;
//            int currBit = bitL(0x1, idx);
//
//            List<Integer> bitList = BitsOper.getBits("");
//
//            //Basic.msg("siz "+siz+" bitList.size() "+bitList.size());
//
//            if(siz < bitList.size()){
//                mByte = bitList.get(siz);
//                if(b) {
//                    //Basic.msg(String.format("+ %x ", mByte | currBit));
//                    bitList.set(siz, (mByte | currBit));
//                }
//                else {
//                    //Basic.msg(String.format("- %x ", mByte ^ currBit));
//                    bitList.set(siz, (mByte ^ currBit));
//                }
//                mList.add(bitList);
//
////                StartVar.setCltBit(BitsOper.saveBits(bitList));
////                daoClt.updateBits(mClt.cliente, StartVar.cltBit);
//            }
//            add+=32;
//            siz++;
//        }
//        return mList;
//    }
}
