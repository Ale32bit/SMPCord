package me.alexdevs;

import java.util.HashMap;
import java.util.Random;

public class Utils {
    public static String generateRandomCode() {
        var rng = new Random();
        var code = rng.nextInt(999999);
        return String.format("%06d", code);
    }

    public static <K, V> K getKeyByValue(HashMap<K, V> map, V value) {
        for (HashMap.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
