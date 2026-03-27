package com.finder.letscheck.search.cache;

import com.finder.letscheck.model.FoodDictionary;
import com.finder.letscheck.model.Item;
import com.finder.letscheck.repository.FoodDictionaryRepository;
import com.finder.letscheck.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
public class SearchCacheService {

    private final FoodDictionaryRepository foodDictionaryRepository;
    private final ItemRepository itemRepository;

    private List<FoodDictionary> activeFoodDictionary = new ArrayList<>();

    private Map<String, String> canonicalMap = new HashMap<>();
    private Map<String, String> aliasToCanonicalMap = new HashMap<>();

    private Set<String> normalizedCities = new HashSet<>();
    private Set<String> normalizedAreas = new HashSet<>();

    @PostConstruct
    public void loadCaches() {
        refreshCaches();
        System.out.println("Food dictionary loaded: " + activeFoodDictionary.size());
        System.out.println("Canonical map size: " + canonicalMap.size());
        System.out.println("Alias map size: " + aliasToCanonicalMap.size());
        System.out.println("Cities loaded: " + normalizedCities.size());
        System.out.println("Areas loaded: " + normalizedAreas.size());
    }

    public synchronized void refreshCaches() {
        loadFoodDictionaryCache();
        loadLocationCache();
    }

    private void loadFoodDictionaryCache() {
        List<FoodDictionary> foodDocs = foodDictionaryRepository.findByIsActiveTrue();
        this.activeFoodDictionary = foodDocs;

        Map<String, String> canonicalTemp = new HashMap<>();
        Map<String, String> aliasTemp = new HashMap<>();

        for (FoodDictionary food : foodDocs) {
            if (Boolean.TRUE.equals(food.getIsActive())) {
                canonicalTemp.put(food.getNormalizedCanonicalName(), food.getNormalizedCanonicalName());

                if (food.getAliases() != null) {
                    for (FoodDictionary.FoodAlias alias : food.getAliases()) {
                        if (Boolean.TRUE.equals(alias.getIsActive())) {
                            aliasTemp.put(alias.getNormalizedName(), food.getNormalizedCanonicalName());
                        }
                    }
                }
            }
        }

        this.canonicalMap = canonicalTemp;
        this.aliasToCanonicalMap = aliasTemp;
    }

    private void loadLocationCache() {
        List<Item> items = itemRepository.findAll();

        this.normalizedCities = items.stream()
                .map(Item::getNormalizedCity)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        this.normalizedAreas = items.stream()
                .map(Item::getNormalizedAreaName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}