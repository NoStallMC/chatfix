import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class PatchChat {
    private static final int MAX_CHUNK = 100;
    private static AbstractInsnNode previousReal(AbstractInsnNode n) {
        AbstractInsnNode p = n.getPrevious();
        while (p != null && (p instanceof LabelNode || p instanceof LineNumberNode || p instanceof FrameNode))
            p = p.getPrevious();
        return p;
    }
    private static AbstractInsnNode nextReal(AbstractInsnNode n) {
        AbstractInsnNode p = n.getNext();
        while (p != null && (p instanceof LabelNode || p instanceof LineNumberNode || p instanceof FrameNode))
            p = p.getNext();
        return p;
    }
    private static byte[] patchBipush(byte[] input, int oldValue, int newValue, String label) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        int hits = 0;
        for (MethodNode mn : cn.methods) {
            List<AbstractInsnNode> matches = new ArrayList<>();
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (!(n instanceof IntInsnNode))
                    continue;
                IntInsnNode ii = (IntInsnNode) n;
                if (ii.getOpcode() == Opcodes.BIPUSH && ii.operand == oldValue)
                    matches.add(ii);
            }
            for (AbstractInsnNode m : matches) {
                mn.instructions.set(m, new LdcInsnNode(Integer.valueOf(newValue)));
                hits++;
            }
        }
        if (hits == 0)
            throw new RuntimeException("Could not find the " + oldValue + " limit in " + label);
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        System.out.println("  " + label + ": replaced " + hits + " limit(s) " + oldValue + " -> " + newValue);
        return cw.toByteArray();
    }

    /** gc (GuiChat): drop the 100-char caps and swap the render for WrapChat.draw. */
    private static byte[] patchGuiChat(byte[] input) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        int caps = 0;
        MethodNode render = null;
        MethodNode keyTyped = null;
        MethodNode initGui = null;
        MethodNode onClose = null;
        for (MethodNode mn : cn.methods) {
            List<AbstractInsnNode> matches = new ArrayList<>();
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n instanceof IntInsnNode) {
                    IntInsnNode ii = (IntInsnNode) n;
                    if (ii.getOpcode() == Opcodes.BIPUSH && ii.operand == 100)
                        matches.add(ii);
                }
            }
            for (AbstractInsnNode m : matches) {
                mn.instructions.set(m, new LdcInsnNode(Integer.valueOf(Integer.MAX_VALUE)));
                caps++;
            }
            if (mn.name.equals("a") && mn.desc.equals("(IIF)V"))
                render = mn;
            else if (mn.name.equals("a") && mn.desc.equals("(CI)V"))
                keyTyped = mn;
            else if (mn.name.equals("b") && mn.desc.equals("()V"))
                initGui = mn;
            else if (mn.name.equals("h") && mn.desc.equals("()V"))
                onClose = mn;
        }
        if (caps == 0)
            throw new RuntimeException("Could not find the 100 char cap in gc (GuiChat)");
        if (render == null)
            throw new RuntimeException("Could not find gc.a(IIF)V render method");
        if (keyTyped == null)
            throw new RuntimeException("Could not find gc.a(CI)V keyTyped method");
        if (initGui == null)
            throw new RuntimeException("Could not find gc.b() init");
        if (onClose == null)
            throw new RuntimeException("Could not find gc.h() close");
        InsnList paste = new InsnList();
        paste.add(new VarInsnNode(Opcodes.ALOAD, 0));
        paste.add(new VarInsnNode(Opcodes.ILOAD, 2));
        paste.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ClipboardHelper", "paste", "(Lgc;I)V", false));
        keyTyped.instructions.insert(paste);
        onClose.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatScroll", "reset", "()V", false));
        // gc.f() is the per-mouse-event path (da.e -> f), so the wheel is handled here.
        MethodNode mouseUpdate = new MethodNode(Opcodes.ACC_PUBLIC, "f", "()V", null, null);
        InsnList mu = new InsnList();
        mu.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/lwjgl/input/Mouse", "getEventDWheel", "()I", false));
        mu.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatScroll", "scroll", "(I)V", false));
        mu.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mu.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "da", "f", "()V", false));
        mu.add(new InsnNode(Opcodes.RETURN));
        mouseUpdate.instructions = mu;
        mouseUpdate.maxStack = 1;
        mouseUpdate.maxLocals = 1;
        cn.methods.add(mouseUpdate);
        InsnList il = new InsnList();
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "da", "g", "Lsj;"));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "gc", "a", "Ljava/lang/String;"));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "da", "c", "I"));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "da", "d", "I"));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "gc", "i", "I"));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "WrapChat", "draw", "(Lda;Lsj;Ljava/lang/String;III)V", false));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new VarInsnNode(Opcodes.FLOAD, 3));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "da", "a", "(IIF)V", false));
        il.add(new InsnNode(Opcodes.RETURN));
        render.instructions = il;
        render.maxStack = 6;
        render.maxLocals = 4;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  gc (GuiChat): removed " + caps + " cap(s) + wrapped render");
        return cw.toByteArray();
    }
    private static byte[] patchChatSend(byte[] input, String label) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        MethodNode target = null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("a") && mn.desc.equals("(Ljava/lang/String;)V")) {
                target = mn;
                break;
            }
        }
        if (target == null)
            throw new RuntimeException("Could not find tk.a(String) in " + label);
        InsnList il = new InsnList();
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "tk", "bN", "Lnb;"));
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatSplit", "send", "(Lnb;Ljava/lang/String;)V", false));
        il.add(new InsnNode(Opcodes.RETURN));
        target.instructions = il;
        target.maxStack = 2;
        target.maxLocals = 2;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  " + label + ": chat send routed through ChatSplit");
        return cw.toByteArray();
    }

    private static byte[] patchMinecraftWheel(byte[] input) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        boolean done = false;
        outer:
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (!(n instanceof MethodInsnNode))
                    continue;
                MethodInsnNode mi = (MethodInsnNode) n;
                if (!(mi.owner.equals("ix") && mi.name.equals("b") && mi.desc.equals("(I)V")))
                    continue;
                AbstractInsnNode loadN = previousReal(mi);
                AbstractInsnNode fieldC = previousReal(loadN);
                AbstractInsnNode fieldH = previousReal(fieldC);
                AbstractInsnNode self = previousReal(fieldH);
                if (!(loadN instanceof VarInsnNode) || loadN.getOpcode() != Opcodes.ILOAD)
                    continue;
                if (!(fieldC instanceof FieldInsnNode) || !((FieldInsnNode) fieldC).name.equals("c"))
                    continue;
                if (!(fieldH instanceof FieldInsnNode) || !((FieldInsnNode) fieldH).name.equals("h"))
                    continue;
                if (!(self instanceof VarInsnNode) || self.getOpcode() != Opcodes.ALOAD)
                    continue;
                int local = ((VarInsnNode) loadN).var;
                LabelNode elseLabel = new LabelNode();
                LabelNode endLabel = new LabelNode();
                InsnList branch = new InsnList();
                branch.add(new VarInsnNode(Opcodes.ALOAD, 0));
                branch.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", "r", "Lda;"));
                branch.add(new TypeInsnNode(Opcodes.INSTANCEOF, "gc"));
                branch.add(new JumpInsnNode(Opcodes.IFEQ, elseLabel));
                branch.add(new VarInsnNode(Opcodes.ILOAD, local));
                branch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatScroll", "scroll", "(I)V", false));
                branch.add(new JumpInsnNode(Opcodes.GOTO, endLabel));
                branch.add(elseLabel);
                mn.instructions.insertBefore(self, branch);
                mn.instructions.insert(mi, endLabel);
                done = true;
                break outer;
            }
        }
        if (!done)
            throw new RuntimeException("Could not find the mouse wheel hotbar scroll in Minecraft.k()");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  Minecraft: mouse wheel routed to ChatScroll when gc is open");
        return cw.toByteArray();
    }
    /** uq.a(FZII)V: draw chat lines from ChatScroll.offset instead of the newest 20. */
    private static byte[] patchChatScroll(byte[] input) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        MethodNode target = null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("a") && mn.desc.equals("(FZII)V")) {
                target = mn;
                break;
            }
        }
        if (target == null)
            throw new RuntimeException("Could not find uq.a(FZII)V chat render");
        InsnList limit = new InsnList();
        limit.add(new VarInsnNode(Opcodes.ALOAD, 0));
        limit.add(new FieldInsnNode(Opcodes.GETFIELD, "uq", "e", "Ljava/util/List;"));
        limit.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true));
        limit.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatScroll", "limit", "(I)V", false));
        target.instructions.insert(limit);
        int patched = 0;
        for (AbstractInsnNode n = target.instructions.getFirst(); n != null; n = n.getNext()) {
            if (!(n instanceof VarInsnNode) || n.getOpcode() != Opcodes.ISTORE || ((VarInsnNode) n).var != 17)
                continue;
            AbstractInsnNode prev = previousReal(n);
            AbstractInsnNode prev2 = previousReal(prev);
            if (prev instanceof InsnNode && prev.getOpcode() == Opcodes.ICONST_0
                    && prev2 instanceof MethodInsnNode && ((MethodInsnNode) prev2).name.equals("glTranslatef")) {
                target.instructions.set(prev, new FieldInsnNode(Opcodes.GETSTATIC, "ChatScroll", "offset", "I"));
                patched++;
                break;
            }
        }
        for (AbstractInsnNode n = target.instructions.getFirst(); n != null; n = n.getNext()) {
            if (!(n instanceof VarInsnNode) || n.getOpcode() != Opcodes.ILOAD || ((VarInsnNode) n).var != 17)
                continue;
            AbstractInsnNode n1 = nextReal(n);
            AbstractInsnNode n2 = nextReal(n1);
            if (n1 instanceof VarInsnNode && n1.getOpcode() == Opcodes.ILOAD && ((VarInsnNode) n1).var == 15
                    && n2 instanceof JumpInsnNode && n2.getOpcode() == Opcodes.IF_ICMPGE) {
                InsnList add = new InsnList();
                add.add(new FieldInsnNode(Opcodes.GETSTATIC, "ChatScroll", "offset", "I"));
                add.add(new InsnNode(Opcodes.IADD));
                target.instructions.insert(n1, add);
                patched++;
                continue;
            }
            if (n1 != null && n1.getOpcode() == Opcodes.INEG
                    && n2 instanceof IntInsnNode && ((IntInsnNode) n2).operand == 9
                    && nextReal(n2) != null && nextReal(n2).getOpcode() == Opcodes.IMUL
                    && nextReal(nextReal(n2)) instanceof VarInsnNode
                    && ((VarInsnNode) nextReal(nextReal(n2))).var == 22) {
                InsnList sub = new InsnList();
                sub.add(new FieldInsnNode(Opcodes.GETSTATIC, "ChatScroll", "offset", "I"));
                sub.add(new InsnNode(Opcodes.ISUB));
                target.instructions.insert(n, sub);
                patched++;
                continue;
            }
        }
        if (patched != 3)
            throw new RuntimeException("uq chat scroll: patched " + patched + "/3 sites");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  uq (GuiIngame): chat scrollback wired into the render loop");
        return cw.toByteArray();
    }

    private static byte[] patchChatHistory(byte[] input) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        boolean done = false;
        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals("a") || !mn.desc.equals("(Ljava/lang/String;)V"))
                continue;
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n instanceof IntInsnNode) {
                    IntInsnNode ii = (IntInsnNode) n;
                    if (ii.getOpcode() == Opcodes.BIPUSH && ii.operand == 50) {
                        mn.instructions.set(ii, new IntInsnNode(Opcodes.SIPUSH, 500));
                        done = true;
                    }
                }
            }
        }
        if (!done)
            throw new RuntimeException("Could not find the 50-line chat history cap in uq.a(String)");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  uq (GuiIngame): chat history 50 -> 500 lines");
        return cw.toByteArray();
    }
    private static byte[] readEntry(JarFile jar, String name) throws IOException {
        JarEntry entry = jar.getJarEntry(name);
        if (entry == null)
            throw new RuntimeException(name + " not found in " + jar.getName());
        try (InputStream is = jar.getInputStream(entry)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1)
                bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }
    private static byte[] readResource(String path) throws IOException {
        InputStream is = PatchChat.class.getClassLoader().getResourceAsStream(path);
        if (is == null)
            throw new RuntimeException(path + " not found on classpath (compile WrapChat.java first)");
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1)
                bos.write(buf, 0, n);
            return bos.toByteArray();
        } finally {
            is.close();
        }
    }
    private static void put(JarOutputStream jos, String name, byte[] data) throws IOException {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(data);
        jos.closeEntry();
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PatchChat <base.jar> <output-jarmod.jar>");
            System.exit(1);
        }
        try (JarFile jar = new JarFile(args[0]);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(args[1]))) {
            byte[] gc = patchGuiChat(readEntry(jar, "gc.class"));
            gc = PatchInputEditor.patchGuiChat(gc);
            gc = PatchHistoryCopy.patchGuiChat(gc);
            put(jos, "gc.class", gc);
            byte[] uq = patchChatHistory(patchChatScroll(readEntry(jar, "uq.class")));
            uq = PatchHistoryCopy.patchGuiIngame(uq);
            put(jos, "uq.class", uq);
            put(jos, "pe.class", patchBipush(readEntry(jar, "pe.class"), 119, 32767, "pe (Packet3Chat)"));
            put(jos, "tk.class", patchChatSend(readEntry(jar, "tk.class"), "tk (sendChatMessage)"));
            put(jos, "WrapChat.class", readResource("WrapChat.class"));
            put(jos, "ClipboardHelper.class", readResource("ClipboardHelper.class"));
            put(jos, "ChatScroll.class", readResource("ChatScroll.class"));
            put(jos, "ChatInput.class", readResource("ChatInput.class"));
            put(jos, "ChatInput$Layout.class", readResource("ChatInput$Layout.class"));
            put(jos, "ChatInputRender.class", readResource("ChatInputRender.class"));
            put(jos, "ChatHistoryCopy.class", readResource("ChatHistoryCopy.class"));
            put(jos, "ChatSplit.class", readResource("ChatSplit.class"));
        }
        System.out.println("Wrote " + args[1]);
    }
}
