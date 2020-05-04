package com.lagou.edu.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtil {

    /*
    * 取得某个接口下所有实现这个接口类
    * */
    public static List<Class> getAllClassByInterface(Class c){
        List<Class> returnClassList = null;

        if (c.isInterface()){
            //获取当前的包名
            String packageName = c.getPackage().getName();
            //获取当前包下以及子包下所有的类
            List<Class<?>> allClass = getClasses(packageName);
            if (allClass != null){
                returnClassList = new ArrayList<Class>();
                for (Class classes : allClass){
                    //判断是否是同一个接口
                    if (c.isAssignableFrom(classes)){
                        //本身不加入进去
                        if (!c.equals(classes)){
                            returnClassList.add(classes);
                        }
                    }
                }
            }
        }

        return returnClassList;
    }

    /*
    * 取得某一类所在包的所有类名不含迭代
    * */
    public static String[] getPackageAllClassName(String classLocation, String packageName){
        //将packageName分解
        String[] packagePathSplit = packageName.split("[.]");
        String realClassLocation = classLocation;
        int packageLength = packagePathSplit.length;
        for (int i = 0; i < packageLength; i++){
            realClassLocation = realClassLocation + File.separator + packagePathSplit[i];
        }
        File packageDir = new File(realClassLocation);
        if (packageDir.isDirectory()){
            String[] allClassName = packageDir.list();
            return allClassName;
        }
        return null;
    }

    /*
    * 从包package中获取所有的Class
    * */
    public static List<Class<?>> getClasses(String packageName){

        //第一个class类的集合
        List<Class<?>> classes = new ArrayList<Class<?>>();
        //是否循环迭代
        boolean recursive = true;
        //获取包的名字 并进行替换
        String packageDirName = packageName.replace('.','/');
        //定义一个枚举的结合 并进行循环来整理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            //循环迭代下去
            while (dirs.hasMoreElements()){
                //获取下一个元素
                URL url = dirs.nextElement();
                //得到协议的名称
                String protocol = url.getProtocol();
                //如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)){
                    //获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(),"UTF-8");
                    //以文件的方式扫描整个包下的文件并添加到集合中
                    findAndAddClassesInPackageByFile(packageName,filePath,recursive,classes);
                }else if ("jar".equals(protocol)){
                    //如果是jar包文件
                    //定义一个JarFile
                    JarFile jar;
                    try {
                        //获取jar
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        //从此jar包得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        //同样的进行循环迭代
                        while (entries.hasMoreElements()){
                            //获取jar中的一个实体可以是目录和一些jar包中的其他文件如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            //如果是以/开头的
                            if (name.charAt(0) == '/'){
                                //获取后面的字符串
                                name = name.substring(1);
                            }
                            //如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)){
                                int idx = name.lastIndexOf('/');
                                //如果以"/"结尾是一个包
                                if (idx != -1){
                                    //获取包名把"/"替换成"."
                                    packageName = name.substring(0,idx).replace('/','.');
                                }
                                //如果可迭代下去并且是一个包
                                if ((idx != -1) || recursive){
                                    //如果是一个".class"文件而且不是目录
                                    if (name.endsWith(".class") && !entry.isDirectory()){
                                        //去掉后面的".class"获取真正的类名
                                        String className = name.substring(packageName.length() + 1,name.length() - 6);
                                        try {
                                            //添加到classes
                                            classes.add(Class.forName(packageName + '.' + className));
                                        }catch (ClassNotFoundException e){
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return classes;
    }

    /*
    * 以文件的形式来获取包下的所有Class
    * */
    public static void findAndAddClassesInPackageByFile(String packageName,String packagePath,final boolean recursive,List<Class<?>> classes){
        //获取此包的目录建立一个File
        File dir = new File(packagePath);
        //如果不存在或者也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()){
            return;
        }
        //如果存在就获取包下的所有文件包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            //自定义过滤规则如果可以循环(包含子目录)或者是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        //循环所有文件
        for (File file : dirfiles){
            //如果是目录则继续扫描
            if (file.isDirectory()){
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                //如果是java类文件去掉后面的.class只留下类名
                String className = file.getName().substring(0,file.getName().length() - 6);
                try {
                    //添加到集合中去
                    classes.add(Class.forName(packageName + '.' + className));
                }catch (ClassNotFoundException e){
                    e.printStackTrace();
                }
            }
        }
    }

}
