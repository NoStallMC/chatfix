import java.util.ArrayList;
import java.util.List;

public final class WrapChat {
    private static final int LINE_HEIGHT = 10;
    private static final int CHUNK = 100;
    private static final String PREFIX = "> ";
    private WrapChat() {
    }
    public static void draw(da screen, sj font, String typed, int width, int height, int blink) {
        int maxWidth = width - 8 - font.a("_");
        String disp = PREFIX + typed;
        int prefixLen = PREFIX.length();
        int[] ends = chunkEnds(typed);
        List ranges = wrapRanges(disp, maxWidth, font);
        int n = ranges.size();
        int baseTop = height - 12;
        int bgTop = baseTop - 2 - (n - 1) * LINE_HEIGHT;
        int bgBottom = height - 2;
        screen.a(2, bgTop, width - 2, bgBottom, Integer.MIN_VALUE);
        for (int li = 0; li < n; ++li) {
            int[] range = (int[]) ranges.get(li);
            int y = baseTop - (n - 1 - li) * LINE_HEIGHT;
            int x = 4;
            int i = range[0];
            while (i < range[1]) {
                int typedPos = i - prefixLen;
                int chunk;
                int segmentEnd;
                if (typedPos < 0) {
                    chunk = 0;
                    segmentEnd = Math.min(range[1], prefixLen);
                } else {
                    chunk = chunkIndex(typedPos, ends);
                    segmentEnd = Math.min(range[1], ends[chunk] + prefixLen);
                }
                String segment = disp.substring(i, segmentEnd);
                if (segment.length() > 0) {
                    screen.b(font, chunkColor(chunk) + segment, x, y, 0xE0E0E0);
                    x += font.a(segment);
                }
                i = segmentEnd;
            }
        }
        if (blink / 6 % 2 == 0) {
            int[] last = (int[]) ranges.get(n - 1);
            int typedPos = last[1] - prefixLen - 1;
            int chunk = typedPos < 0 ? 0 : chunkIndex(typedPos, ends);
            int lineWidth = font.a(disp.substring(last[0], last[1]));
            screen.b(font, chunkColor(chunk) + "_", 4 + lineWidth, baseTop, 0xE0E0E0);
        }
    }
    private static int[] chunkEnds(String s) {
        List ends = new ArrayList();
        int start = 0;
        while (s.length() - start > CHUNK) {
            int cut = s.lastIndexOf(' ', start + CHUNK - 1);
            if (cut <= start)
                cut = start + CHUNK;
            ends.add(Integer.valueOf(cut));
            int next = cut;
            while (next < s.length() && s.charAt(next) == ' ')
                ++next;
            start = next;
        }
        ends.add(Integer.valueOf(s.length()));
        return toIntArray(ends);
    }
    private static int chunkIndex(int pos, int[] ends) {
        for (int k = 0; k < ends.length; ++k) {
            if (pos < ends[k])
                return k;
        }
        return ends.length - 1;
    }
    private static List wrapRanges(String s, int maxWidth, sj font) {
        List ranges = new ArrayList();
        int pos = 0;
        while (pos < s.length()) {
            int cut = s.length();
            for (int i = s.length(); i > pos; --i) {
                if (font.a(s.substring(pos, i)) <= maxWidth) {
                    cut = i;
                    break;
                }
            }
            if (cut <= pos)
                cut = pos + 1;
            ranges.add(new int[]{pos, cut});
            pos = cut;
        }
        if (ranges.isEmpty())
            ranges.add(new int[]{0, 0});
        return ranges;
    }
    private static String chunkColor(int i) {
        return (i % 2 == 1) ? "\u00a7" + "7" : "";
    }
    private static int[] toIntArray(List list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; ++i)
            out[i] = ((Integer) list.get(i)).intValue();
        return out;
    }
}
