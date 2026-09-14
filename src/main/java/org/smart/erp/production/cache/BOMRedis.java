package org.smart.erp.production.cache;

import org.smart.erp.common.utils.RedisUtil;
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
public class BOMRedis extends RedisUtil {

    private final String BOM_CACHE_KEY_PREFIX = "erp:bom:active:";

    public BOMRedis(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public BOMCacheDto getBomCache(Long materialId) {
        return getCache(BOM_CACHE_KEY_PREFIX, materialId);
    }

    public void cacheActiveBom(BOMCacheDto bomCacheDto) {
        activeCache(BOM_CACHE_KEY_PREFIX, bomCacheDto.getMaterialId(), bomCacheDto, Duration.ofHours(1));
    }

    public void evictBomCache(Long materialId) {
        evictCache(BOM_CACHE_KEY_PREFIX, materialId);
    }

    /**
     * 构建BOM缓存对象
     * @param bom BOM实体
     * @param bomItems BOM明细列表
     * @return BOM缓存对象
     */
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

}
