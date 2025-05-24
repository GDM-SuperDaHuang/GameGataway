package com.slg.module.config;

import com.alibaba.fastjson.JSONObject;
import com.slg.module.util.StringUtil;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConfigStore {

    private String filePath;

    private Class<? extends GameConfig> configClass;

    List<GameConfig> configList = new ArrayList<>();

    Map<Object,GameConfig> configMap = new HashMap<>();

    public ConfigStore(Class<? extends GameConfig> configClass) {
        this.configClass = configClass;
    }

    public boolean loadData(String rootPath){
        ConfigManager.KVConfig kvRes = configClass.getAnnotation(ConfigManager.KVConfig.class);
        ConfigManager.DataConfig dataRes = configClass.getAnnotation(ConfigManager.DataConfig.class);
        if (Objects.nonNull(kvRes) && !StringUtil.emptyString(kvRes.path())) {
            String configPath = rootPath + kvRes.path();
            return this.loadKVData(configPath);
        }
        if (Objects.nonNull(dataRes) && !StringUtil.emptyString(dataRes.path())) {
            String configPath = rootPath + dataRes.path();
            return this.loadArrData(configPath);
        }
        return false;
    }

    public boolean loadKVData(String filePath){
        try {
            this.filePath = filePath;
            GameConfig configBase = this.configClass.newInstance();
            configBase.setConfigStore( GameConfig.ResType.KV,this);

            File file = new File(this.filePath);
            if (!file.exists()) {
                throw new RuntimeException("config file not exist: " + this.filePath);
            }

            List<String> content = IOUtils.readLines(new FileInputStream(this.filePath), "UTF-8");
            StringBuffer sb = new StringBuffer();
            for(String line : content){
                sb.append(line);
            }
            JSONObject object = JSONObject.parseObject(sb.toString());
            for (String key : object.keySet()) {
                setAttr(configBase, key, object.getString(key));
            }
            // 存储
            configList.add(configBase);
            return true;
        } catch (Exception e) {
            e.getMessage();
            return false;
        }
    }


    public boolean loadArrData(String filePath){

        try {
            this.filePath = filePath;
            File file = new File(this.filePath);
            if (!file.exists()) {
                throw new RuntimeException("config file not exist: " + this.filePath);
            }

            List<String> content = IOUtils.readLines(new FileInputStream(this.filePath), "UTF-8");
            for(String line : content){
                GameConfig configBase = this.configClass.newInstance();
                configBase.setConfigStore( GameConfig.ResType.KV,this);
                JSONObject object = JSONObject.parseObject(line);
                Object dataKey = null;
                for (String key : object.keySet()) {
                    setAttr(configBase, key, object.getString(key));
                    Field field = getClassField(configBase, key);
                    ConfigManager.Id id = field.getAnnotation(ConfigManager.Id.class);
                    if (id != null) {
                        // id标注重复
                        if (dataKey != null) {
                            throw new RuntimeException("config id annotation duplicate: " + this.filePath + ", nodeName: "+ key);
                        }

                        String type = field.getType().toString();
                        if (type.indexOf("int") >= 0 || type.indexOf("Integer") >= 0) {
                            dataKey = object.getIntValue(key);
                        } else if (type.indexOf("String") >= 0) {
                            dataKey = object.getString(key);
                        }
                    }
                }
                // 存储
                configList.add(configBase);
                // map存储
                if (dataKey != null) {
                    if (configMap.containsKey(dataKey)) {
                        throw new Exception("config key duplicate: " + this.filePath + ", key: " + dataKey);
                    }
                    configMap.put(dataKey, configBase);
                }
            }
            return true;
        } catch (Exception e) {
            e.getMessage();
            return false;
        }
    }


    public <T> T getConfigByIndex(int index) {
        if (configList.size() > index) {
            return (T) configList.get(index);
        }
        return null;
    }

    public <T> List<T> getConfigList() {
        return (List<T>) this.configList;
    }

    public <T> T getConfigByKey(Object key) {
        return (T) this.configMap.get(key);
    }






    public void setAttr(Object instance, String attrName, String attrValue) throws Exception {
        Field field = getClassField(instance, attrName);
        if (field == null) {
            throw new RuntimeException("config class cannot find field, class: " + instance.getClass().getName());
        }
        try {
            // 必须带final属性
            if ((field.getModifiers() & Modifier.FINAL) == 0) {
                throw new RuntimeException("config attribute must be final, class: " + instance.getClass().getSimpleName() + ", field: " + attrName);
            }
            String type = field.getType().toString();
            field.setAccessible(true);

            try {
                if (type.indexOf("boolean") >= 0 || type.indexOf("Boolean") >= 0) {
                    if (attrValue.equals("0")) {
                        field.set(instance, false);
                    } else if (attrValue.equals("1")) {
                        field.set(instance, true);
                    } else {
                        field.set(instance, Boolean.valueOf(attrValue));
                    }
                } else if (type.indexOf("int") >= 0 || type.indexOf("Integer") >= 0) {
                    field.set(instance, Integer.valueOf(attrValue));
                } else if (type.indexOf("long") >= 0 || type.indexOf("Long") >= 0) {
                    field.set(instance, Long.valueOf(attrValue));
                } else if (type.indexOf("float") >= 0 || type.indexOf("Float") >= 0) {
                    field.set(instance, Float.valueOf(attrValue));
                } else if (type.indexOf("double") >= 0 || type.indexOf("Double") >= 0) {
                    field.set(instance, Double.valueOf(attrValue));
                } else if (type.indexOf("String") >= 0) {
                    field.set(instance, attrValue);
                } else if (type.indexOf("Date") >= 0) {
                    field.set(instance, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(attrValue));
                } else {
                    throw new Exception("cannot support property type: " + type);
                }
            } catch (Exception exception) {
                throw exception;
            } finally {
                field.setAccessible(false);
            }

        } catch (Exception exception) {
            throw exception;
        }
    }




    public Field getClassField(Object instance, String attrName) {
        Field field = null;
        try {
            Class<?> instanceClass = instance.getClass();

            do {
                try {

                    field = instanceClass.getDeclaredField(attrName);
                } catch (Exception e) {
                    e.printStackTrace();
                    instanceClass = instanceClass.getSuperclass();
                }
            } while (field == null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (field != null) {
            field.setAccessible(true);
        }
        return field;
    }
}
