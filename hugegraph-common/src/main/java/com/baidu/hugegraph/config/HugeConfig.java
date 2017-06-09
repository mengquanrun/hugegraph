/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.hugegraph.config;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.exception.ConfigException;
import com.baidu.hugegraph.util.E;

public class HugeConfig extends PropertiesConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(HugeConfig.class);

    public HugeConfig(PropertiesConfiguration config) {
        if (config == null) {
            throw new ConfigException("Config object is null.");
        }

        if (config.getFile() != null) {
            this.setFile(config.getFile());
        }

        Iterator<String> keys = config.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            this.setProperty(key.replace("..", "."),
                    config.getProperty(key));
        }

        updateDefaultConfiguration();
    }

    public HugeConfig(String configurationFile) throws ConfigurationException {
        super(loadConfiguration(configurationFile));
        updateDefaultConfiguration();
    }

    private static File loadConfiguration(String fileName) {
        E.checkArgument(StringUtils.isNotEmpty(fileName),
                "can not load configuration file: "
                        + "%s", fileName);
        File file = new File(fileName);
        E.checkArgument(file.exists() && file.isFile() && file.canRead(),
                "Need to specify a readable configuration file, but was "
                        + "given: %s", file.toString());
        return file;
    }

    public void updateDefaultConfiguration() {
        try {
            Iterator<String> keys = this.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!ConfigSpace.containKey(key)) {
                    logger.error("A redundant config option is set：" + key);
                    continue;
                }
                ConfigOption option = ConfigSpace.get(key);
                Class dataType = option.dataType();
                String getMethod = "get" + dataType.getSimpleName();
                Method method = this.getClass()
                        .getMethod(getMethod, String.class, dataType);
                option.value(method.invoke(this, key, option.value()));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new ConfigException(e.getMessage());
        }
    }

    /**
     * @param option
     * @param <T>
     *
     * @return
     */
    public <T> T get(ConfigOption<T> option) {
        return option.value();
    }

    @Override
    public void addProperty(String key, Object value) {
        if (value instanceof String) {
            String val = (String) value;
            if (val.startsWith("[") && val.endsWith("]")) {
                val = val.substring(1, val.length() - 1);
            }
            value = val;
        }
        super.addProperty(key, value);
    }

}
