package com.aidream.javaagent;

import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Map;

public class HotCodeHelper {

    private static final String jarPath;
    private static final String pid;
    private static final Class<?> vmClass;

    /**
     * redefine classes
     *
     * @param classes
     * @throws Exception
     */
    public static void redefineClasses(Map<String, byte[]> classes) throws Exception {
        Object vm = null;
        try {
            // attach vm
            vm = vmClass.getMethod("attach", String.class).invoke(null, pid);
            vmClass.getMethod("loadAgent", String.class).invoke(vm, jarPath);// load agent
            Instrumentation instrumentation = (Instrumentation) System.getProperties().get(AgentTag.KEY_AGENT);
            if (instrumentation == null) {
                throw new RuntimeException("fail to load agent!");
            }
            // 1.整理需要重定义的类
            ClassDefinition[] classDefines = new ClassDefinition[classes.size()];
            int i = 0;
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                Class<?> c = Class.forName(entry.getKey());
                ClassDefinition classDefinition = new ClassDefinition(c, entry.getValue());
                classDefines[i++] = classDefinition;
            }
            // 2.redefine
            instrumentation.redefineClasses(classDefines);
        } finally {
            if (vm != null) {
                vmClass.getMethod("detach").invoke(vm);// detach vm
            }
        }
    }

    /**
     * 获取 agent jar包路径
     *
     * @return
     */
    private static String getJarPath() {
        // StringUtils是jar文件内容
        URL url = AgentTag.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            filePath = URLDecoder.decode(url.getPath(), "utf-8");// 转化为utf-8编码
        } catch (Exception e) {
            e.printStackTrace();
        }
        File file = new File(filePath);
        return file.getAbsolutePath();
    }

    private static final String getToolsClasspath() {
        File javaHome = new File(System.getProperty("java.home"));
        String tools = "lib/tools.jar";
        File toolsFile = new File(javaHome.getParent(), tools);
        if (toolsFile.exists()) {
            return toolsFile.getAbsolutePath();
        }
        toolsFile = new File(javaHome, tools);
        if (toolsFile.exists()) {
            return toolsFile.getAbsolutePath();
        }
        String JAVA_HOME = System.getenv("JAVA_HOME");
        if (JAVA_HOME != null && JAVA_HOME.length() > 0) {
            toolsFile = new File(JAVA_HOME, tools);
            if (toolsFile.exists()) {
                return toolsFile.getAbsolutePath();
            }
        }
        return null;
    }

    static {
        jarPath = getJarPath();
        System.out.println("java agent:jarPath: " + jarPath);

        // 当前进程pid
        String name = ManagementFactory.getRuntimeMXBean().getName();
        pid = name.split("@")[0];
        System.out.println("当前进程pid：" + pid);
        String toolsClasspath = getToolsClasspath();
        Class<?> vmClass_ = null;
        if (toolsClasspath != null) {
            try {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{new File(toolsClasspath).toURI().toURL()});
                vmClass_ = classLoader.loadClass("com.sun.tools.attach.VirtualMachine");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("toolsPath=" + toolsClasspath + ", vmClass：" + vmClass_);
        vmClass = vmClass_;
    }
}
