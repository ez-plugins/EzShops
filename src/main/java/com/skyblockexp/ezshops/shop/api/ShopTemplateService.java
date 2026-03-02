package com.skyblockexp.ezshops.shop.api;

import com.skyblockexp.ezshops.shop.template.ShopTemplate;
import java.util.Collection;
import java.util.Optional;

/**
 * API that exposes shop template registration and import capabilities to other plugins.
 */
public interface ShopTemplateService {

    /**
     * Registers a shop template so other plugins can discover and import it.
     * If a template with the same id exists it will be replaced.
     */
    void registerTemplate(ShopTemplate template);

    /**
     * Lists all registered templates.
     */
    Collection<ShopTemplate> listTemplates();

    /**
     * Imports a template by id. Returns empty when template not found.
     */
    Optional<ShopTemplate> importTemplate(String templateId);
}
