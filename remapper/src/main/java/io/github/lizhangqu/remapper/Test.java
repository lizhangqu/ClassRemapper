package io.github.lizhangqu.remapper;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        ClassRemapper classRemapper = new ClassRemapper(new File("working/bugly.jar"), new File("working/out.jar"), "com.tencent.bugly", "shadow.bugly");
        boolean remap = classRemapper.remap();
        System.out.println("result:" + remap);
    }
}
