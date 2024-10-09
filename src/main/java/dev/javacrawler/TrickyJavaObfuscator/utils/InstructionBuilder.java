package dev.javacrawler.TrickyJavaObfuscator.utils;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

public class InstructionBuilder {
    private final InsnList instructions = new InsnList();

    private InstructionBuilder() {
    }

    public static InstructionBuilder allocate() {
        return new InstructionBuilder();
    }

    public InstructionBuilder put(AbstractInsnNode node) {
        instructions.add(node);
        return this;
    }

    public InstructionBuilder put(InsnList node) {
        instructions.add(node);
        return this;
    }

    public InstructionBuilder put(int opcode) {
        instructions.add(new InsnNode(opcode));
        return this;
    }

    public InsnList get() {
        return instructions;
    }
}