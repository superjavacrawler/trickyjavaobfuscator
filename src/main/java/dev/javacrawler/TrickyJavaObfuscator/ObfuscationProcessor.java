package dev.javacrawler.TrickyJavaObfuscator;

import dev.javacrawler.TrickyJavaObfuscator.utils.BytecodeUtil;
import dev.javacrawler.TrickyJavaObfuscator.utils.wrapper.ClassWrapper;
import dev.javacrawler.TrickyJavaObfuscator.utils.wrapper.FieldWrapper;
import dev.javacrawler.TrickyJavaObfuscator.utils.wrapper.MethodWrapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import dev.javacrawler.TrickyJavaObfuscator.utils.StringUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.*;

public @RequiredArgsConstructor
class ObfuscationProcessor extends Thread {
    private static final Map<String, ClassWrapper> CLASSES = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> FILES = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static final String[] types = {"Ljava/lang/invoke/MutableCallSite;", "Ljava/lang/invoke/CallSite;"};
    private static final String[] hashers = {"MD2", "MD5"};
    private static final int[] length = {16, 24, 32};
    private static String mainClass;
    public final ReentrantLock flagLock = new ReentrantLock();
    private final List<MethodWrapper> methodHandles = new ArrayList<>();

    private @NonNull File file;
    private Boolean flag = null;

    private static MethodWrapper generateMethodHandle(ClassWrapper classWrapper) {
        var methodVisitor = new MethodNode(ACC_PUBLIC | ACC_STATIC, StringUtil.getString(StringUtil.Locale.EN), "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/invoke/MethodHandle;", null, new String[]{"java/lang/Throwable"});
        var wrapper = new MethodWrapper(classWrapper, methodVisitor);

        methodVisitor.visitCode();
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
        methodVisitor.visitVarInsn(ASTORE, 4);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        methodVisitor.visitVarInsn(ASTORE, 5);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false);
        methodVisitor.visitVarInsn(ASTORE, 6);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitInsn(ICONST_2);
        methodVisitor.visitInsn(IAND);
        Label label0 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label0);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ALOAD, 6);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitInsn(ICONST_4);
        methodVisitor.visitInsn(IAND);
        Label label1 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label1);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ALOAD, 6);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitLabel(label1);
        methodVisitor.visitInsn(ACONST_NULL);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitEnd();
        return wrapper;
    }

    private static byte[] encrypt(byte[] bytes, String klass, String method, String hash, int keyLength) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(hash);
            SecretKeySpec secretKey = new SecretKeySpec(Arrays.copyOf(messageDigest.digest((klass + method).getBytes()), keyLength), "AES");
            Cipher encoder = Cipher.getInstance("AES/ECB/PKCS5Padding");
            encoder.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encode(encoder.doFinal(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] classToBytes(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);

        return classWriter.toByteArray();
    }

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean value) {
        synchronized (flagLock) {
            flag = value;
            String strpath = file.getAbsolutePath().replace(".jar", "-out.jar");
            if (value)
                saveOutput(new Path(strpath));
            Main.log("File saved to " + strpath);
            CLASSES.clear();
            FILES.clear();
            flagLock.notifyAll();
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        Thread.currentThread().setName("File Processor Thread");
        if (!loadInput(new Path(file.getAbsolutePath()))) {
            setFlag(false);
            return;
        }
        byte[] manifestData;
        manifestData = FILES.get(FILES.containsKey("bungee.yml") ? "bungee.yml" : "plugin.yml");
        if (manifestData == null) {
            setFlag(false);
            return;
        }
        String main = Arrays.stream(new String(manifestData).split("\n"))
                .filter(s -> s.startsWith("main: "))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Can't find main class"))
                .trim()
                .split(": ")[1]
                .replaceAll("\\.", "/");
        mainClass = main;
        var classWrapper = CLASSES.get(main);
        if (classWrapper == null) {
            setFlag(false);
            return;
        }

        ClassNode classNode = classWrapper.getClassNode();

        String hasher = hashers[RANDOM.nextInt(hashers.length)];
        int keyLength = length[RANDOM.nextInt(length.length)];

        MethodNode generated = generateMethod(hasher, keyLength);
        MethodWrapper methodHandle = generateMethodHandle(new ClassWrapper(classNode));

        methodHandles.add(methodHandle);
        classNode.methods.add(generated);
        classNode.methods.add(methodHandle.getMethodNode());
        Main.log("");
        Main.log(classNode.name+ " | " + "Class loaded!");
        Main.log(classNode.name+ " | " + "Generating handle methods...");

        classNode.methods.stream()
                .filter(method -> "V".equals(Type.getReturnType(method.desc).getDescriptor()))
                .collect(Collectors.toList())  // Создаем копию списка методов
                .forEach(method -> {
                    Main.log(classNode.name+ " | " + "Applying Flow obfuscation...");
                    processNormalFlow(method);
                    Main.log(classNode.name+ " | " + "Applying Invoke Dynamic obfuscation...");
                    processInvokeDynamic(classNode, method);
                    Main.log(classNode.name+ " | " + "Applying String Encryption obfuscation...");
                    processLdcPacker(classNode, method, generated, hasher, keyLength);
                    Main.log(classNode.name+ " | " + "Done!");
                    Main.log("");
                });

        processCrasher(classNode);

        setFlag(true);


    }


    private void processCrasher(ClassNode classNode) {
        classNode.methods.forEach(methodNode -> {
            methodNode.signature = StringUtil.getString(StringUtil.Locale.EN, 100);
            if (!((classNode.access & ACC_INTERFACE) == 0)) return;
            if (classNode.invisibleAnnotations == null)
                classNode.invisibleAnnotations = new ArrayList<>();
            classNode.invisibleAnnotations.add(new AnnotationNode("Protected by https://javacrawler.lol/" + StringUtil.getString(StringUtil.Locale.EN, 100).repeat(20)));
        });
    }

    public InsnList getRandomFlow() {
        InsnList flow = new InsnList();

        int m = RANDOM.nextInt(3);
        if (m == 0) {
            flow.add(new IntInsnNode(BIPUSH, RANDOM.nextInt(60) - 30));
            flow.add(new IntInsnNode(BIPUSH, RANDOM.nextInt(60) - 30));
            int r = RANDOM.nextInt(4);
            if (r == 0) {
                flow.add(new InsnNode(Opcodes.IAND));
            }
            if (r == 1) {
                flow.add(new InsnNode(Opcodes.IADD));
            }
            if (r == 2) {
                flow.add(new InsnNode(POP));
            }
            if (r == 3) {
                flow.add(new InsnNode(Opcodes.ISUB));
            }
            flow.add(new InsnNode(POP));
        }
        if (m == 1) {
            flow.add(new LdcInsnNode(StringUtil.getString(StringUtil.Locale.EN, 255)));
            flow.add(new InsnNode(POP));
        }
        if (m == 2) {
            flow.add(new InsnNode(RANDOM.nextBoolean() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            flow.add(new InsnNode(POP));
        }


        return flow;
    }

    private void processNormalFlow(MethodNode onEnable) {
        onEnable.instructions.insertBefore(onEnable.instructions.getFirst(), getRandomFlow());
        Arrays.stream(onEnable.instructions.toArray()).forEach((insn) -> {
            if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH || insn.getOpcode() == Opcodes.LDC || insn.getOpcode() == Opcodes.ISTORE || insn.getOpcode() == Opcodes.DSTORE) {
                if (RANDOM.nextInt(10) == 1) {
                    onEnable.instructions.insert(insn, getRandomFlow());
                }
            }
        });
    }

    private void processLdcPacker(ClassNode pluginClass, MethodNode onEnable, MethodNode generatedMethod, String hasher, int keyLength) {
        for (AbstractInsnNode node : onEnable.instructions) {
            if (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof String) {
                InsnList insnList = new InsnList();

                insnList.add(new TypeInsnNode(NEW, "java/lang/Throwable"));
                insnList.add(new InsnNode(DUP));
                insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V"));
                insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
                insnList.add(new InsnNode(ICONST_0));
                insnList.add(new InsnNode(AALOAD));
                insnList.add(new VarInsnNode(ASTORE, 1337));

                insnList.add(new TypeInsnNode(NEW, "java/lang/String"));
                insnList.add(new InsnNode(DUP));

                insnList.add(new LdcInsnNode(new String(encrypt(((String) ((LdcInsnNode) node).cst).getBytes(), pluginClass.name.replace("/", "."), onEnable.name, hasher, keyLength))));

                insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B"));
                insnList.add(new VarInsnNode(ALOAD, 1337));
                insnList.add(new MethodInsnNode(INVOKESTATIC, pluginClass.name, generatedMethod.name, "([BLjava/lang/StackTraceElement;)[B"));
                insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"));

                onEnable.instructions.insertBefore(node, insnList);
                onEnable.instructions.remove(node);
            }
        }
    }

    private void processInvokeDynamic(ClassNode mainClass, MethodNode onEnable) {
        Arrays.stream(onEnable.instructions.toArray()).filter(BytecodeUtil::isDefaultInvoke).map(instruction -> (MethodInsnNode) instruction).forEach(
                instruction -> {
/*                    var values = new ArrayList<>(CLASSES.values());
                    var cl = values.get(RANDOM.nextInt(values.size()));
                    while (!cl.isModifiable() || cl.isEnum())
                        cl = values.get(RANDOM.nextInt(values.size()));

 */
                    var cl = CLASSES.get(mainClass.name);
                    var callSite = cl.addField(ACC_STATIC, StringUtil.getString(StringUtil.Locale.EN), types[RANDOM.nextInt(types.length)]);
                    var bootstrapper = generateBootstrapper(instruction, callSite);
                    mainClass.methods.add(bootstrapper);
                    var handle = new Handle(H_INVOKESTATIC, mainClass.name, bootstrapper.name, bootstrapper.desc, false);

                    var desc = instruction.desc;

                    if (instruction.getOpcode() == INVOKEVIRTUAL)
                        desc = "(Ljava/lang/Object;" + desc.substring(1);

                    var invokeDynamic = new InvokeDynamicInsnNode(instruction.getOpcode() == INVOKEVIRTUAL ? "v" : "w", BytecodeUtil.getGenericMethodDescriptor(desc), handle);

                    onEnable.instructions.set(instruction, invokeDynamic);
                }
        );
    }

    private MethodNode generateBootstrapper(MethodInsnNode instruction, FieldWrapper callSite) {
        var methodVisitor = new MethodNode(ACC_STATIC, StringUtil.getString(StringUtil.Locale.EN), "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", null, null);
        var methodHandle = methodHandles.get(RANDOM.nextInt(methodHandles.size()));

        methodVisitor.visitCode();
        Label label0 = new Label();
        Label label1 = new Label();
        Label label2 = new Label();
        methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/NoSuchMethodException");
        methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/IllegalAccessException");
        methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/ClassNotFoundException");
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLdcInsn(instruction.owner.replace('/', '.'));
        methodVisitor.visitLdcInsn(instruction.name);
        methodVisitor.visitLdcInsn(instruction.desc);
        methodVisitor.visitInsn(ICONST_1);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        methodVisitor.visitIntInsn(BIPUSH, 118);
        Label label3 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPNE, label3);
        methodVisitor.visitInsn(ICONST_2);
        Label label4 = new Label();
        methodVisitor.visitJumpInsn(GOTO, label4);
        methodVisitor.visitLabel(label3);
        methodVisitor.visitInsn(ICONST_1);
        methodVisitor.visitLabel(label4);
        methodVisitor.visitInsn(ISHL);
        methodVisitor.visitMethodInsn(INVOKESTATIC, methodHandle.getOwnerName(), methodHandle.getName(), methodHandle.getDescriptor(), false);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ASTORE, 3);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        methodVisitor.visitVarInsn(ASTORE, 3);
        methodVisitor.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "type", "()Ljava/lang/invoke/MethodType;", false);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodType;)V", false);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitFieldInsn(PUTSTATIC, callSite.getOwnerName(), callSite.getName(), callSite.getDescriptor());
        if (!callSite.getDescriptor().equals("Ljava/lang/invoke/MutableCallSite;"))
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/invoke/MutableCallSite");
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);
        methodVisitor.visitFieldInsn(GETSTATIC, callSite.getOwnerName(), callSite.getName(), callSite.getDescriptor());
        methodVisitor.visitLabel(label1);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitVarInsn(ASTORE, 3);
        methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false);
        methodVisitor.visitInsn(ATHROW);
        methodVisitor.visitEnd();

        return methodVisitor;
    }

    private MethodNode generateMethod(String hasher, int keyLength) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, StringUtil.getString(StringUtil.Locale.EN, 12),
                "([BLjava/lang/StackTraceElement;)[B", null, null);

        InsnList insnList = new InsnList();
        LabelNode label1 = new LabelNode();
        insnList.add(label1);
        insnList.add(new LineNumberNode(1, label1));
        insnList.add(new LdcInsnNode(hasher));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "java/security/MessageDigest", "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;"));
        insnList.add(new VarInsnNode(ASTORE, 2));
        LabelNode label2 = new LabelNode();
        insnList.add(label2);
        insnList.add(new LineNumberNode(2, label2));
        insnList.add(new TypeInsnNode(NEW, "javax/crypto/spec/SecretKeySpec"));
        insnList.add(new InsnNode(DUP));
        insnList.add(new VarInsnNode(ALOAD, 2));
        insnList.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
        insnList.add(new InsnNode(DUP));
        insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));
        insnList.add(new VarInsnNode(ALOAD, 1));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
        insnList.add(new VarInsnNode(ALOAD, 1));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/security/MessageDigest", "digest", "([B)[B"));
        insnList.add(new IntInsnNode(BIPUSH, keyLength));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "java/util/Arrays", "copyOf", "([BI)[B"));
        insnList.add(new LdcInsnNode("AES"));
        insnList.add(new MethodInsnNode(INVOKESPECIAL, "javax/crypto/spec/SecretKeySpec", "<init>", "([BLjava/lang/String;)V"));
        insnList.add(new VarInsnNode(ASTORE, 3));
        LabelNode label3 = new LabelNode();
        insnList.add(label3);
        insnList.add(new LineNumberNode(3, label3));
        insnList.add(new LdcInsnNode("AES/ECB/PKCS5Padding"));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;"));
        insnList.add(new VarInsnNode(ASTORE, 4));
        LabelNode label4 = new LabelNode();
        insnList.add(label4);
        insnList.add(new LineNumberNode(4, label4));
        insnList.add(new VarInsnNode(ALOAD, 4));
        insnList.add(new InsnNode(ICONST_2));
        insnList.add(new VarInsnNode(ALOAD, 3));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;)V"));
        LabelNode label5 = new LabelNode();
        insnList.add(label5);
        insnList.add(new LineNumberNode(5, label5));
        insnList.add(new VarInsnNode(ALOAD, 4));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;"));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "([B)[B"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "doFinal", "([B)[B"));
        insnList.add(new InsnNode(ARETURN));

        method.instructions = insnList;
        return method;
    }

    private boolean loadInput(@NonNull Path path) {
        AtomicBoolean result = new AtomicBoolean(true);
        try (ZipFile zipFile = new ZipFile(path.path())) {
            zipFile.entries().asIterator().forEachRemaining(
                    zipEntry -> {
                        try {
                            var is = zipFile.getInputStream(zipEntry);
                            var name = zipEntry.getName();
                            var buffer = is.readAllBytes();
                            if (isClassFile(name, buffer)) {
                                var wrapper = new ClassWrapper(buffer);
                                CLASSES.put(wrapper.getName(), wrapper);
                            } else FILES.put(name, buffer);
                        } catch (Exception e) {
                            result.set(false);
                        }
                    }
            );
        } catch (Exception e) {
            result.set(false);
        }
        return result.get();
    }

    private void saveOutput(@NonNull Path path) {
        try (ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(path.path()))) {
            zipFile.setComment("Obfuscated by Tricky Java Obfuscator (https://javacrawler.lol/)");

            CLASSES.forEach(
                    (name, wrapper) -> {
                        try {
                            zipFile.putNextEntry(new ZipEntry(name + ".class"));
                            if (name.equals(mainClass)) {
                                zipFile.write(wrapper.write());
                            } else {
                                var data = classToBytes(wrapper.getClassNode());
                                zipFile.write(data);
                            }
                            zipFile.closeEntry();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }
            );

            FILES.forEach(
                    (name, buffer) -> {
                        if (name.endsWith("/"))
                            return;

                        try {
                            zipFile.putNextEntry(new ZipEntry(name));
                            zipFile.write(buffer);
                            zipFile.closeEntry();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    boolean isClassFile(@NonNull String entryName, byte @NonNull [] buffer) {
        return (
                (entryName.endsWith(".class") || entryName.endsWith(".class/")) && buffer.length >= 4 && String.format("%02X%02X%02X%02X",
                        buffer[0], buffer[1], buffer[2], buffer[3]
                ).equalsIgnoreCase("cafebabe")
        );
    }

    record Path(@NonNull String path) {
        public File asFile() {
            return new File(path).getAbsoluteFile();
        }
    }


}
