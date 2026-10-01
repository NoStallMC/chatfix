public final class ChatScroll {
    public static int offset = 0;
    private static final int VIEW = 20;
    private static int size = 0;
    private ChatScroll() {
    }
    public static void limit(int lineCount) {
        size = lineCount;
        clamp();
    }
    public static void scroll(int dwheel) {
        if (dwheel > 0)
            ++offset;
        else if (dwheel < 0)
            --offset;
        clamp();
    }
    public static void reset() {
        offset = 0;
    }
    private static void clamp() {
        int max = size - VIEW;
        if (max < 0)
            max = 0;
        if (offset < 0)
            offset = 0;
        else if (offset > max)
            offset = max;
    }
}
