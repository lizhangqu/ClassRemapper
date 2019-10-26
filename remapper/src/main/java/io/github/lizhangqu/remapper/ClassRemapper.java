package io.github.lizhangqu.remapper;

import javassist.ClassPool;
import javassist.CtClass;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

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
    private boolean isAndroidAarFile = false;

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

        if (zipFile.getEntry("AndroidManifest.xml") != null) {
            if (originalAndroidPackageName == null || newAndroidPackageName == null) {
                throw new RuntimeException("It's Android aar zipFile, you should provide originalAndroidPackageName and newAndroidPackageName");
            }
            isAndroidAarFile = true;
        }

        if (isAndroidAarFile) {
            File temp = new File(outFile.getParentFile(), "temp");
            ZipUtil.unpack(srcFile, temp);
            File classesJar = new File(temp, "classes.jar");
            zipFile.close();
            zipFile = new ZipFile(classesJar);
            FileUtils.deleteQuietly(temp);
        }

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
        if (isAndroidAarFile) {
            File temp = new File(outFile.getParentFile(), "temp");
            try {
                ZipUtil.unpack(srcFile, temp);
                File classesJar = new File(temp, "classes.jar");
                File remappedJar = new File(temp, "remapped.jar");
                boolean remap = remap(classesJar, remappedJar, mappedName);
                if (remap) {
                    classesJar.delete();
                    remappedJar.renameTo(classesJar);
                }
                File manifestXml = new File(temp, "AndroidManifest.xml");
                File aaptManifestXml = new File(temp, "aapt/AndroidManifest.xml");
                String manifestXmlContent = FileUtils.readFileToString(manifestXml);
                manifestXmlContent = manifestXmlContent.replace(originalAndroidPackageName, newAndroidPackageName);
                FileUtils.deleteQuietly(manifestXml);
                FileUtils.copy(new ByteArrayInputStream(manifestXmlContent.getBytes()), manifestXml);

                String aaptManifestXmlContent = FileUtils.readFileToString(aaptManifestXml);
                aaptManifestXmlContent = aaptManifestXmlContent.replace(originalAndroidPackageName, newAndroidPackageName);
                FileUtils.deleteQuietly(aaptManifestXml);
                FileUtils.copy(new ByteArrayInputStream(aaptManifestXmlContent.getBytes()), aaptManifestXml);

                ZipUtil.pack(temp, outFile);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                FileUtils.deleteQuietly(temp);
            }
        } else {
            return remap(srcFile, outFile, mappedName);
        }
        return false;
    }

    private boolean remap(File srcFile, File outFile, Map<String, String> mappedName) {
        System.out.println(srcFile);
        System.out.println(outFile);
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
                    edit(classPool, ctClass);
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
                    ctClass.detach();
                } else {
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    copy(zis, byteArrayOutputStream);
                    zos.write(byteArrayOutputStream.toByteArray());
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

    private static void copy(InputStream inputStream, OutputStream outputStream) {
        int length = -1;
        byte[] buffer = new byte[4096];
        try {
            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, length);
                outputStream.flush();
            }
        } catch (Exception ignore) {

        }
    }

    protected void edit(ClassPool classPool, CtClass ctClass) throws Exception {

    }

}
