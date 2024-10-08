package me.imvs.springcloudconfighelper;

import lombok.Data;
import me.imvs.springcloudconfighelper.core.PropertiesParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PropertiesParserTests {

    static final List<? extends Map.Entry<String, ? extends Serializable>> entryList = List.of(
            Map.entry("single-object", "object"),
            Map.entry("single-array[0]", "array"),
            Map.entry("array[0]", 1),
            Map.entry("array[1]", 2),
            Map.entry("list.of[0].dict.name", "name-0"),
            Map.entry("list.of[0].dict.value", 0),
            Map.entry("list.of[1].dict.name", "name-1"),
            Map.entry("list.of[1].dict.value", 1),
            Map.entry("list.type", "dict"),
            Map.entry("nested[0].array[0]", "nested-array-1"),
            Map.entry("nested[0].array[1]", "nested-array-2")
    );
    static final List<? extends Map.Entry<String, ? extends Serializable>> TestObjectEntryList = List.of(
            Map.entry("string", "foo"),
            Map.entry("number", 9000),
            Map.entry("map.foo", "bar"),
            Map.entry("map.bar", "foo"),
            Map.entry("list[0]", "array-0"),
            Map.entry("list[1]", "array-1"),
            Map.entry("list[2]", "array-2")
    );

    @Test
    public void mapToMapSuccess() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Serializable> entry : entryList) {
            map.put(entry.getKey(), entry.getValue());
        }
        Object object = PropertiesParser.toMap(map);
        System.out.println(object);
        Assertions.assertEquals(
                "{single-object=object, single-array=[array], array=[1, 2], list={of=[{dict={name=name-0, value=0}}, {dict={name=name-1, value=1}}], type=dict}, nested=[{array=[nested-array-1, nested-array-2]}]}",
                object.toString());
    }

    @Test
    public void propertiesToMapSuccess() {
        Properties properties = new Properties();
        for (Map.Entry<String, ? extends Serializable> entry : entryList) {
            properties.put(entry.getKey(), entry.getValue());
        }
        Object object = PropertiesParser.toMap(properties);
        System.out.println(object);
        Assertions.assertEquals(
                "{list={of=[{dict={value=0, name=name-0}}, {dict={value=1, name=name-1}}], type=dict}, nested=[{array=[nested-array-1, nested-array-2]}], single-object=object, array=[1, 2], single-array=[array]}",
                object.toString());
    }

    @Test
    public void mapToObjectSuccess() {
        Properties properties = new Properties();
        for (Map.Entry<String, ? extends Serializable> entry : TestObjectEntryList) {
            properties.put(entry.getKey(), entry.getValue());
        }
        TestObject object = PropertiesParser.toObject(properties, TestObject.class);
        System.out.println(object);
        Assertions.assertEquals(
                "PropertiesParserTests.TestObject(string=foo, number=9000, map={foo=bar, bar=foo}, list=[array-0, array-1, array-2])",
                object.toString());
    }

    @Data
    private static class TestObject {
        String string;
        Number number;
        Map<String, String> map;
        List<String> list;
    }
}
