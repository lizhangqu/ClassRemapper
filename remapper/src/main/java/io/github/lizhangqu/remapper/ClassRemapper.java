package io.github.lizhangqu.remapper;

import javassist.ClassPool;
import javassist.CtClass;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ClassRemapper {

    private final File srcFile;
    private final File outFile;
    private final String originalPrefix;
    private final String newPrefix;
    private final String originalAndroidPackageName;
    private final String newAndroidPackageName;

    private final Map<String, String> mappedName = new HashMap<>();

    public ClassRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix) {
        this(srcFile, outFile, originalPrefix, newPrefix, null, null);
    }

    public ClassRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix, String originalAndroidPackageName, String newAndroidPackageName) {
        this.srcFile = srcFile;
        this.outFile = outFile;
        this.originalPrefix = originalPrefix;
        this.newPrefix = newPrefix;
        this.originalAndroidPackageName = originalAndroidPackageName;
        this.newAndroidPackageName = newAndroidPackageName;
        try {
            initMappedName();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initMappedName() throws IOException {
        ZipFile zipFile = new ZipFile(srcFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.getName().endsWith(".class")) {
                String className = zipEntry.getName().replace("/", ".").replace(".class", "");
                mappedName.put(className, className.replace(originalPrefix, newPrefix));
            }
        }
        zipFile.close();

        if (originalAndroidPackageName != null && newAndroidPackageName != null) {
            String[] generatedClasses = new String[]{"BuildConfig", "R", "R$anim", "R$attr", "R$bool", "R$color", "R$dimen", "R$drawable", "R$id", "R$integer", "R$layout", "R$mipmap", "R$string", "R$style", "R$styleable"};
            for (String generatedClass : generatedClasses) {
                mappedName.put(originalAndroidPackageName + "." + generatedClass, newAndroidPackageName + "." + generatedClass);
            }
        }
    }


    public boolean remap() {
        FileInputStream fis = null;
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.insertClassPath(srcFile.getAbsolutePath());
            fis = new FileInputStream(srcFile);
            zis = new ZipInputStream(fis);
            outFile.delete();
            outFile.getParentFile().mkdirs();
            fos = new FileOutputStream(outFile);
            zos = new ZipOutputStream(fos);
            ZipEntry entry = zis.getNextEntry();
            while (entry != null && !entry.getName().contains("../")) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    CtClass ctClass = classPool.makeClass(zis, false);
                    if (ctClass.isFrozen()) {
                        ctClass.defrost();
                    }
                    Set<String> keys = mappedName.keySet();
                    for (String key : keys) {
                        ctClass.replaceClassName(key, mappedName.get(key));
                    }

                    String newEntryName = ctClass.getName();
                    if (newEntryName != null && newEntryName.length() > 0) {
                        newEntryName = newEntryName.replace(".", "/");
                    } else {
                        newEntryName = entry.getName();
                    }
                    if (!newEntryName.endsWith(".class")) {
                        newEntryName = newEntryName + ".class";
                    }
                    zos.putNextEntry(new ZipEntry(newEntryName));
                    zos.write(ctClass.toBytecode());
                }
                entry = zis.getNextEntry();
            }
            return true;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

}
