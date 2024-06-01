package com.luoboduner.moo.tool.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Yaml 转 Properties 工具
 */
@Slf4j
public class YmlAndPropUtil {
    private static final String PROP_DOT = ".";
    private static final String PROP_COMMA = ",";
    public static final String COMMENTS = " Automatically converts yml file to properties file.";

    public static void main(String[] args) {
        String propertiesData = """
                ababa.list.mylist[0]=1
                ababa.list.mylist[1]=2
                ababa.list.mylist[2]=3
                ababa.map.myMap.1=张三  
                ababa.map.myMap.2=李四  
                ababa.map.myMap.3=王五  
                system.user.age=30
                system.user.name=John Doe  
                system.user.offset= 200
                system.user.sex=1
                spring.launcher=appl.class
                
                tencent.0=1001
                tencent.1=2001
                tencent.2=3002
                
                baidu.list[0]=a001
                baidu.list[1]=b001
                baidu.list[2]=c001
              
                """;


        System.out.println(convertProp2Yml(propertiesData));

        /*String readString = """
                language: java
                                
                before_install:
                  - mvn
                  - install
                  - closet
                  - pillows
                  - DartifactId
                                
                jdk:
                  - openjdk8
                script: "mvn clean package -Dmaven.test.skip=true"
                                
                notifications:
                  name: George
                  sex: 男
                  age:\s
                  email:
                    - rememberber@163.com
                    - adjf@qq.com
                   \s
                """;
        String yml2Prop = YmlAndPropUtil.convertYml2Prop(readString);
        log.info("prop is :{}", yml2Prop);*/
    }

    public static String convertProp2Yml(String propStr) {
        Map<String, Object> yamlData = parseProperties(propStr);

        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(false); // 使用漂亮的流样式
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 设置默认流样式为块样式
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setAllowUnicode(true); // 允许Unicode字符
        options.setIndent(4); // 设置缩进宽度
        Yaml yaml = new Yaml(options);
        return yaml.dumpAsMap(yamlData);
    }

    private static Map<String, Object> parseProperties(String propertiesData) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(propertiesData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> data = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            parseKey(data, key, properties.getProperty(key));
        }
        return data;
    }

    private static void parseKey(Map<String, Object> data, String key, String value) {
        // 去除前后空格
        value = StrUtil.trim(value);
        key = StrUtil.trim(key);

        String[] parts = key.split("\\.");
        Map<String, Object> current = data;
        Map<String, Object> beforeCurrent = null;
        for (int i = 0; i < parts.length - 1; i++) {
            if ((i == parts.length - 2 || parts.length <= 2)
                    && StrUtil.isNumeric(parts[parts.length - 1])) {
                // 如果最后一个key是数字 则将其转成列表
                addObj2MapList(current, parts[i], value);
                return;
            }

            String part = parts[i];
            if (!current.containsKey(part)) {
                current.put(part, new HashMap<>());
            }

            // 指针下移
            current = (Map<String, Object>) current.get(part);
        }
        String lastPart = parts[parts.length - 1];

        if (lastPart.matches("\\w+\\[\\d+\\]")) {
            String listKey = lastPart.substring(0, lastPart.indexOf('['));

            // 将list类型key转成map
            addObj2MapList(current, listKey, value);
        } else {
            current.put(lastPart, value);
        }
    }

    private static void addObj2MapList(Map<String, Object> map, String key, Object value) {
        map.compute(key, (k, v) -> {
            if (v == null) {
                return CollUtil.newArrayList(value);
            } else {
                List<Object> list = Convert.toList(Object.class, v);
                list.add(value);
                return list;
            }
        });
    }

    /**
     * 转换 Yml → properties
     *
     * @param ymlStr
     * @return
     */
    public static String convertYml2Prop(String ymlStr) {
        Assert.notBlank(ymlStr);

        // 将YAML转换为Properties
        Properties props = recursiveYaml2Prop(YamlUtil.load(new StringReader(ymlStr), Map.class));

        // 将Properties对象写入到控制台
        try (Writer out = new StringWriter()) {
            props.store(out, COMMENTS);
            return out.toString();
        } catch (IOException e) {
            log.error("error", e);
            throw new UtilException("Properties store error.");
        }
    }

    /**
     * map 递归
     *
     * @param ymlMap
     * @return
     */
    private static Properties recursiveYaml2Prop(Map<String, Object> ymlMap) {
        Properties props = new Properties();
        for (Map.Entry<String, Object> entry : ymlMap.entrySet()) {
            Object value = entry.getValue();

            // 空值处理
            if (ObjectUtil.isEmpty(value)) {
                props.setProperty(entry.getKey(), StrUtil.SPACE);
            }

            if (value instanceof Map) {
                // 递归处理嵌套的Map
                Properties nestedProps = recursiveYaml2Prop((Map<String, Object>) value);
                for (String key : nestedProps.stringPropertyNames()) {
                    props.setProperty(entry.getKey() + PROP_DOT + key, nestedProps.getProperty(key));
                }
            } else if (value instanceof List) {
                // list类型处理，用逗号拼接
                props.setProperty(entry.getKey(), StrUtil.join(PROP_COMMA, (List) value));
            } else {
                // 其他
                props.setProperty(entry.getKey(), StrUtil.toString(value));
            }
        }
        return props;
    }
}