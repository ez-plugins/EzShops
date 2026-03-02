package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.shop.api.ShopTemplateService;
import com.skyblockexp.ezshops.shop.template.ShopTemplate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

/**
 * Core implementation of the ShopTemplateService. Keeps an in-memory registry of templates.
 */
public final class ShopTemplateServiceImpl implements ShopTemplateService {

    private final Map<String, ShopTemplate> templates = new LinkedHashMap<>();
    private final File templatesDir;

    public ShopTemplateServiceImpl(File templatesDir) {
        this.templatesDir = templatesDir;
        if (!templatesDir.exists()) {
            templatesDir.mkdirs();
        }
        loadFromDisk();
    }

    public ShopTemplateServiceImpl() {
        this.templatesDir = null;
    }

    @Override
    public synchronized void registerTemplate(ShopTemplate template) {
        templates.put(template.id(), template);
        if (templatesDir != null) {
            File out = new File(templatesDir, template.id() + ".yml");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                String yaml = template.toYaml();
                fos.write(yaml.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                // best effort: log via stderr (plugin logging not available here)
                System.err.println("Failed to save template " + template.id() + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public synchronized Collection<ShopTemplate> listTemplates() {
        return templates.values().stream().collect(Collectors.toUnmodifiableList());
    }

    @Override
    public synchronized Optional<ShopTemplate> importTemplate(String templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }

    private void loadFromDisk() {
        if (templatesDir == null || !templatesDir.exists()) return;
        Yaml yaml = new Yaml();
        File[] files = templatesDir.listFiles((d, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return;
        for (File f : files) {
            try (FileInputStream fis = new FileInputStream(f)) {
                ShopTemplate t = ShopTemplate.fromYaml(fis);
                templates.put(t.id(), t);
            } catch (Exception ex) {
                System.err.println("Failed to load template from " + f.getName() + ": " + ex.getMessage());
            }
        }
    }
}
