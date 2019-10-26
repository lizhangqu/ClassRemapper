package io.github.lizhangqu.remapper;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        ClassRemapper classRemapperJar = new ClassRemapper(new File("working/bugly.jar"), new File("working/out.jar"), "com.tencent.bugly", "shadow.bugly");
        boolean remapJar = classRemapperJar.remap();
        System.out.println("jar result:" + remapJar);

        ClassRemapper classRemapperAar = new ClassRemapper(new File("/Users/lizhangqu/IdeaProjects/ClassRemapper/working/bugly.aar"), new File("/Users/lizhangqu/IdeaProjects/ClassRemapper/working/out.aar"), "com.tencent.bugly", "shadow.bugly", "com.tencent.bugly.crashreport", "shadow.bugly.crashreport");
        boolean remapAar = classRemapperAar.remap();
        System.out.println("aar result:" + remapAar);
    }
}
