package com.slg.module.config;

public abstract class GameConfig {

    protected static class ResType {
        static final int ARR = 1;
        static final int KV = 2;
    }


    /**
     * 配置类型
     */
    public int resType;

    /**
     * 所属仓库
     */
    public ConfigStore configStore;



    public void setConfigStore(int resType, ConfigStore configStore) {
        this.resType = resType;
        this.configStore = configStore;
    }


    /**
     * 检查表数据
     * @return
     */
    public boolean dataCheck() {
        return true;
    }

    /**
     * 重新组装数据
     * @return
     */
    public boolean assembleData(){
        return true;
    }
}
