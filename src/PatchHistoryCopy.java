import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class PatchHistoryCopy {
    public static byte[] patchGuiIngame(byte[] input) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        MethodNode render = null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("a") && mn.desc.equals("(FZII)V")) {
                render = mn;
                break;
            }
        }
        if (render == null)
            throw new RuntimeException("Could not find uq.a(FZII)V chat render");
        AbstractInsnNode anchor = null;
        for (AbstractInsnNode n = render.instructions.getFirst(); n != null; n = n.getNext()) {
            if (!(n instanceof VarInsnNode) || n.getOpcode() != Opcodes.ISTORE || ((VarInsnNode) n).var != 17)
                continue;
            AbstractInsnNode prev = prevReal(n);
            AbstractInsnNode prev2 = prevReal(prev);
            boolean offsetLoad = prev instanceof FieldInsnNode
                    && ((FieldInsnNode) prev).owner.equals("ChatScroll")
                    && ((FieldInsnNode) prev).name.equals("offset");
            boolean zero = prev instanceof InsnNode && prev.getOpcode() == Opcodes.ICONST_0;
            boolean afterTranslate = prev2 instanceof MethodInsnNode
                    && ((MethodInsnNode) prev2).name.equals("glTranslatef");
            if ((offsetLoad || zero) && afterTranslate) {
                anchor = prev;
                break;
            }
        }
        if (anchor == null)
            throw new RuntimeException("Could not find the chat loop start in uq.a(FZII)V");
        InsnList il = new InsnList();
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "uq", "e", "Ljava/util/List;"));
        il.add(new VarInsnNode(Opcodes.ALOAD, 8));
        il.add(new VarInsnNode(Opcodes.ILOAD, 15));
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(new VarInsnNode(Opcodes.ILOAD, 7));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, "uq", "g", "Lnet/minecraft/client/Minecraft;"));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatHistoryCopy", "draw", "(Luq;Ljava/util/List;Lsj;IIILnet/minecraft/client/Minecraft;)V", false));
        render.instructions.insertBefore(anchor, il);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  uq (GuiIngame): inserted ChatHistoryCopy.draw into the chat render");
        return cw.toByteArray();
    }
    public static byte[] patchGuiChat(byte[] input) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        MethodNode press = null;
        MethodNode close = null;
        boolean hasRelease = false;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("a") && mn.desc.equals("(III)V"))
                press = mn;
            else if (mn.name.equals("b") && mn.desc.equals("(III)V"))
                hasRelease = true;
            else if (mn.name.equals("h") && mn.desc.equals("()V"))
                close = mn;
        }
        if (press == null)
            throw new RuntimeException("Could not find gc.a(III)V mousePressed");
        if (close == null)
            throw new RuntimeException("Could not find gc.h()V close");
        InsnList p = new InsnList();
        p.add(new VarInsnNode(Opcodes.ILOAD, 1));
        p.add(new VarInsnNode(Opcodes.ILOAD, 2));
        p.add(new VarInsnNode(Opcodes.ILOAD, 3));
        p.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatHistoryCopy", "mousePressed", "(III)V", false));
        press.instructions.insert(p);
        if (!hasRelease) {
            MethodNode rel = new MethodNode(Opcodes.ACC_PROTECTED, "b", "(III)V", null, null);
            InsnList r = new InsnList();
            r.add(new VarInsnNode(Opcodes.ILOAD, 1));
            r.add(new VarInsnNode(Opcodes.ILOAD, 2));
            r.add(new VarInsnNode(Opcodes.ILOAD, 3));
            r.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatHistoryCopy", "mouseReleased", "(III)V", false));
            r.add(new VarInsnNode(Opcodes.ALOAD, 0));
            r.add(new VarInsnNode(Opcodes.ILOAD, 1));
            r.add(new VarInsnNode(Opcodes.ILOAD, 2));
            r.add(new VarInsnNode(Opcodes.ILOAD, 3));
            r.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "da", "b", "(III)V", false));
            r.add(new InsnNode(Opcodes.RETURN));
            rel.instructions = r;
            rel.maxStack = 4;
            rel.maxLocals = 4;
            cn.methods.add(rel);
        }
        close.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatHistoryCopy", "reset", "()V", false));
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  gc (GuiChat): mouse selection hooks wired into press/release/close");
        return cw.toByteArray();
    }
    private static AbstractInsnNode prevReal(AbstractInsnNode n) {
        AbstractInsnNode p = n == null ? null : n.getPrevious();
        while (p != null && (p instanceof org.objectweb.asm.tree.LabelNode
                || p instanceof org.objectweb.asm.tree.LineNumberNode
                || p instanceof org.objectweb.asm.tree.FrameNode))
            p = p.getPrevious();
        return p;
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
        InputStream is = PatchHistoryCopy.class.getClassLoader().getResourceAsStream(path);
        if (is == null)
            throw new RuntimeException(path + " not found on classpath (compile ChatHistoryCopy.java first)");
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
            System.err.println("Usage: PatchHistoryCopy <input.jar> <output.jar>");
            System.err.println("  input : base jar, or an existing chatfix build (to compose)");
            System.exit(1);
        }
        try (JarFile jar = new JarFile(args[0]);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(args[1]))) {
            byte[] uq = patchGuiIngame(readEntry(jar, "uq.class"));
            byte[] gc = patchGuiChat(readEntry(jar, "gc.class"));
            byte[] helper = readResource("ChatHistoryCopy.class");
            boolean helperWritten = false;
            java.util.Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory())
                    continue;
                String name = entry.getName();
                if (name.equals("uq.class"))
                    put(jos, name, uq);
                else if (name.equals("gc.class"))
                    put(jos, name, gc);
                else if (name.equals("ChatHistoryCopy.class")) {
                    put(jos, name, helper);
                    helperWritten = true;
                } else
                    put(jos, name, readEntry(jar, name));
            }
            if (!helperWritten)
                put(jos, "ChatHistoryCopy.class", helper);
        }
        System.out.println("Wrote " + args[1]);
    }
}
