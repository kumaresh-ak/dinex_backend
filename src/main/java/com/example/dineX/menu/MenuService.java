package com.example.dineX.menu;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.storage.FileStorageService;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MenuService {
    private final MenuCategoryRepository categories;
    private final MenuItemRepository items;
    private final FileStorageService storage;
    private final AuditService audit;

    public MenuService(MenuCategoryRepository categories, MenuItemRepository items,
                       FileStorageService storage, AuditService audit) {
        this.categories = categories;
        this.items = items;
        this.storage = storage;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<MenuDtos.CategoryView> categories(Long hotelId) {
        Map<Long, Long> counts = items.findByHotelIdAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(hotelId).stream()
                .collect(Collectors.groupingBy(MenuItem::getCategoryId, Collectors.counting()));
        return categories.findByHotelIdAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(hotelId).stream()
                .map(c -> categoryView(c, counts.getOrDefault(c.getId(), 0L).intValue()))
                .toList();
    }

    @Transactional
    public MenuDtos.CategoryView createCategory(Long hotelId, MenuDtos.CategoryRequest request) {
        String name = request.name().trim();
        ensureUniqueName(hotelId, name, null);
        MenuCategory category = new MenuCategory();
        category.setHotelId(hotelId);
        category.setName(name);
        category.setDisplayPriority(request.displayPriority());
        category.setEnabled(request.enabled());
        categories.save(category);
        audit.record(hotelId, "MENU_CATEGORY_CREATED", "MenuCategory", category.getId(), name);
        return categoryView(category, 0);
    }

    @Transactional
    public MenuDtos.CategoryView updateCategory(Long hotelId, Long id, MenuDtos.CategoryRequest request) {
        MenuCategory category = findCategory(hotelId, id);
        String name = request.name().trim();
        ensureUniqueName(hotelId, name, id);
        category.setName(name);
        category.setDisplayPriority(request.displayPriority());
        category.setEnabled(request.enabled());
        categories.save(category);
        audit.record(hotelId, "MENU_CATEGORY_UPDATED", "MenuCategory", id, name);
        return categoryView(category, (int) items.countByHotelIdAndCategoryIdAndDeletedFlagFalse(hotelId, id));
    }

    @Transactional
    public void deleteCategory(Long hotelId, Long id) {
        MenuCategory category = findCategory(hotelId, id);
        if (items.countByHotelIdAndCategoryIdAndDeletedFlagFalse(hotelId, id) > 0) {
            throw ApiException.conflict("Remove or move the menu items in this category first");
        }
        String base = category.getName().length() > 80 ? category.getName().substring(0, 80) : category.getName();
        category.setName(base + " #" + id);
        category.setEnabled(false);
        category.setDeletedFlag(true);
        categories.save(category);
        audit.record(hotelId, "MENU_CATEGORY_DELETED", "MenuCategory", id, null);
    }

    @Transactional(readOnly = true)
    public List<MenuDtos.ItemView> items(Long hotelId, Long categoryId) {
        Map<Long, MenuCategory> categoryMap = categoryMap(hotelId);
        return items.findByHotelIdAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(hotelId).stream()
                .filter(i -> categoryId == null || categoryId.equals(i.getCategoryId()))
                .map(i -> itemView(i, categoryMap))
                .toList();
    }

    @Transactional
    public MenuDtos.ItemView createItem(Long hotelId, MenuDtos.ItemRequest request) {
        findCategory(hotelId, request.categoryId());
        MenuItem item = new MenuItem();
        item.setHotelId(hotelId);
        apply(item, request);
        items.save(item);
        audit.record(hotelId, "MENU_ITEM_CREATED", "MenuItem", item.getId(), item.getName());
        return itemView(item, categoryMap(hotelId));
    }

    @Transactional
    public MenuDtos.ItemView updateItem(Long hotelId, Long id, MenuDtos.ItemRequest request) {
        MenuItem item = findItem(hotelId, id);
        findCategory(hotelId, request.categoryId());
        apply(item, request);
        items.save(item);
        audit.record(hotelId, "MENU_ITEM_UPDATED", "MenuItem", id, item.getName());
        return itemView(item, categoryMap(hotelId));
    }

    @Transactional
    public void deleteItem(Long hotelId, Long id) {
        MenuItem item = findItem(hotelId, id);
        item.setDeletedFlag(true);
        item.setAvailable(false);
        item.setVisible(false);
        items.save(item);
        audit.record(hotelId, "MENU_ITEM_DELETED", "MenuItem", id, item.getName());
    }

    @Transactional
    public MenuDtos.ItemView setAvailability(Long hotelId, Long id, boolean available) {
        MenuItem item = findItem(hotelId, id);
        item.setAvailable(available);
        items.save(item);
        audit.record(hotelId, available ? "MENU_ITEM_AVAILABLE" : "MENU_ITEM_UNAVAILABLE", "MenuItem", id, item.getName());
        return itemView(item, categoryMap(hotelId));
    }

    @Transactional
    public MenuDtos.ItemView setVisibility(Long hotelId, Long id, boolean visible) {
        MenuItem item = findItem(hotelId, id);
        item.setVisible(visible);
        items.save(item);
        audit.record(hotelId, visible ? "MENU_ITEM_SHOWN" : "MENU_ITEM_HIDDEN", "MenuItem", id, item.getName());
        return itemView(item, categoryMap(hotelId));
    }

    @Transactional
    public MenuDtos.ItemView uploadImage(Long hotelId, Long id, MultipartFile file) {
        MenuItem item = findItem(hotelId, id);
        item.setImageUrl(storage.store(file, "menu"));
        items.save(item);
        audit.record(hotelId, "MENU_ITEM_IMAGE_UPDATED", "MenuItem", id, null);
        return itemView(item, categoryMap(hotelId));
    }

    private void apply(MenuItem item, MenuDtos.ItemRequest request) {
        item.setCategoryId(request.categoryId());
        item.setName(request.name().trim());
        item.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        item.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
        item.setPreparationMinutes(request.preparationMinutes());
        item.setFoodType(request.foodType());
        item.setDisplayPriority(request.displayPriority());
        item.setAvailable(request.available());
        item.setVisible(request.visible());
    }

    private void ensureUniqueName(Long hotelId, String name, Long exceptId) {
        categories.findByHotelIdAndNameIgnoreCase(hotelId, name)
                .filter(existing -> !existing.getId().equals(exceptId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("A category with this name already exists");
                });
    }

    private MenuCategory findCategory(Long hotelId, Long id) {
        return categories.findByIdAndHotelIdAndDeletedFlagFalse(id, hotelId)
                .orElseThrow(() -> ApiException.notFound("Menu category"));
    }

    private MenuItem findItem(Long hotelId, Long id) {
        return items.findByIdAndHotelIdAndDeletedFlagFalse(id, hotelId)
                .orElseThrow(() -> ApiException.notFound("Menu item"));
    }

    private Map<Long, MenuCategory> categoryMap(Long hotelId) {
        return categories.findByHotelIdAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(hotelId).stream()
                .collect(Collectors.toMap(MenuCategory::getId, Function.identity()));
    }

    private MenuDtos.CategoryView categoryView(MenuCategory c, int itemCount) {
        return new MenuDtos.CategoryView(c.getId(), c.getName(), c.getDisplayPriority(), c.isEnabled(), itemCount);
    }

    private MenuDtos.ItemView itemView(MenuItem i, Map<Long, MenuCategory> categoryMap) {
        MenuCategory category = categoryMap.get(i.getCategoryId());
        return new MenuDtos.ItemView(i.getId(), i.getCategoryId(), category == null ? null : category.getName(),
                i.getName(), i.getDescription(), i.getPrice(), i.getImageUrl(), i.getPreparationMinutes(),
                i.getFoodType().name(), i.isAvailable(), i.isVisible(), i.getDisplayPriority());
    }

    /** Resolves visible menu items for a hotel, keyed by id, for ordering. */
    @Transactional(readOnly = true)
    public Map<Long, MenuItem> orderableItems(Long hotelId, Collection<Long> ids) {
        return items.findByHotelIdAndIdInAndDeletedFlagFalse(hotelId, ids).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
    }

}
