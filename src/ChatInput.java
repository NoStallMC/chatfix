import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;

public final class ChatInput {
    public static final String PREFIX = "> ";
    public static final int PREFIX_LEN = PREFIX.length();
    public static final int LINE_HEIGHT = 10;
    public static final int X0 = 4;
    private static final int HISTORY_MAX = 5;
    private static int caret = 0;
    private static int anchor = 0;
    private static final String[] history = new String[HISTORY_MAX];
    private static int historyCount = 0;
    private static int historyPos = -1;
    private static String historyDraft = "";
    private ChatInput() {}
    public static void init(gc screen) {
        int len = screen.a == null ? 0 : screen.a.length();
        caret = len;
        anchor = len;
        historyPos = -1;
        historyDraft = "";
    }
    public static void reset(gc screen) {
        screen.a = "";
        caret = 0;
        anchor = 0;
        historyPos = -1;
        historyDraft = "";
    }
    public static int caret() {
        return caret;
    }
    public static int anchor() {
        return anchor;
    }
    public static boolean hasSelection() {
        return anchor != caret;
    }
    public static int selStart() {
        return anchor < caret ? anchor : caret;
    }
    public static int selEnd() {
        return anchor < caret ? caret : anchor;
    }
    public static void key(gc screen, char c, int key) {
        sync(screen);
        if (key == 1) { // Escape
            screen.b.a((da) null);
            return;
        }
        if (key == 28) { // Enter
            send(screen);
            return;
        }
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (ctrl) {
            if (key == Keyboard.KEY_A) { selectAll(screen); return; }
            if (key == Keyboard.KEY_C) { copy(screen); return; }
            if (key == Keyboard.KEY_X) { cut(screen); return; }
            if (key == Keyboard.KEY_V) { paste(screen); return; }
            if (key == Keyboard.KEY_LEFT) { moveWord(screen, -1, shift); return; }
            if (key == Keyboard.KEY_RIGHT) { moveWord(screen, 1, shift); return; }
            if (key == Keyboard.KEY_BACK) { deleteWordBackward(screen); return; }
            if (key == Keyboard.KEY_DELETE) { deleteWordForward(screen); return; }
        }
        switch (key) {
            case Keyboard.KEY_BACK:
                backspace(screen);
                return;
            case Keyboard.KEY_DELETE:
                deleteForward(screen);
                return;
            case Keyboard.KEY_LEFT:
                moveHorizontal(screen, -1, shift);
                return;
            case Keyboard.KEY_RIGHT:
                moveHorizontal(screen, 1, shift);
                return;
            case Keyboard.KEY_UP:
                up(screen, shift);
                return;
            case Keyboard.KEY_DOWN:
                down(screen, shift);
                return;
            case Keyboard.KEY_HOME:
                moveLineEdge(screen, shift, true);
                return;
            case Keyboard.KEY_END:
                moveLineEdge(screen, shift, false);
                return;
            default:
                break;
        }
        if (c != 0 && fp.a != null && fp.a.indexOf(c) >= 0)
            insert(screen, c);
    }
    public static void insert(gc screen, char c) {
        deleteSelection(screen);
        String t = text(screen);
        caret = clamp(caret, 0, t.length());
        screen.a = t.substring(0, caret) + c + t.substring(caret);
        caret++;
        anchor = caret;
        historyPos = -1;
    }
    public static void backspace(gc screen) {
        if (hasSelection()) {
            deleteSelection(screen);
            return;
        }
        String t = text(screen);
        if (caret <= 0 || t.length() == 0)
            return;
        caret = clamp(caret, 0, t.length());
        if (caret == 0)
            return;
        screen.a = t.substring(0, caret - 1) + t.substring(caret);
        caret--;
        anchor = caret;
        historyPos = -1;
    }
    public static void deleteForward(gc screen) {
        if (hasSelection()) {
            deleteSelection(screen);
            return;
        }
        String t = text(screen);
        caret = clamp(caret, 0, t.length());
        if (caret >= t.length())
            return;
        screen.a = t.substring(0, caret) + t.substring(caret + 1);
        anchor = caret;
        historyPos = -1;
    }
    public static void selectAll(gc screen) {
        anchor = 0;
        caret = text(screen).length();
    }
    public static void moveHorizontal(gc screen, int dir, boolean shift) {
        int len = text(screen).length();
        if (!shift && hasSelection()) {
            caret = dir < 0 ? selStart() : selEnd();
            anchor = caret;
            return;
        }
        caret = clamp(caret + dir, 0, len);
        if (!shift)
            anchor = caret;
    }
    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '\'';
    }
    public static void moveWord(gc screen, int dir, boolean shift) {
        String t = text(screen);
        int len = t.length();
        int p = clamp(caret, 0, len);
        if (dir < 0) {
            while (p > 0 && !isWordChar(t.charAt(p - 1)))
                --p;
            while (p > 0 && isWordChar(t.charAt(p - 1)))
                --p;
        } else {
            while (p < len && !isWordChar(t.charAt(p)))
                ++p;
            while (p < len && isWordChar(t.charAt(p)))
                ++p;
        }
        caret = p;
        if (!shift)
            anchor = p;
    }
    public static void deleteWordBackward(gc screen) {
        if (hasSelection()) {
            deleteSelection(screen);
            return;
        }
        String t = text(screen);
        int len = t.length();
        int p = clamp(caret, 0, len);
        int end = p;
        while (p > 0 && !isWordChar(t.charAt(p - 1)))
            --p;
        while (p > 0 && isWordChar(t.charAt(p - 1)))
            --p;
        if (p == end)
            return;
        screen.a = t.substring(0, p) + t.substring(end);
        caret = p;
        anchor = p;
        historyPos = -1;
    }
    public static void deleteWordForward(gc screen) {
        if (hasSelection()) {
            deleteSelection(screen);
            return;
        }
        String t = text(screen);
        int len = t.length();
        int p = clamp(caret, 0, len);
        int start = p;
        while (p < len && !isWordChar(t.charAt(p)))
            ++p;
        while (p < len && isWordChar(t.charAt(p)))
            ++p;
        if (p == start)
            return;
        screen.a = t.substring(0, start) + t.substring(p);
        caret = start;
        anchor = start;
        historyPos = -1;
    }
    private static int[] wordBounds(String t, int index) {
        int len = t.length();
        int i = clamp(index, 0, len);
        if (i < len && isWordChar(t.charAt(i))) {
            int s = i;
            int e = i;
            while (s > 0 && isWordChar(t.charAt(s - 1)))
                --s;
            while (e < len && isWordChar(t.charAt(e)))
                ++e;
            return new int[]{s, e};
        }
        if (i > 0 && isWordChar(t.charAt(i - 1))) {
            int s = i;
            int e = i;
            while (s > 0 && isWordChar(t.charAt(s - 1)))
                --s;
            while (e < len && isWordChar(t.charAt(e)))
                ++e;
            return new int[]{s, e};
        }
        return null;
    }
    public static void selectWord(gc screen, int caretPos, int anchorPos) {
        String t = text(screen);
        int[] w = wordBounds(t, caretPos);
        if (w == null)
            w = wordBounds(t, anchorPos);
        if (w == null)
            return;
        anchor = w[0];
        caret = w[1];
    }
    public static void selectLine(gc screen) {
        anchor = 0;
        caret = text(screen).length();
    }
    public static void setCaret(int pos) {
        caret = pos < 0 ? 0 : pos;
        anchor = caret;
    }
    public static void setSelection(int a, int b) {
        anchor = a;
        caret = b;
    }
    private static long lastClickTime;
    private static int clickCount;
    private static int clickX;
    private static int clickY;
    public static void mousePressed(gc screen, int x, int y, int button) {
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
        int pos = charIndexAt(screen, x, y);
        if (clickCount >= 3) {
            selectLine(screen);
        } else if (clickCount == 2) {
            int anchorBefore = anchor;
            selectWord(screen, pos, anchorBefore);
        } else {
            caret = pos;
            anchor = pos;
        }
    }
    private static int charIndexAt(gc screen, int x, int y) {
        Layout L = layout(screen.g, screen.a, screen.c, screen.d);
        int line = 0;
        for (int i = 0; i < L.lines; ++i) {
            if (y >= L.lineY[i] - 2 && y <= L.lineY[i] + 8) {
                line = i;
                break;
            }
            if (i == L.lines - 1)
                line = y < L.lineY[0] ? 0 : L.lines - 1;
        }
        String text = L.lineText[line];
        int body = bodyOffset(text);
        int best = 0;
        int bestX = 0;
        for (int p = body; p <= text.length(); ++p) {
            int px = X0 + screen.g.a(text.substring(0, p));
            if (px <= x) {
                best = p - body;
                bestX = px;
            } else {
                break;
            }
        }
        int pos = L.lineStart[line] - PREFIX_LEN + best;
        return clamp(pos, 0, text(screen).length());
    }
    static int bodyOffset(String line) {
        if (line.startsWith("#")) {
            int sp = line.indexOf(' ');
            return sp < 0 ? 0 : sp + 1;
        }
        return PREFIX_LEN;
    }
    public static void resetClicks() {
        clickCount = 0;
        lastClickTime = 0L;
    }
    public static void copy(gc screen) {
        if (!hasSelection())
            return;
        setClipboard(text(screen).substring(selStart(), selEnd()));
    }
    public static void cut(gc screen) {
        if (!hasSelection())
            return;
        copy(screen);
        deleteSelection(screen);
        historyPos = -1;
    }
    public static void paste(gc screen) {
        String clip = getClipboardText();
        if (clip == null || clip.length() == 0)
            return;
        StringBuilder sb = new StringBuilder(clip.length());
        for (int i = 0; i < clip.length(); ++i) {
            char ch = clip.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t')
                sb.append(' ');
            else if (ch >= 32)
                sb.append(ch);
        }
        if (sb.length() == 0)
            return;
        deleteSelection(screen);
        String t = text(screen);
        caret = clamp(caret, 0, t.length());
        screen.a = t.substring(0, caret) + sb.toString() + t.substring(caret);
        caret += sb.length();
        anchor = caret;
        historyPos = -1;
    }
    private static void deleteSelection(gc screen) {
        if (!hasSelection())
            return;
        int a = selStart();
        int b = selEnd();
        String t = text(screen);
        a = clamp(a, 0, t.length());
        b = clamp(b, 0, t.length());
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }
        screen.a = t.substring(0, a) + t.substring(b);
        caret = a;
        anchor = a;
    }
    private static void up(gc screen, boolean shift) {
        String cur = text(screen);

        if (cur.length() == 0) {
            if (historyPos >= 0) {
                if (historyPos > 0) {
                    historyPos--;
                    loadHistory(screen);
                }
                return;
            }
            if (historyCount > 0) {
                historyDraft = "";
                historyPos = historyCount - 1;
                loadHistory(screen);
            }
            return;
        }
        Layout L = layout(screen.g, screen.a, screen.c, screen.d);
        if (!shift && L.caretLine <= 0) {
            if (historyPos >= 0) {
                if (historyPos > 0) {
                    historyPos--;
                    loadHistory(screen);
                }
            } else if (historyCount > 0) {
                historyDraft = cur;
                historyPos = historyCount - 1;
                loadHistory(screen);
            }
            return;
        }
        moveVertical(screen, -1, shift);
    }
    private static void down(gc screen, boolean shift) {
        if (historyPos >= 0) {
            Layout L = layout(screen.g, screen.a, screen.c, screen.d);
            if (!shift && L.caretLine < L.lines - 1) {
                moveVertical(screen, 1, shift);
                return;
            }
            if (historyPos < historyCount - 1) {
                historyPos++;
                loadHistory(screen);
            } else {
                screen.a = historyDraft;
                historyPos = -1;
                caret = screen.a.length();
                anchor = caret;
            }
            return;
        }
        moveVertical(screen, 1, shift);
    }
    private static void loadHistory(gc screen) {
        screen.a = history[historyPos] == null ? "" : history[historyPos];
        caret = screen.a.length();
        anchor = caret;
    }
    private static void moveVertical(gc screen, int dir, boolean shift) {
        Layout L = layout(screen.g, screen.a, screen.c, screen.d);
        int target = L.caretLine + dir;
        if (target < 0 || target >= L.lines)
            return;
        String text = L.lineText[target];
        int body = bodyOffset(text);
        int colX = L.caretX;
        int best = 0;
        for (int p = body; p <= text.length(); ++p) {
            if (X0 + screen.g.a(text.substring(0, p)) <= colX)
                best = p - body;
            else
                break;
        }
        int pos = L.lineStart[target] - PREFIX_LEN + best;
        caret = clamp(pos, 0, text(screen).length());
        if (!shift)
            anchor = caret;
    }
    private static void moveLineEdge(gc screen, boolean shift, boolean home) {
        Layout L = layout(screen.g, screen.a, screen.c, screen.d);
        int line = clamp(L.caretLine, 0, L.lines - 1);
        int dispIdx = home ? L.lineStart[line] : L.lineEnd[line];
        int pos = dispIdx - PREFIX_LEN;
        if (pos < 0)
            pos = 0;
        caret = pos;
        if (!shift)
            anchor = caret;
    }
    private static void send(gc screen) {
        String raw = text(screen);
        String s = raw.trim();
        if (s.length() > 0)
            pushHistory(raw);
        if (s.length() > 0 && !screen.b.b(s))
            screen.b.h.a(s);
        screen.b.a((da) null);
    }
    private static void pushHistory(String msg) {
        if (msg == null || msg.length() == 0)
            return;
        if (historyCount > 0 && history[historyCount - 1] != null && history[historyCount - 1].equals(msg)) {
            historyPos = -1;
            return;
        }
        if (historyCount < HISTORY_MAX) {
            history[historyCount++] = msg;
        } else {
            for (int i = 1; i < HISTORY_MAX; ++i)
                history[i - 1] = history[i];
            history[HISTORY_MAX - 1] = msg;
        }
        historyPos = -1;
    }
    public static final class Layout {
        public final int lines;
        public final int[] lineStart;
        public final int[] lineEnd;
        public final int[] lineY;
        public final String[] lineText;
        public final String[] lineDisplay;
        public final int caretLine;
        public final int caretX;
        public final int caretY;
        public final int[] selStartX;
        public final int[] selEndX;
        Layout(int lines, int[] lineStart, int[] lineEnd, int[] lineY, String[] lineText,
               String[] lineDisplay, int caretLine, int caretX, int caretY,
               int[] selStartX, int[] selEndX) {
            this.lines = lines;
            this.lineStart = lineStart;
            this.lineEnd = lineEnd;
            this.lineY = lineY;
            this.lineText = lineText;
            this.lineDisplay = lineDisplay;
            this.caretLine = caretLine;
            this.caretX = caretX;
            this.caretY = caretY;
            this.selStartX = selStartX;
            this.selEndX = selEndX;
        }
    }
    public static Layout layout(sj font, String typed, int width, int height) {
        if (typed == null)
            typed = "";
        int maxWidth = width - 8 - font.a("_");
        int[] starts = chunkStarts(typed);
        int chunks = starts.length;
        List lineTexts = new ArrayList();
        List lineDisplays = new ArrayList();
        List lineMarkers = new ArrayList();
        List lineStarts = new ArrayList();
        List lineEnds = new ArrayList();
        String active = "";
        for (int c = 0; c < chunks; ++c) {
            String marker = chunks > 1 ? "#" + (c + 1) + " " : PREFIX;
            int from = starts[c];
            int to = c + 1 < chunks ? starts[c + 1] : typed.length();
            int len = to - from;
            int pos = 0;
            boolean first = true;
            while (first || pos < len) {
                int leadLen = first ? marker.length() : 0;
                int avail = maxWidth - font.a(active) - font.a(leadLen == 0 ? "" : marker);
                int take = len - pos;
                while (take > 0 && font.a(typed.substring(from + pos, from + pos + take)) > avail)
                    --take;
                if (take <= 0)
                    take = Math.min(1, len - pos);
                if (take > 0 && take < len - pos && typed.charAt(from + pos + take - 1) == '&') {
                    --take;
                    if (take <= 0)
                        take = 1;
                }
                String body = take <= 0 ? "" : typed.substring(from + pos, from + pos + take);
                String prefix = (first ? marker : "") + active;
                lineTexts.add(prefix + body);
                lineDisplays.add(prefix + colorized(body));
                lineMarkers.add(prefix);
                lineStarts.add(Integer.valueOf(from + pos + PREFIX_LEN));
                lineEnds.add(Integer.valueOf(from + pos + take + PREFIX_LEN));
                active = activeColor(active, body);
                pos += take;
                first = false;
            }
        }
        int n = lineTexts.size();
        String[] lineText = new String[n];
        String[] lineDisplay = new String[n];
        String[] lineMarker = new String[n];
        int[] lineStart = new int[n];
        int[] lineEnd = new int[n];
        int[] lineY = new int[n];
        int baseTop = height - 12;
        for (int i = 0; i < n; ++i) {
            lineText[i] = (String) lineTexts.get(i);
            lineDisplay[i] = (String) lineDisplays.get(i);
            lineMarker[i] = (String) lineMarkers.get(i);
            lineStart[i] = ((Integer) lineStarts.get(i)).intValue();
            lineEnd[i] = ((Integer) lineEnds.get(i)).intValue();
            lineY[i] = baseTop - (n - 1 - i) * LINE_HEIGHT;
        }
        int c = clamp(caret, 0, typed.length());
        int caretLine = lineFor(lineStart, lineEnd, c + PREFIX_LEN);
        int dispCaret = clamp(c + PREFIX_LEN, lineStart[caretLine], lineEnd[caretLine]);
        String caretPre = lineMarker[caretLine];
        String caretBody = bodySlice(lineText[caretLine], caretPre, dispCaret - lineStart[caretLine]);
        int caretX = X0 + font.a(caretPre) + font.a(caretBody);
        int caretY = lineY[caretLine];
        int[] selStartX = new int[n];
        int[] selEndX = new int[n];
        for (int i = 0; i < n; ++i) {
            selStartX[i] = -1;
            selEndX[i] = -1;
        }
        if (anchor != caret) {
            int sa = selStart() + PREFIX_LEN;
            int sb = selEnd() + PREFIX_LEN;
            for (int i = 0; i < n; ++i) {
                int a = Math.max(lineStart[i], sa);
                int b = Math.min(lineEnd[i], sb);
                if (a < b) {
                    String pre = lineMarker[i];
                    selStartX[i] = X0 + font.a(pre) + font.a(bodySlice(lineText[i], pre, a - lineStart[i]));
                    selEndX[i] = X0 + font.a(pre) + font.a(bodySlice(lineText[i], pre, b - lineStart[i]));
                }
            }
        }
        return new Layout(n, lineStart, lineEnd, lineY, lineText, lineDisplay,
                caretLine, caretX, caretY, selStartX, selEndX);
    }
    private static String bodySlice(String lineText, String marker, int len) {
        String body = lineText.substring(marker.length());
        if (len <= 0)
            return "";
        if (len >= body.length())
            return body;
        return body.substring(0, len);
    }

    private static String activeColor(String start, String text) {
        String color = start;
        for (int i = 0; i < text.length() - 1; ++i) {
            if (text.charAt(i) == '&' && isColorCode(text.charAt(i + 1))) {
                color = "\u00a7" + text.charAt(i + 1);
                ++i;
            }
        }
        return color;
    }
    private static String colorized(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length() && isColorCode(s.charAt(i + 1))) {
                sb.append('\u00a7').append(s.charAt(i + 1));
                sb.append('&').append(s.charAt(i + 1));
                ++i;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                || (c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O') || c == 'r' || c == 'R';
    }
    private static int lineFor(int[] lineStart, int[] lineEnd, int dispPos) {
        int line = 0;
        for (int i = 0; i < lineStart.length; ++i) {
            if (dispPos >= lineStart[i] && dispPos <= lineEnd[i])
                line = i;
        }
        return line;
    }
    public static int[] chunkStarts(String typed) {
        List parts = ChatSplit.chunks(typed);
        int[] out = new int[parts.size()];
        int pos = 0;
        for (int i = 0; i < parts.size(); ++i) {
            out[i] = pos;
            pos += ((String) parts.get(i)).length();
        }
        return out;
    }
    private static void sync(gc screen) {
        int len = text(screen).length();
        caret = clamp(caret, 0, len);
        anchor = clamp(anchor, 0, len);
    }
    private static String text(gc screen) {
        return screen.a == null ? "" : screen.a;
    }
    private static int clamp(int v, int lo, int hi) {
        if (v < lo)
            return lo;
        if (v > hi)
            return hi;
        return v;
    }
    private static void setClipboard(String s) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(s), null);
        } catch (Throwable ignored) {
        }
    }
    private static String getClipboardText() {
        try {
            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
