package dev.javacrawler.TrickyJavaObfuscator.utils;

import lombok.experimental.UtilityClass;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


@UtilityClass
public final class StringUtil {
    private static final Random RANDOM = new Random();

    public String getString(Locale locale, int length) {
        var builder = new StringBuilder();
        for (int i = 0; i < length; ++i)
            builder.append(locale.getChar());
        return builder.toString();
    }

    public String getString(int length) {
        var builder = new StringBuilder();
        for (int i = 0; i < length; ++i)
            builder.append(RANDOM.nextInt(20000));
        return builder.toString();
    }

    public String getString(Locale locale) {
        return getString(locale, ThreadLocalRandom.current().nextInt(30, 50));
    }

    public String getString() {
        return getString(ThreadLocalRandom.current().nextInt(30, 50));
    }

    public enum Locale {
        EN("qwertyuiopasdfghjklzxcvbnm"),
        RU("ёйцукенгшщзхъфывапролджэячсмитьбю"),
        NUM("1234567890");

        private final char[] table;

        Locale(String table) {
            this.table = (table.toLowerCase() + table.toUpperCase()).toCharArray();
        }

        public char getChar() {
            return table[RANDOM.nextInt(table.length)];
        }
    }
}
