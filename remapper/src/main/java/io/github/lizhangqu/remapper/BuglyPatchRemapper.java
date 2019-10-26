package io.github.lizhangqu.remapper;

import javassist.CtClass;

import java.io.File;

public class BuglyPatchRemapper extends ClassRemapper {
    public BuglyPatchRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix) {
        super(srcFile, outFile, originalPrefix, newPrefix);
    }

    public BuglyPatchRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix, String originalAndroidPackageName, String newAndroidPackageName) {
        super(srcFile, outFile, originalPrefix, newPrefix, originalAndroidPackageName, newAndroidPackageName);
    }

    @Override
    protected void edit(CtClass ctClass) {
        super.edit(ctClass);
    }
}
