import java.util.ArrayList;
import java.util.List;

public final class ChatSplit {
    private static final int CHUNK = 100;
    private ChatSplit() {
    }
    public static void send(nb handler, String msg) {
        if (msg == null || msg.length() == 0) {
            handler.b(new pe(msg == null ? "" : msg));
            return;
        }
        if (msg.charAt(0) == '/') {
            handler.b(new pe(msg));
            return;
        }
        List parts = chunks(msg);
        for (int i = 0; i < parts.size(); ++i)
            handler.b(new pe((String) parts.get(i)));
    }
    public static List chunks(String msg) {
        List out = new ArrayList();
        String active = "";
        int start = 0;
        while (start < msg.length()) {
            int remaining = msg.length() - start;
            int budget = CHUNK - active.length();
            int take;
            if (remaining <= budget) {
                take = remaining;
            } else {
                take = budget;
                int cut = msg.lastIndexOf(' ', start + budget - 1);
                if (cut > start)
                    take = cut - start;
                if (take <= 0)
                    take = budget;
                if (start + take < msg.length() && msg.charAt(start + take - 1) == '&')
                    --take;
                if (take <= 0)
                    take = budget;
            }
            String body = msg.substring(start, start + take);
            out.add(active + body);
            active = activeColor(active, body);
            start += take;
            while (start < msg.length() && msg.charAt(start) == ' ')
                ++start;
            if (remaining <= budget)
                break;
        }
        if (out.isEmpty())
            out.add("");
        return out;
    }
    public static String activeColor(String start, String body) {
        String color = start;
        for (int i = 0; i < body.length() - 1; ++i) {
            if (body.charAt(i) == '&' && isColorCode(body.charAt(i + 1))) {
                color = "&" + body.charAt(i + 1);
                ++i;
            }
        }
        return color;
    }
    public static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                || (c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O') || c == 'r' || c == 'R';
    }
}
