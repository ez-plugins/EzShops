package com.skyblockexp.ezshops.shop.template;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.skyblockexp.ezshops.shop.api.ShopTemplateCategory;
import java.util.Objects;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Simple, serializable representation of a shop template.
 * Intended to be extensible; currently holds an id, human name and a list of item definitions.
 */
public class ShopTemplate {
    protected String id;
    protected String name;
    protected List<Map<String, Object>> items;
    protected Map<String, String> files;
    protected Map<String, ShopTemplateCategory> categories;

    /**
     * Protected no-arg constructor to allow subclassing and programmatic construction.
     */
    protected ShopTemplate() {
        this.id = "";
        this.name = "";
        this.items = Collections.emptyList();
        this.files = Collections.emptyMap();
        this.categories = Collections.emptyMap();
    }

    public ShopTemplate(String id, String name, List<Map<String, Object>> items) {
        this(id, name, items, null);
    }

    public ShopTemplate(String id, String name, List<Map<String, Object>> items, Map<String, String> files) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.items = items == null ? Collections.emptyList() : List.copyOf(items);
        this.files = files == null ? Collections.emptyMap() : Map.copyOf(files);
        this.categories = Collections.emptyMap();
    }

    public ShopTemplate(String id, String name, List<Map<String, Object>> items, Map<String, String> files, Map<String, ShopTemplateCategory> categories) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.items = items == null ? Collections.emptyList() : List.copyOf(items);
        this.files = files == null ? Collections.emptyMap() : Map.copyOf(files);
        this.categories = categories == null ? Collections.emptyMap() : Map.copyOf(categories);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<Map<String, Object>> items() {
        return items;
    }

    public Map<String, String> files() {
        return files;
    }

    public Map<String, ShopTemplateCategory> categories() {
        return categories;
    }

    /**
     * Parse a YAML input stream into a ShopTemplate. Expects a mapping with at least `id` and `name`.
     */
    @SuppressWarnings("unchecked")
    public static ShopTemplate fromYaml(InputStream in) {
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(in);
        if (!(loaded instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("Invalid template format, expected mapping");
        }
        String id = String.valueOf(m.get("id"));
        String name = String.valueOf(m.get("name"));
        Object itemsObj = m.get("items");
        List<Map<String, Object>> items = itemsObj instanceof List<?> list ? (List<Map<String, Object>>) list : Collections.emptyList();
        Object filesObj = m.get("files");
        Map<String, String> files = Collections.emptyMap();
        if (filesObj instanceof Map<?, ?> fm) {
            Map<String, String> tmp = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> e : fm.entrySet()) {
                tmp.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
            files = tmp;
        }

        Object catsObj = m.get("categories");
        Map<String, ShopTemplateCategory> categories = Collections.emptyMap();
        if (catsObj instanceof Map<?, ?> cm) {
            Map<String, ShopTemplateCategory> tmp = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> e : cm.entrySet()) {
                String catId = String.valueOf(e.getKey());
                Object catVal = e.getValue();
                if (catVal instanceof Map<?, ?> catMap) {
                    Map<String, Object> props = new java.util.LinkedHashMap<>();
                    Map<String, Map<String, Object>> itemsMap = new java.util.LinkedHashMap<>();
                    for (Map.Entry<?, ?> ce : catMap.entrySet()) {
                        String k = String.valueOf(ce.getKey());
                        if ("items".equals(k) && ce.getValue() instanceof Map<?, ?> im) {
                            for (Map.Entry<?, ?> ie : im.entrySet()) {
                                String itemId = String.valueOf(ie.getKey());
                                if (ie.getValue() instanceof Map<?, ?> itemProps) {
                                    Map<String, Object> copy = new java.util.LinkedHashMap<>();
                                    for (Map.Entry<?, ?> ip : itemProps.entrySet()) copy.put(String.valueOf(ip.getKey()), ip.getValue());
                                    itemsMap.put(itemId, copy);
                                }
                            }
                        } else {
                            props.put(k, ce.getValue());
                        }
                    }
                    tmp.put(catId, new ShopTemplateCategory(catId, props, itemsMap));
                }
            }
            categories = tmp;
        }

        return new ShopTemplate(id, name, items, files, categories);
    }

    /**
     * Produces a YAML representation of this template.
     */
    public String toYaml() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(opts);
        StringWriter sw = new StringWriter();
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("id", id);
        out.put("name", name);
        out.put("items", items);
        if (files != null && !files.isEmpty()) out.put("files", files);
        if (categories != null && !categories.isEmpty()) {
            Map<String, Object> catsOut = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, ShopTemplateCategory> e : categories.entrySet()) {
                catsOut.put(e.getKey(), e.getValue().toMap());
            }
            out.put("categories", catsOut);
        }
        yaml.dump(out, sw);
        return sw.toString();
    }
}
