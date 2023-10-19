/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.grab.test


import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

private const val EMPTY_FLAGS = 0
private const val CONSTRUCTOR: String = "<init>"
private const val CLASS_CONSTRUCTOR: String = "<clinit>"
private val ENUM_METHODS = setOf(CLASS_CONSTRUCTOR, "valueOf", "values")
private val INTEGER_LIKE_TYPES = setOf<Type>(
    Type.INT_TYPE,
    Type.BYTE_TYPE,
    Type.BOOLEAN_TYPE,
    Type.CHAR_TYPE,
    Type.SHORT_TYPE
)

class MockableJarGenerator(private val returnDefaultValues: Boolean) {

    private val prefixesToSkip = setOf(
        "java.",
        "javax.",
        "org.xml.",
        "org.w3c.",
        "junit.",
        "org.apache.commons.logging"
    )

    @Throws(IOException::class)
    fun createMockableJar(input: File, output: File) {
        JarFile(input).use { androidJar ->
            JarOutputStream(BufferedOutputStream(FileOutputStream(output))).use { outputStream ->
                for (entry in Collections.list(androidJar.entries())) {
                    androidJar.getInputStream(entry).use { inputStream ->
                        if (entry.name.endsWith(".class")) {
                            if (!skipClass(entry.name.replace("/", "."))) {
                                rewriteClass(entry, inputStream, outputStream)
                            }
                        } else if (!skipEntry(entry)) {
                            val zipEntry = ZipEntry(entry.name).apply {
                                comment = entry.comment
                            }
                            outputStream.putNextEntry(zipEntry)
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        }
    }

    private fun skipClass(className: String): Boolean {
        for (prefix in prefixesToSkip) {
            if (className.startsWith(prefix)) {
                return true
            }
        }
        return false
    }

    /**
     * Writes a modified *.class file to the output JAR file.
     */
    @Throws(IOException::class)
    private fun rewriteClass(
        entry: JarEntry,
        inputStream: InputStream,
        outputStream: JarOutputStream
    ) {
        val classNode = ClassNode(Opcodes.ASM5)
        ClassReader(inputStream).apply {
            accept(classNode, EMPTY_FLAGS)
        }
        modifyClass(classNode)
        ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES).also { classWriter ->
            classNode.accept(classWriter)
            outputStream.putNextEntry(ZipEntry(entry.name))
            outputStream.write(classWriter.toByteArray())
        }
    }

    /**
     * Modifies a [ClassNode] to clear final flags and rewrite byte code.
     */
    private fun modifyClass(classNode: ClassNode) {
        // Make the class not final.
        classNode.access = classNode.access and Opcodes.ACC_FINAL.inv()
        classNode.methods.forEach { node ->
            node.access = node.access and (Opcodes.ACC_FINAL or Opcodes.ACC_NATIVE).inv()
            fixMethodBody(node, classNode)
        }
        classNode.fields.forEach { node ->
            if (node.access and Opcodes.ACC_PUBLIC !== 0 &&
                node.access and Opcodes.ACC_STATIC === 0
            ) {
                node.access = node.access and Opcodes.ACC_FINAL.inv()
            }
        }
        classNode.innerClasses.forEach { node ->
            node.access = node.access and Opcodes.ACC_FINAL.inv()
        }
    }

    /**
     * Rewrites the method bytecode to remove the "Stub!" exception.
     */
    private fun fixMethodBody(methodNode: MethodNode, classNode: ClassNode) {
        // Abstract methods don't have bodies to rewrite.
        if (methodNode.access and Opcodes.ACC_ABSTRACT != 0) return
        // Don't break enum classes.
        if (classNode.access and Opcodes.ACC_ENUM != 0 && ENUM_METHODS.contains(methodNode.name)) return
        // Create a body if the method didn't have it, e.g. if it was native.
        if (methodNode.instructions == null) {
            methodNode.instructions = InsnList()
        }
        if (methodNode.name == CONSTRUCTOR) {
            // Keep the call to parent constructor, delete the exception after that.
            var deadCode = false
            for (instruction in methodNode.instructions.toArray()) {
                if (!deadCode) {
                    if (instruction.opcode == Opcodes.INVOKESPECIAL) {
                        methodNode.instructions.insert(instruction, InsnNode(Opcodes.RETURN))
                        // Start removing all following instructions.
                        deadCode = true
                    }
                } else {
                    methodNode.instructions.remove(instruction)
                }
            }
        } else {
            methodNode.instructions.run {
                clear()
                val returnType: Type = Type.getReturnType(methodNode.desc)
                if (returnDefaultValues || methodNode.name == CLASS_CONSTRUCTOR) {
                    when {
                        INTEGER_LIKE_TYPES.contains(returnType) -> add(InsnNode(Opcodes.ICONST_0))
                        returnType == Type.LONG_TYPE -> add(InsnNode(Opcodes.LCONST_0))
                        returnType == Type.FLOAT_TYPE -> add(InsnNode(Opcodes.FCONST_0))
                        returnType == Type.DOUBLE_TYPE -> add(InsnNode(Opcodes.DCONST_0))
                        returnType != Type.VOID_TYPE -> add(InsnNode(Opcodes.ACONST_NULL))
                    }
                    add(InsnNode(returnType.getOpcode(Opcodes.IRETURN)))
                } else {
                    insert(throwExceptionsList(methodNode, classNode))
                }
            }
        }
    }

    private fun skipEntry(entry: JarEntry): Boolean {
        val name = entry.name
        return entry.name.run {
            (endsWith("/")
                    || startsWith("res/")
                    || startsWith("assets/")
                    || this == "AndroidManifest.xml" || name == "resources.arsc")
        }
    }

    private fun throwExceptionsList(methodNode: MethodNode, classNode: ClassNode): InsnList {
        return try {
            val runtimeException: String = Type.getInternalName(RuntimeException::class.java)
            val constructor = RuntimeException::class.java.getConstructor(String::class.java)
            InsnList().also { instructions ->
                instructions.add(TypeInsnNode(Opcodes.NEW, runtimeException))
                instructions.add(InsnNode(Opcodes.DUP))
                val className = classNode.name.replace('/', '.')
                instructions.add(
                    LdcInsnNode(
                        "Method " + methodNode.name + " in " + className
                                + " not mocked. "
                                + "See http://g.co/androidstudio/not-mocked for details."
                    )
                )
                instructions.add(
                    MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        runtimeException,
                        CONSTRUCTOR,
                        Type.getType(constructor).descriptor,
                        false
                    )
                )
                instructions.add(InsnNode(Opcodes.ATHROW))
            }
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
    }
}