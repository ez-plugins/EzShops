# Shop Template API

---

## Purpose

The Shop Template API allows other plugins to register, list and import YAML-serializable shop templates. Templates may contain categories, item entries, and optional full `ItemStack` data (as Base64).

## Service

Preferred access (recommended):

```java
ShopTemplateService svc = EzShopsAPI.getInstance().getTemplateAPI();
if (svc == null) {
    // handle feature-disabled case
}
```

Fallback (legacy):

```java
RegisteredServiceProvider<ShopTemplateService> p = Bukkit.getServicesManager().getRegistration(ShopTemplateService.class);
ShopTemplateService svc = p != null ? p.getProvider() : null;
```

## API surface

- `void registerTemplate(ShopTemplate template)` — register or replace a template (persists to `plugins/EzShops/templates/`)
- `Collection<ShopTemplate> listTemplates()` — list registered templates
- `Optional<ShopTemplate> importTemplate(String templateId)` — import a template by id

Source: [src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateService.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateService.java)

## Examples

Registering a template from an embedded YAML resource:

```java
try (InputStream in = plugin.getResource("templates/my-template.yml")) {
    ShopTemplate template = ShopTemplate.fromYaml(in);
    svc.registerTemplate(template);
}
```

Importing a template by id:

```java
svc.importTemplate("example-template-id").ifPresent(template -> {
    // read template.categories() / template.items()
});
```

Creating a template programmatically (short):

```java
ShopIcon icon = ShopIcon.builder().material("DIRT").amount(16).build();
ShopItem item = ShopItem.builder("dirt").material("DIRT").amount(16).icon(icon).buy(250).sell(100).build();
ShopTemplate t = new ShopTemplateBuilder("id","Name").addCategory(new ShopTemplateCategoryBuilder("building").addItem("dirt", item).build()).build();
svc.registerTemplate(t);
```

## Full ItemStack data

To preserve full ItemStack metadata (enchants, NBT, custom model data), serialize ItemStacks to Base64 and include them under the `itemstack-base64` key for item entries. Use `ItemStackSerializers` or `TemplateWriter` helpers found in the codebase.

Example helper:

```java
List<ItemStack> stacks = List.of(player.getInventory().getItemInMainHand());
ShopTemplate template = TemplateWriter.createTemplateFromStacks("my-id", "My Template", stacks);
String yaml = TemplateWriter.templateToYaml(template);
```

## Persistence & Admin commands

- Registered templates are saved to `plugins/EzShops/templates/<id>.yml` and are loaded on startup.
- Admin export command: `/shop export <templateId>` (requires `ezshops.export`).

## See also

- [docs/api.md](docs/api.md) — canonical API reference
- Source: [src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateBuilder.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateBuilder.java)
Exporting templates via command

