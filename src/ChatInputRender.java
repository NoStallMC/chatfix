public final class ChatInputRender {
    private ChatInputRender() {
    }
    public static void draw(da screen, sj font, String typed, int width, int height, int blink) {
        if (typed == null)
            typed = "";
        ChatInput.Layout L = ChatInput.layout(font, typed, width, height);
        int bgTop = L.lineY[0] - 2;
        int bgBottom = height - 2;
        screen.a(2, bgTop, width - 2, bgBottom, Integer.MIN_VALUE);
        for (int i = 0; i < L.lines; ++i) {
            if (L.selStartX[i] >= 0)
                screen.a(L.selStartX[i], L.lineY[i] - 1, L.selEndX[i], L.lineY[i] + 8, 0x803A6EE0);
        }
        for (int i = 0; i < L.lines; ++i) {
            if (L.lineDisplay[i].length() > 0)
                screen.b(font, L.lineDisplay[i], ChatInput.X0, L.lineY[i], 0xE0E0E0);
        }
        if (blink / 6 % 2 == 0)
            screen.a(L.caretX, L.caretY - 1, L.caretX + 1, L.caretY + 8, 0xFFE0E0E0);
    }
}
