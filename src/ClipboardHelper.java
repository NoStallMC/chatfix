import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import org.lwjgl.input.Keyboard;

public final class ClipboardHelper {
    private ClipboardHelper() {
    }
    public static void paste(gc screen, int key) {
        if (key != Keyboard.KEY_V)
            return;
        if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
            return;
        String clip = getClipboardText();
        if (clip == null || clip.length() == 0)
            return;
        StringBuilder sb = new StringBuilder(clip.length());
        for (int i = 0; i < clip.length(); ++i) {
            char c = clip.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c >= 32) {
                sb.append(c);
            }
        }
        if (sb.length() > 0)
            screen.a = screen.a + sb.toString();
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
