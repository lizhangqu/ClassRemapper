package io.github.lizhangqu.remapper;

import javassist.CtClass;
import javassist.bytecode.ConstPool;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BuglyPatchRemapper extends ClassRemapper {
    public BuglyPatchRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix) {
        super(srcFile, outFile, originalPrefix, newPrefix);
    }

    public BuglyPatchRemapper(File srcFile, File outFile, String originalPrefix, String newPrefix, String originalAndroidPackageName, String newAndroidPackageName) {
        super(srcFile, outFile, originalPrefix, newPrefix, originalAndroidPackageName, newAndroidPackageName);
    }

    @Override
    protected void edit(CtClass ctClass) throws Exception {
        super.edit(ctClass);

        //常量池修改
        String name = ctClass.getName();
        if (name.equals("com.tencent.bugly.proguard.y")
                || name.equals("com.tencent.bugly.proguard.q")
                || name.equals("com.tencent.bugly.proguard.n")
                || name.equals("com.tencent.bugly.proguard.x")
                || name.equals("com.tencent.bugly.crashreport.common.info.a")
                || name.equals("com.tencent.bugly.crashreport.crash.anr.b")
                || name.equals("com.tencent.bugly.crashreport.crash.jni.NativeCrashHandler")
                || name.equals("com.tencent.bugly.crashreport.crash.d")
        ) {
            ConstPool constPool = ctClass.getClassFile().getConstPool();
            Field items1 = constPool.getClass().getDeclaredField("items");
            items1.setAccessible(true);
            Object items = items1.get(constPool);

            Class<?> Utf8InfoClass = Class.forName("javassist.bytecode.Utf8Info");
            Field tag1 = Utf8InfoClass.getDeclaredField("tag");
            tag1.setAccessible(true);
            int tagFlag = (int) tag1.get(null);


            Method elementAt = items.getClass().getDeclaredMethod("elementAt", int.class);
            elementAt.setAccessible(true);

            int size = constPool.getSize();
            for (int i = 1; i < size; ++i) {
                int tag = constPool.getTag(i);
                //noinspection GroovyAccessibility
                if (tag == tagFlag) {
                    String utf8Info = constPool.getUtf8Info(i);
                    if (utf8Info.equals("/buglylog_")) {
                        //将常量池/buglylog_修改为/shadow_buglylog_
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "/shadow_buglylog_");

                    } else if (utf8Info.equals("bugly_db_")) {
                        //将常量池bugly_db_修改为shadow_bugly_db_
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_bugly_db_");
                    } else if (utf8Info.equals("bugly_db")) {
                        //将常量池bugly_db修改为shadow_bugly_db
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_bugly_db");
                    } else if (utf8Info.equals("crashrecord")) {
                        //将常量池crashrecord修改为shadow_crashrecord
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_crashrecord");
                    } else if (utf8Info.equals("bugly")) {
                        //将常量池bugly修改为bugly
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "shadow_bugly");
                    } else if (utf8Info.equals("/app_bugly")) {
                        //将常量池/app_bugly修改为/shadow_app_bugly
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "/app_shadow_bugly");
                    } else if (utf8Info.equals("H5")) {
                        //将常量池H5修改为Other
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "Other");
                    } else if (utf8Info.equals("CrashReportInfo")) {
                        //将常量池CrashReportInfo修改为ShadowCrashReportInfo
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "ShadowCrashReportInfo");
                    } else if (utf8Info.equals("CrashReport")) {
                        //将常量池CrashReport修改为ShadowCrashReport
                        Object item = elementAt.invoke(items, i);
                        Field string = item.getClass().getDeclaredField("string");
                        string.setAccessible(true);
                        string.set(item, "ShadowCrashReport");
                    }

                    Object item = elementAt.invoke(items, i);
                    Field string = item.getClass().getDeclaredField("string");
                    string.setAccessible(true);
                    String currentValue = (String) string.get(item);
                    if (!utf8Info.equals(currentValue)) {
                        System.out.println(ctClass.getName() + ": " + utf8Info + " -> " + currentValue);
                    }
                }
            }
        }
    }
}
