package io.github.lizhangqu.remapper;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        BuglyPatchRemapper classRemapperJar = new BuglyPatchRemapper(new File("working/bugly.jar"), new File("working/out.jar"), "com.tencent.bugly", "shadow.bugly");
        classRemapperJar.setAndroidJar(new File("working/android.jar"));
        boolean remapJar = classRemapperJar.remap();
        System.out.println("jar result:" + remapJar);

        BuglyPatchRemapper classRemapperAar = new BuglyPatchRemapper(new File("working/bugly.aar"), new File("working/out.aar"), "com.tencent.bugly", "shadow.bugly", "com.tencent.bugly.crashreport", "shadow.bugly.crashreport");
        classRemapperAar.setAndroidJar(new File("working/android.jar"));
        boolean remapAar = classRemapperAar.remap();
        System.out.println("aar result:" + remapAar);
    }
}
