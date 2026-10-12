package software.santodan.extension.nuviomenu;

/** Validated choices shared by the settings controls and date-label renderer. */
public final class DateLabelStyle {
    public static final String[] KEYS = {"position", "size", "background", "font_size", "foreground"};
    public static final String[] TITLES = {"Label position", "Label size", "Label color", "Font size", "Font color"};
    public static final String[] POSITIONS = {"Top left", "Top center", "Top right", "Center left", "Center", "Center right", "Bottom left", "Bottom center", "Bottom right"};
    public static final String[] SIZES = {"50%", "75%", "100% (default)", "125%", "150%", "175%", "200%", "250%", "300%"};
    private static final float[] SCALE = {.5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f};
    public static final String[] COLORS = {"Blue", "White", "Black", "Red", "Green", "Orange", "Purple", "Yellow", "Cyan", "Pink", "Gray", "Transparent"};
    private static final int[] ARGB = {0xff1976d2, 0xffffffff, 0xff000000, 0xffd32f2f, 0xff388e3c, 0xfff57c00, 0xff7b1fa2, 0xffffeb3b, 0xff00bcd4, 0xffe91e63, 0xff616161, 0x00000000};
    private static final int[] DEFAULTS = {7, 2, 0, 6, 1};
    private static final int[] COUNTS = {9, 9, 12, 33, 11};
    private DateLabelStyle() {}
    public static int defaultChoice(int property) { return DEFAULTS[property]; }
    public static String[] choices(int property) {
        if (property == 0) return POSITIONS.clone();
        if (property == 1) return SIZES.clone();
        if (property == 2) return COLORS.clone();
        if (property == 4) return java.util.Arrays.copyOf(COLORS, COLORS.length - 1);
        String[] sizes = new String[33];
        for (int index = 0; index < sizes.length; index++) sizes[index] = (index + 8) + " sp";
        return sizes;
    }
    public static int valid(int property, int value) {
        return value >= 0 && value < COUNTS[property] ? value : DEFAULTS[property];
    }
    public static String alignment(int value) { return String.valueOf((char) ('a' + valid(0, value))); }
    public static float horizontalPadding(int value) { return 6f * SCALE[valid(1, value)]; }
    public static float verticalPadding(int value) { return 3f * SCALE[valid(1, value)]; }
    public static long color(int property, int value) { return ((long) ARGB[valid(property, value)]) << 32; }
    public static long fontSize(int value) {
        return 0x100000000L | (Float.floatToRawIntBits(8f + valid(3, value)) & 0xffffffffL);
    }
}
