package me.edgan.redditslide.util;

import android.graphics.Color;

public class ColorParser {

    /**
     * Parses a CSS-style color string and converts it into an int color.
     * Supports formats like "#RRGGBB", "#AARRGGBB", and named colors.
     *
     * @param colorString The CSS-style color string.
     * @return The parsed color as an int.
     * @throws IllegalArgumentException If the color string is invalid.
     */
    public static int parseCssColor(String colorString) throws IllegalArgumentException {
        // Handle named colors (if necessary, extend this list as needed)
        switch (colorString.toLowerCase()) {
            case "red": return Color.RED;
            case "green": return Color.GREEN;
            case "blue": return Color.BLUE;
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "gray": return Color.GRAY;
            case "grey": return Color.GRAY; // Alternate spelling
            case "yellow": return Color.YELLOW;
            case "cyan": return Color.CYAN;
            case "magenta": return Color.MAGENTA;
            default:
                // Default to parsing hex colors
                return Color.parseColor(colorString);
        }
    }
}

