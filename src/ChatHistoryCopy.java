import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public final class ChatHistoryCopy {
    private static final int LINE_HEIGHT = 9;
    private static final int X0 = 2;
    private static final int BOTTOM_GAP = 48;
    private static final int HIGHLIGHT = 0x803A6EE0;
    private static boolean dragging;
    private static boolean hasSelection;
    private static boolean copyPending;
    private static int anchorX;
    private static int anchorY;
    private static int anchorLine = -1;
    private static int anchorChar;
    private static int lastTopLine = -1;
    private static int lastTopChar;
    private static int lastBottomLine = -1;
    private static int lastBottomChar;
    private static long lastClickTime;
    private static int clickCount;
    private static int clickX;
    private static int clickY;
    private ChatHistoryCopy() {}
    public static void mousePressed(int x, int y, int button) {
        if (button != 0)
            return;
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 250L && Math.abs(x - clickX) <= 4 && Math.abs(y - clickY) <= 4)
            ++clickCount;
        else
            clickCount = 1;
        lastClickTime = now;
        clickX = x;
        clickY = y;
        dragging = true;
        hasSelection = false;
        copyPending = false;
        anchorX = x;
        anchorY = y;
        anchorLine = -1;
        anchorChar = 0;
        lastTopLine = -1;
        lastBottomLine = -1;
    }
    public static void mouseReleased(int x, int y, int button) {
        if (button != 0 || !dragging)
            return;
        dragging = false;
        hasSelection = true;
        copyPending = true;
    }
    public static void reset() {
        dragging = false;
        hasSelection = false;
        copyPending = false;
        anchorLine = -1;
        anchorChar = 0;
        lastTopLine = -1;
        lastBottomLine = -1;
        clickCount = 0;
        lastClickTime = 0L;
    }
    public static void draw(uq screen, java.util.List lines, sj font, int maxLines,
                            int width, int height, net.minecraft.client.Minecraft mc) {
        if (mc == null || !(mc.r instanceof gc))
            return;
        if (!dragging && !hasSelection)
            return;
        if (lines == null || lines.isEmpty() || maxLines <= 0)
            return;
        int offset = ChatScroll.offset;
        if (dragging) {
            int mouseX = scaleX(mc, width);
            int mouseY = scaleY(mc, height);
            if (clickCount >= 3) {
                int[] h = locate(lines, font, anchorX, anchorY, height, maxLines, offset);
                int[] f = locate(lines, font, mouseX, mouseY, height, maxLines, offset);
                lastTopLine = anchorLine == -1 ? h[0] : anchorLine;
                lastTopChar = anchorLine == -1 ? h[1] : anchorChar;
                lastBottomLine = f[0];
                lastBottomChar = f[1];
                anchorLine = lastTopLine;
                anchorChar = lastTopChar;
                lastTopChar = 0;
                lastBottomChar = f[0] == lastTopLine ? textOf(lines, f[0]).length()
                        : textOf(lines, lastBottomLine).length();
            } else if (clickCount == 2) {
                int[] h = locate(lines, font, anchorX, anchorY, height, maxLines, offset);
                int line = h[0];
                String wtext = textOf(lines, line);
                int[] w = wordBounds(wtext, h[1]);
                if (w == null) {
                    w = new int[]{h[1], h[1]};
                }
                lastTopLine = line;
                lastBottomLine = line;
                lastTopChar = w[0];
                lastBottomChar = w[1];
                anchorLine = line;
                anchorChar = w[0];
            } else {
                if (anchorLine < 0) {
                    int[] a = locate(lines, font, anchorX, anchorY, height, maxLines, offset);
                    anchorLine = a[0];
                    anchorChar = a[1];
                }
                int[] f = locate(lines, font, mouseX, mouseY, height, maxLines, offset);
                int focusLine = f[0];
                int focusChar = f[1];
                if (anchorLine >= focusLine) {
                    lastTopLine = anchorLine;
                    lastTopChar = anchorChar;
                    lastBottomLine = focusLine;
                    lastBottomChar = focusChar;
                } else {
                    lastTopLine = focusLine;
                    lastTopChar = focusChar;
                    lastBottomLine = anchorLine;
                    lastBottomChar = anchorChar;
                }
            }
        }
        int topLine = lastTopLine;
        int topChar = lastTopChar;
        int bottomLine = lastBottomLine;
        int bottomChar = lastBottomChar;
        int size = lines.size();
        for (int i = bottomLine; i <= topLine; ++i) {
            if (i < 0 || i < offset || i >= offset + maxLines || i >= size)
                continue;
            String text = textOf(lines, i);
            int lo;
            int hi;
            if (topLine == bottomLine) {
                lo = Math.min(topChar, bottomChar);
                hi = Math.max(topChar, bottomChar);
            } else if (i == topLine) {
                lo = topChar;
                hi = text.length();
            } else if (i == bottomLine) {
                lo = 0;
                hi = bottomChar;
            } else {
                lo = 0;
                hi = text.length();
            }
            if (lo < 0)
                lo = 0;
            if (hi > text.length())
                hi = text.length();
            if (hi <= lo)
                continue;
            int y = -(i - offset) * LINE_HEIGHT;
            int xStart = X0 + font.a(text.substring(0, lo));
            int xEnd = X0 + font.a(text.substring(0, hi));
            if (xEnd <= xStart)
                xEnd = xStart + 1;
            screen.a(xStart, y - 1, xEnd, y + 8, HIGHLIGHT);
        }
        if (copyPending) {
            copyPending = false;
            if (lastTopLine != lastBottomLine || lastTopChar != lastBottomChar)
                copySelection(lines);
            else
                hasSelection = false;
        }
    }
    public static void copySelection(java.util.List lines) {
        if (lines == null || lines.isEmpty() || lastTopLine < 0)
            return;
        copy(lines, lastTopLine, lastTopChar, lastBottomLine, lastBottomChar);
    }
    private static int[] locate(java.util.List lines, sj font, int mouseX, int mouseY,
                                int height, int maxLines, int offset) {
        int size = lines.size();
        int local = mouseY - (height - BOTTOM_GAP);
        int v = Math.floorDiv(8 - local, LINE_HEIGHT);
        if (v < 0)
            v = 0;
        if (v >= maxLines)
            v = maxLines - 1;
        int line = offset + v;
        if (line < 0)
            line = 0;
        if (line >= size)
            line = size - 1;
        return new int[]{line, charAt(textOf(lines, line), font, mouseX)};
    }
    private static int charAt(String text, sj font, int mouseX) {
        int rel = mouseX - X0;
        if (rel <= 0)
            return 0;
        for (int c = 1; c <= text.length(); ++c) {
            if (font.a(text.substring(0, c)) > rel)
                return c - 1;
        }
        return text.length();
    }
    private static void copy(java.util.List lines, int topLine, int topChar,
                             int bottomLine, int bottomChar) {
        int size = lines.size();
        if (size == 0)
            return;
        if (topLine >= size)
            topLine = size - 1;
        if (bottomLine >= size)
            bottomLine = size - 1;
        if (bottomLine < 0)
            bottomLine = 0;
        if (topLine < bottomLine)
            return;
        StringBuilder sb = new StringBuilder();
        for (int i = topLine; i >= bottomLine; --i) {
            String text = textOf(lines, i);
            int lo;
            int hi;
            if (topLine == bottomLine) {
                lo = Math.min(topChar, bottomChar);
                hi = Math.max(topChar, bottomChar);
            } else if (i == topLine) {
                lo = topChar;
                hi = text.length();
            } else if (i == bottomLine) {
                lo = 0;
                hi = bottomChar;
            } else {
                lo = 0;
                hi = text.length();
            }
            if (lo < 0)
                lo = 0;
            if (hi > text.length())
                hi = text.length();
            if (hi > lo)
                appendAmpersand(sb, text.substring(lo, hi));
            if (i > bottomLine)
                sb.append('\n');
        }
        setClipboard(sb.toString());
    }
    private static int[] wordBounds(String t, int index) {
        int len = t.length();
        int i = index;
        if (i < 0)
            i = 0;
        if (i > len)
            i = len;
        boolean atChar = i < len && isWordChar(t.charAt(i));
        boolean afterChar = i > 0 && isWordChar(t.charAt(i - 1));
        if (!atChar && !afterChar)
            return null;
        int s = i;
        int e = i;
        while (s > 0 && isWordChar(t.charAt(s - 1)))
            --s;
        while (e < len && isWordChar(t.charAt(e)))
            ++e;
        return new int[]{s, e};
    }
    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '\'';
    }
    private static void appendAmpersand(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            sb.append(c == '\u00a7' ? '&' : c);
        }
    }
    private static String textOf(java.util.List lines, int i) {
        Object o = lines.get(i);
        if (o instanceof sw)
            return ((sw) o).a;
        return String.valueOf(o);
    }
    private static int scaleX(net.minecraft.client.Minecraft mc, int width) {
        if (mc.d <= 0)
            return 0;
        return org.lwjgl.input.Mouse.getX() * width / mc.d;
    }
    private static int scaleY(net.minecraft.client.Minecraft mc, int height) {
        if (mc.e <= 0)
            return 0;
        return height - org.lwjgl.input.Mouse.getY() * height / mc.e - 1;
    }
    private static void setClipboard(String text) {
        if (text.length() == 0)
            return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } catch (Throwable ignored) {
        }
    }
}
