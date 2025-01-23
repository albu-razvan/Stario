/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.utils;

public class Casing {
    private static final String WORD_SEPARATORS = " .-_/()";

    /**
     * Converts an input string into sentence case, capitalizing the first word in the string and every word that
     * follows a period.
     * @param s input string
     * @return string transformed into sentence case
     */
    public static String toSentenceCase(final String s) {
        final StringBuilder sb = new StringBuilder(s);
        return toSentenceCase(sb).toString();
    }

    private static StringBuilder toSentenceCase(final StringBuilder sb) {
        boolean capitalizeNext = true;
        for (int i = 0; i < sb.length(); i++) {
            final char c = sb.charAt(i);
            if (c == '.') {
                capitalizeNext = true;
            } else if (capitalizeNext && !isSeparator(c)) {
                sb.setCharAt(i, Character.toTitleCase(c));
                capitalizeNext = false;
            } else if (!Character.isLowerCase(c)) {
                sb.setCharAt(i, Character.toLowerCase(c));
            }
        }
        return sb;
    }

    private static boolean isSeparator(char c) {
        return WORD_SEPARATORS.indexOf(c) >= 0;
    }

    /**
     * Converts an input string into title case, capitalizing the first character of every word.
     * @param s input string
     * @return string transformed into title case
     */
    public static String toTitleCase(final String s) {
        final StringBuilder sb = new StringBuilder(s);
        return toTitleCase(sb).toString();
    }

    private static StringBuilder toTitleCase(final StringBuilder sb) {
        boolean capitalizeNext = true;
        for (int i = 0; i < sb.length(); i++) {
            final char c = sb.charAt(i);
            if (isSeparator(c)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.setCharAt(i, Character.toTitleCase(c));
                capitalizeNext = false;
            } else if (!Character.isLowerCase(c)) {
                sb.setCharAt(i, Character.toLowerCase(c));
            }
        }
        return sb;
    }
}