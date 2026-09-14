package org.smart.erp.production.cache;

import org.smart.erp.production.dto.BOMCacheDto;
import org.smart.erp.production.dto.BOMItemCacheDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.entity.BOMItem;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class BOMRedis {

    private final String BOM_CACHE_KEY_PREFIX = "erp:bom:active:";

    private final RedisTemplate<String, Object> redisTemplate;

    public BOMRedis(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public BOMCacheDto getBomCache(Long materialId) {
        String key = BOM_CACHE_KEY_PREFIX + materialId;
        return (BOMCacheDto) redisTemplate.opsForValue().get(key);
    }

    public BOMCacheDto buildBomCache(BOM bom, List<BOMItem> bomItems) {
        BOMCacheDto bomCacheDto = new BOMCacheDto();
        BeanUtils.copyProperties(bom, bomCacheDto);
        bomCacheDto.setItems(Optional.ofNullable(bomItems)
                .orElse(List.of()).stream().map(bomItem -> {
                    BOMItemCacheDto bomItemCacheDto = new BOMItemCacheDto();
                    BeanUtils.copyProperties(bomItem,bomItemCacheDto);
                    return bomItemCacheDto;
                }).toList());
        return bomCacheDto;
    }

    public void cacheActiveBom(BOMCacheDto bomCacheDto) {
        if (bomCacheDto == null || bomCacheDto.getMaterialId() == null) return;
        String key = BOM_CACHE_KEY_PREFIX + bomCacheDto.getMaterialId();
        redisTemplate.opsForValue().set(key, bomCacheDto, Duration.ofHours(2));
    }

    public void evictBomCache(Long materialId) {
        String key = BOM_CACHE_KEY_PREFIX + materialId;
        redisTemplate.delete(key);
    }

}
