package com.slg.module.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 放到外层，确保spring启动优先创建BeanTool
 */
@Component
public class BeanTool implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private static final ConcurrentHashMap<Class<?>, Object> beanCache = new ConcurrentHashMap<>();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BeanTool.applicationContext = applicationContext;
    }

    /**
     * 拿到ApplicationContext对象实例后就可以手动获取Bean的注入实例对象
     */
    public static <T> T getBean(Class<T> clazz) {
        // 1. 检查缓存
        Object cachedBean = beanCache.get(clazz);
        if (cachedBean != null) {
            return clazz.cast(cachedBean);
        }

        // 2. 从 Spring 容器获取
        T bean = resolveBeanFromContext(clazz);

        // 3. 缓存并返回
        beanCache.putIfAbsent(clazz, bean); // 线程安全的原子操作
        return bean;
    }

    private static <T> T resolveBeanFromContext(Class<T> clazz) {
        try {
            return applicationContext.getBean(clazz);
        } catch (NoUniqueBeanDefinitionException e) {
            // 明确提示用户需要指定 Bean 名称或使用 @Primary
            throw new IllegalStateException("存在多个 " + clazz.getName() + " 类型的 Bean，请通过名称指定或标记 @Primary。", e);
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalStateException("未找到类型为 " + clazz.getName() + " 的 Bean。", e);
        }
    }
}
