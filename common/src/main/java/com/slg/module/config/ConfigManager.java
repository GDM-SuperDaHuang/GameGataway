package com.slg.module.config;


import com.slg.module.util.StringUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.File;
import java.lang.annotation.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private String configRootPath;

    private String configFilePackage;

    private ConcurrentHashMap<Class<? extends GameConfig>, ConfigStore> storages;

    public ConfigManager(String configFilePackage) {
        this.configRootPath = System.getProperty("user.dir")+File.separator+"config"+File.separator;
        this.configFilePackage = configFilePackage;
        this.storages = new ConcurrentHashMap<>();
        System.out.println(this.configRootPath);
    }

    public boolean init(){

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(KVConfig.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(DataConfig.class));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(this.configFilePackage);
        for (BeanDefinition candidate : candidates) {
            System.out.println("load config: " + candidate.getBeanClassName());
            try {
                Class<?> cls = Class.forName(candidate.getBeanClassName());
                if(!GameConfig.class.isAssignableFrom(cls)){
                   continue;
                }
                Class<? extends GameConfig> configCls = (Class<? extends GameConfig>) cls;
                KVConfig kvRes = cls.getAnnotation(KVConfig.class);
                DataConfig dataRes = cls.getAnnotation(DataConfig.class);
                if(Objects.isNull(kvRes)  && Objects.isNull(dataRes)){
                    continue;
                }
                if (Objects.nonNull(kvRes) && StringUtil.emptyString(kvRes.path())) {
                   continue;
                }
                if (Objects.nonNull(dataRes) && StringUtil.emptyString(dataRes.path())) {
                    continue;
                }
                ConfigStore configStore = new ConfigStore(configCls);
                if(configStore.loadData(this.configRootPath)) {
                    storages.put(configCls, configStore);
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }

        }
        return true;
    }



    public <T extends GameConfig> T getKVConfig(Class<T> cfgClass) {
        ConfigStore storage = storages.get(cfgClass);
        if (storage != null) {
            return storage.getConfigByIndex(0);
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public <T extends GameConfig> List<T> getConfigDataList(Class<T> cfgClass) {
        ConfigStore storage = storages.get(cfgClass);
        if (storage != null) {
            return storage.getConfigList();
        }
        return null;
    }

    public <T extends GameConfig> T getConfigDataByKey(Class<T> cfgClass,Object key) {
        ConfigStore storage = storages.get(cfgClass);
        if (storage != null) {
            return storage.getConfigByKey(key);
        }
        return null;
    }

    /**
     * KV格式
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface KVConfig {
        String path() default "";
    }

    /**
     * 数组格式
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DataConfig {
        String path() default "";
    }

    /**
     * Id主键
     *
     * @author hawk
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface Id {
    }
}
