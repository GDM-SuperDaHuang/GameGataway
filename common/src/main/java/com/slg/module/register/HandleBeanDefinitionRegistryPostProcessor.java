package com.slg.module.register;

import com.slg.module.annotation.ToMethod;
import com.slg.module.annotation.ToServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HandleBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    //pd对象
    private final Map<Integer, Class<?>> classRespMap = new ConcurrentHashMap<>();

    //pb序列化方法
    private final Map<Integer, Method> parseFromMethodMap = new ConcurrentHashMap<>();

    //handle处理类
    private final Map<Integer, Class<?>> handleMap = new ConcurrentHashMap<>();

    //handle目标方法
    private final Map<Integer, Method> methodMap = new ConcurrentHashMap<>();
    private final String[] basePackages = new String[]{""};
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ToServer.class));
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(candidate.getBeanClassName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                if (clazz.isAnnotationPresent(ToServer.class)) {
                    registry.registerBeanDefinition(clazz.getSimpleName(), candidate);
                    // 处理@ToMethod注解的方法
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(ToMethod.class)) {
                            int key = method.getAnnotation(ToMethod.class).value();
                            // 获取方法的所有参数类型
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            int length = parameterTypes.length;
                            //获取第二个参数，加载Protobuf，请求类
                            Class<?> parameterReqType = parameterTypes[1];
                            //获取第三个参数，加载Protobuf，响应类
//                            Class<?> parameterRespType = parameterTypes[2];
//                            classRespMap.put(key, parameterRespType);
                            handleMap.putIfAbsent(key, clazz);
                            methodMap.put(key,method);
                            try {
//                                Method parseFromMethod = parameterType.getMethod("parseFrom", byte[].class);
                                Method parseFromMethod = parameterReqType.getMethod("parseFrom", ByteBuffer.class);
                                parseFromMethodMap.put(key, parseFromMethod);
                            } catch (NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public Method getParseFromMethod (Integer key) {
        return parseFromMethodMap.getOrDefault(key, null);
    }


    public Class<?> getHandleMap(Integer key) {
        return handleMap.getOrDefault(key,null);
    }

    public Method getMethodMap(Integer key) {
        return methodMap.getOrDefault(key,null);
    }

    public Map<Integer, Class<?>> getClassRespMap() {
        return classRespMap;
    }
}