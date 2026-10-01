import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class PatchInputEditor {
    private static void replaceKeyTyped(MethodNode keyTyped) {
        InsnList il = new InsnList();
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatInput", "key", "(Lgc;CI)V", false));
        il.add(new InsnNode(Opcodes.RETURN));
        clear(keyTyped);
        keyTyped.instructions = il;
        keyTyped.maxStack = 3;
        keyTyped.maxLocals = 3;
    }
    private static void replaceRender(MethodNode render) {
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
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatInputRender", "draw", "(Lda;Lsj;Ljava/lang/String;III)V", false));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new VarInsnNode(Opcodes.FLOAD, 3));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "da", "a", "(IIF)V", false));
        il.add(new InsnNode(Opcodes.RETURN));
        clear(render);
        render.instructions = il;
        render.maxStack = 6;
        render.maxLocals = 4;
    }
    private static void clear(MethodNode mn) {
        mn.tryCatchBlocks = new ArrayList();
        mn.localVariables = null;
        mn.visibleLocalVariableAnnotations = null;
        mn.invisibleLocalVariableAnnotations = null;
        mn.visibleTypeAnnotations = null;
        mn.invisibleTypeAnnotations = null;
    }
    private static boolean prependCall(MethodNode mn, String owner, String name) {
        if (hasCall(mn, owner, name))
            return false;
        InsnList il = new InsnList();
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, "(Lgc;)V", false));
        mn.instructions.insert(il);
        if (mn.maxStack < 1)
            mn.maxStack = 1;
        if (mn.maxLocals < 1)
            mn.maxLocals = 1;
        return true;
    }
    private static boolean hasCall(MethodNode mn, String owner, String name) {
        for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) n;
                if (mi.owner.equals(owner) && mi.name.equals(name))
                    return true;
            }
        }
        return false;
    }
    public static byte[] patchGuiChat(byte[] input) {
        ClassNode cn = new ClassNode();
        new ClassReader(input).accept(cn, 0);
        MethodNode render = null;
        MethodNode keyTyped = null;
        MethodNode init = null;
        MethodNode close = null;
        boolean hasF = false;
        int caps = 0;
        for (MethodNode mn : cn.methods) {
            ArrayList toReplace = new ArrayList();
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n instanceof IntInsnNode) {
                    IntInsnNode ii = (IntInsnNode) n;
                    if (ii.getOpcode() == Opcodes.BIPUSH && ii.operand == 100)
                        toReplace.add(ii);
                }
            }
            for (int i = 0; i < toReplace.size(); ++i) {
                mn.instructions.set((AbstractInsnNode) toReplace.get(i),
                        new LdcInsnNode(Integer.valueOf(Integer.MAX_VALUE)));
                caps++;
            }
            if (mn.name.equals("a") && mn.desc.equals("(IIF)V"))
                render = mn;
            else if (mn.name.equals("a") && mn.desc.equals("(CI)V"))
                keyTyped = mn;
            else if (mn.name.equals("b") && mn.desc.equals("()V"))
                init = mn;
            else if (mn.name.equals("h") && mn.desc.equals("()V"))
                close = mn;
            else if (mn.name.equals("f") && mn.desc.equals("()V"))
                hasF = true;
        }
        if (render == null)
            throw new RuntimeException("Could not find gc.a(IIF)V render method");
        if (keyTyped == null)
            throw new RuntimeException("Could not find gc.a(CI)V keyTyped method");
        if (init == null)
            throw new RuntimeException("Could not find gc.b() init");
        if (close == null)
            throw new RuntimeException("Could not find gc.h() close");
        replaceKeyTyped(keyTyped);
        replaceRender(render);
        prependCall(init, "ChatInput", "init");
        prependCall(close, "ChatInput", "reset");
        MethodNode mousePressed = null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("a") && mn.desc.equals("(III)V")) {
                mousePressed = mn;
                break;
            }
        }
        if (mousePressed != null) {
            InsnList il = new InsnList();
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new VarInsnNode(Opcodes.ILOAD, 1));
            il.add(new VarInsnNode(Opcodes.ILOAD, 2));
            il.add(new VarInsnNode(Opcodes.ILOAD, 3));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatInput", "mousePressed",
                    "(Lgc;III)V", false));
            mousePressed.instructions.insert(il);
        }
        close.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "ChatInput", "resetClicks", "()V", false));
        boolean addedF = false;
        if (!hasF) {
            MethodNode f = new MethodNode(Opcodes.ACC_PUBLIC, "f", "()V", null, null);
            InsnList il = new InsnList();
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "da", "f", "()V", false));
            il.add(new InsnNode(Opcodes.RETURN));
            f.instructions = il;
            f.maxStack = 1;
            f.maxLocals = 1;
            cn.methods.add(f);
            addedF = true;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("  gc (GuiChat): input editor patched"
                + " (caps " + caps + ", keyTyped+render replaced"
                + (addedF ? ", f() added" : ", f() already present") + ")");
        return cw.toByteArray();
    }
    private static byte[] readEntry(JarFile jar, String name) throws IOException {
        JarEntry entry = jar.getJarEntry(name);
        if (entry == null)
            throw new RuntimeException(name + " not found in " + jar.getName());
        try (InputStream is = jar.getInputStream(entry)) {
            return readAll(is);
        }
    }
    private static byte[] readResource(String path) throws IOException {
        InputStream is = PatchInputEditor.class.getClassLoader().getResourceAsStream(path);
        if (is == null)
            throw new RuntimeException(path + " not found on classpath (compile ChatInput.java first)");
        try {
            return readAll(is);
        } finally {
            is.close();
        }
    }
    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1)
            bos.write(buf, 0, n);
        return bos.toByteArray();
    }
    private static void put(JarOutputStream jos, String name, byte[] data) throws IOException {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(data);
        jos.closeEntry();
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PatchInputEditor <base.jar> <output.jar>");
            System.exit(1);
        }
        try (JarFile jar = new JarFile(args[0]);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(args[1]))) {
            put(jos, "gc.class", patchGuiChat(readEntry(jar, "gc.class")));
            put(jos, "ChatInput.class", readResource("ChatInput.class"));
            put(jos, "ChatInput$Layout.class", readResource("ChatInput$Layout.class"));
            put(jos, "ChatInputRender.class", readResource("ChatInputRender.class"));
        }
        System.out.println("Wrote " + args[1]);
    }
}
