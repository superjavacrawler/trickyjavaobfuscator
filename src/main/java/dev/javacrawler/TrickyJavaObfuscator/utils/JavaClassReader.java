package dev.javacrawler.TrickyJavaObfuscator.utils;

import org.objectweb.asm.ClassReader;


public final class JavaClassReader extends ClassReader {
    public JavaClassReader(byte[] classFile) {
        super(classFile);
    }
}
