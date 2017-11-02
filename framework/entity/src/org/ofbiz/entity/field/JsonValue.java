package org.ofbiz.entity.field;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ofbiz.base.util.collections.LocalizedMap;
import org.ofbiz.entity.model.ModelField;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.*;

/**
 * JSON字段的处理
 * 先不进行字段验证等，实现后再重构吧，实在没时间
 * Created by Ted Zheng on 2017/8/16.
 */
public class JsonValue extends PGobject implements Map<String, Object>, LocalizedMap<Object> {
    // 不参与序列化
    private transient ModelField modelField = null;

    private Map<String, Object> fields = new HashMap<String, Object>();

    public JsonValue() {
        this.type = "jsonb";
    }

    public void setModelField(ModelField modelField) {
        this.modelField = modelField;
    }

    public ModelField getModelField() {
        return this.modelField;
    }

    // 通过Model处理的，是GenericEntity的一个字段
    public static JsonValue create(ModelField modelField, String text) {
        ObjectMapper mapper = new ObjectMapper();
        if (text != null && !"".equals(text)) {
            Map<String, Object> map = null;
            try {
                map = mapper.readValue(text, Map.class);
            } catch (Exception e) {
                e.printStackTrace(); // 先只打印出来
            }
            return create(modelField, map);
        } else {
            return new JsonValue();
        }
    }

    public String getValue() {
        return this.toString();
    }


    public static JsonValue create(ModelField modelField, Map<? extends String, ?> m) {
        JsonValue newValue = new JsonValue();
        newValue.modelField = modelField; // 暂时为Null 未来需要扩展

        if (m != null && m.size() != 0) {
            newValue.fields.putAll(m);
        }

        return newValue;
    }

    protected void init(ModelField modelField) {
        if (modelField == null) {
            throw new IllegalArgumentException("Init");
        }
        this.modelField = modelField;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this.fields);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // 将来要想想怎么处理好，Map应该不会出现
            return "{}";
        }
    }

    @Override
    public Object clone() {
        return JsonValue.create(this.modelField, this.fields);
    }


    @Override
    public Object get(String name, Locale locale) {
        // 现在还不太会用
        return this.get(name);
    }

    @Override
    public int size() {
        return fields.size();
    }

    @Override
    public boolean isEmpty() {
        if (this.size() == 0) return true;
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return fields.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return fields.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return fields.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return fields.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return fields.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        fields.putAll(m);
    }

    @Override
    public void clear() {
        fields.clear();
    }

    @Override
    public Set<String> keySet() {
        return fields.keySet();
    }

    @Override
    public Collection<Object> values() {
        return fields.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return fields.entrySet();
    }
}
