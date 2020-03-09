package com.personal.wspring.servlet;

import com.personal.wspring.annotation.WAutowired;
import com.personal.wspring.annotation.WController;
import com.personal.wspring.annotation.WRequestMapping;
import com.personal.wspring.annotation.WService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class WDispatchServlet extends HttpServlet {

    // 属性配置文件
    private Properties contextConfig = new Properties();
    private List<String> classNameList = new ArrayList<String>();

    // ioc容器
    Map<String, Object> iocMap = new HashMap<String, Object>();
    Map<String, Method> handleMapping = new HashMap<String, Method>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init();
        // 1.加载配置文件
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));

        // 2.扫描相关的类
        doScanner(contextConfig.getProperty("scan-package"));

        // 3.初始化ioc容器，将所有相关的类保存到IOC容器中
        doInstance();

        // 4.依赖注入
        doAutowire();

        // 5.初始化handleMapping
        initHandleMapping();

        // 6.打印数据
        doPrintTestData();


    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 拦截、匹配
     * @param req
     * @param resp
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        System.out.println("[INFO] Request url is {" + url +"}");

        if (!this.handleMapping.containsKey(url)){
            try {
                resp.getWriter().write("404 NOT FOUND");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Method method = this.handleMapping.get(url);
            System.out.println("[INFO] method -->" + method);

            String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
            System.out.println("[INFO] iocMap.get(beanName)->" + iocMap.get(beanName));

            try {
                method.invoke(iocMap.get(beanName),req,resp);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 6.打印测试数据
     */
    private void doPrintTestData() {
        System.out.println("[INFO-6]----data------------------------");

        System.out.println("contextConfig.propertyNames()-->" + contextConfig.propertyNames());

        System.out.println("[classNameList]-->");
        for (String str : classNameList) {
            System.out.println(str);
        }

        System.out.println("[iocMap]-->");
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[handlerMapping]-->");
        for (Map.Entry<String, Method> entry : handleMapping.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[INFO-6]----done-----------------------");

        System.out.println("====启动成功====");
    }

    /**
     * 5.初始化handleMapping
     */
    private void initHandleMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            // 不是controller的话继续循环
            if (!clazz.isAnnotationPresent(WController.class)) {
                continue;
            }
            String baseUrl = "";

            // 类上面加了WRequestMapping的注解，先提取类的url
            if (clazz.isAnnotationPresent(WRequestMapping.class)) {
                WRequestMapping wRequstMapping = clazz.getAnnotation(WRequestMapping.class);
                baseUrl = wRequstMapping.value();
            }

            // 方法加了WRequestMapping的注解，将url进行拼接
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(WRequestMapping.class)) {
                    continue;
                }
                WRequestMapping wRequstMapping = method.getAnnotation(WRequestMapping.class);
                String url = ("/" + baseUrl + "/" + wRequstMapping.value());
                url = url.replaceAll("/+","/");

                // 将具体方法与对应的url放入handleMapping中
                handleMapping.put(url, method);
                System.out.println("[INFO] HandleMapping put {" + url + "}-{" + method + "}");

            }
        }
    }

    /**
     * 4.依赖注入
     */
    private void doAutowire() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> stringObjectEntry : iocMap.entrySet()) {
            // 获取类的属性
            Field[] fields = stringObjectEntry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(WAutowired.class)) {
                    continue;
                }
                System.out.println("[INFO]{exist autowire}");

                // 获取注解对应的类
                WAutowired wAutowired = field.getAnnotation(WAutowired.class);
                String beanName = wAutowired.value().trim();

                // 获取wAutowire注解的值
                if ("".equals(beanName)) {
                    System.out.println("[INFO]{wAutowire value is null}");
                    beanName = field.getType().getName();
                }

                // 强制访问私有属性
                field.setAccessible(true);

                try {
                    field.set(stringObjectEntry.getValue(), iocMap.get(beanName));
                    System.out.println("[INFO]{field set " + stringObjectEntry.getValue() + "}-{" + iocMap.get(beanName) + "}");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }


            }

        }
    }

    /**
     * 3.初始化ioc容器
     */
    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }
        try {
            for (String className : classNameList) {

                Class<?> clazz = Class.forName(className);
                // controller注入ioc容器
                if (clazz.isAnnotationPresent(WController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();

                    // 保存在ioc容器-->map中
                    iocMap.put(beanName, instance);
                    System.out.println("[INFO]{" + beanName + " have been saved in ioc map.}");

                    // service 保存到ioc容器中
                } else if (clazz.isAnnotationPresent(WService.class)) {
                    // 1.默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    // 2.自定义命名
                    WService wService = clazz.getAnnotation(WService.class);
                    if (!"".equals(wService.value())) {
                        beanName = wService.value();
                    }

                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);

                    System.out.println("[INFO]{" + beanName + " have been saved in ioc map.}");
                    // 3.接口不能直接实例化，需实例化实现类
                    for (Class<?> aClass : clazz.getInterfaces()) {
                        if (iocMap.containsKey(aClass.getName())) {
                            throw new Exception("The beanName is exist.");
                        }
                        iocMap.put(aClass.getName(), instance);
                        System.out.println("[INFO]{" + aClass.getName() + " have been saved in ioc map.}");

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 类名首字母小写
     *
     * @param simpleName
     * @return
     */
    private String toLowerFirstCase(String simpleName) {
        char[] charArray = simpleName.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    /**
     * 2.扫描相关的类
     *
     * @param property
     */
    private void doScanner(String property) {
        URL resourcePath = this.getClass().getClassLoader()
                .getResource("/" + property.replaceAll("\\.", "/"));
        if (resourcePath == null) {
            return;
        }
        File classPath = new File(resourcePath.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                System.out.println("[INFO]{" + file.getName() + "}is a directory");
                // 递归子目录
                doScanner(property + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    System.out.println("[INFO]{" + property + "." + file.getName() + " is not a class file.}");
                    continue;
                }
                String className = property + "." + file.getName().replace(".class", "");
                classNameList.add(className);
                System.out.println("[INFO]{" + className + " was saved in classNameList.}");
            }
        }

    }

    /**
     * 1.加载配置文件
     *
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {

        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
