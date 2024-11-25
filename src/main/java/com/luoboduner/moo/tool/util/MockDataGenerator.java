package com.luoboduner.moo.tool.util;

import cn.hutool.json.JSONUtil;
import com.github.javafaker.Faker;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDataGenerator {

    private static final Faker faker = new Faker();

    public static String generateMockJson(String classCode) {
        Map<String, Object> mockData = new HashMap<>();
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(classCode).getResult().orElseThrow(() -> new RuntimeException("Failed to parse class code"));

        cu.findAll(FieldDeclaration.class).forEach(field -> {
            for (VariableDeclarator variable : field.getVariables()) {
                String fieldName = variable.getNameAsString();
                String fieldType = variable.getTypeAsString();
                mockData.put(fieldName, generateMockValue(fieldType));
            }
        });

        try {
            return JSONUtil.toJsonPrettyStr(mockData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate mock JSON", e);
        }
    }

    private static Object generateMockValue(String type) {
        switch (type) {
            case "String":
                return faker.lorem().sentence();
            case "int":
            case "Integer":
                return faker.number().numberBetween(1, 100);
            case "long":
            case "Long":
                return faker.number().numberBetween(1L, 100L);
            case "double":
            case "Double":
                return faker.number().randomDouble(2, 1, 100);
            case "boolean":
            case "Boolean":
                return faker.bool().bool();
            case "Date":
                return faker.date().birthday();
            default:
                if (type.startsWith("List<")) {
                    String genericType = type.substring(5, type.length() - 1);
                    List<Object> list = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        list.add(generateMockValue(genericType));
                    }
                    return list;
                } else if (type.startsWith("Map<")) {
                    String[] genericTypes = type.substring(4, type.length() - 1).split(",");
                    String keyType = genericTypes[0].trim();
                    String valueType = genericTypes[1].trim();
                    Map<Object, Object> map = new HashMap<>();
                    for (int i = 0; i < 3; i++) {
                        map.put(generateMockValue(keyType), generateMockValue(valueType));
                    }
                    return map;
                } else if (type.startsWith("Set<")) {
                    String genericType = type.substring(4, type.length() - 1);
                    List<Object> list = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        list.add(generateMockValue(genericType));
                    }
                    return list;
                } else if (type.endsWith("[]")) {
                    String genericType = type.substring(0, type.length() - 2);
                    List<Object> list = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        list.add(generateMockValue(genericType));
                    }
                    return list;
                }
                return null;
        }
    }

}