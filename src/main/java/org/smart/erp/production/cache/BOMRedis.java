package org.smart.erp.production.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.production.dto.BOMCacheDto;
import org.smart.erp.production.dto.BOMItemCacheDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.entity.BOMItem;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class BOMRedis{

    private final String BOM_CACHE_KEY_PREFIX = "erp:bom:active:";

    private static final String EMPTY_BOM = "EMPTY";

    public static final BOMCacheDto EMPTY_BOM_MARKER = new BOMCacheDto();

    private final OperationString operationString;
    private final RedissonClient redissonClient;

    public BOMRedis(
            OperationString operationString,
            RedissonClient redissonClient
    ) {
        this.operationString = operationString;
        this.redissonClient = redissonClient;
    }
    public BOMCacheDto getBomCache(Long materialId) {
        Object cached = operationString.get(BOM_CACHE_KEY_PREFIX, materialId);
        if (cached == null) {
            return null; // 未命中，调用方需回源查库
        }
        // 注意必须用 .equals()：cached 是从 Redis 反序列化出来的新 String 对象，
        // 与常量 EMPTY_BOM 不是同一引用，用 == 会恒为 false，导致空哨兵被误判后强转抛 ClassCastException
        if (EMPTY_BOM.equals(cached)) {
            return EMPTY_BOM_MARKER;
        }
        if (cached instanceof BOMCacheDto dto) {
            return dto; // 命中生效 BOM
        }
        return null;
    }


    public void cacheActiveBom(BOMCacheDto bomCacheDto) {
        operationString.set(BOM_CACHE_KEY_PREFIX, bomCacheDto.getMaterialId(), bomCacheDto, Duration.ofHours(2));
    }

    public void cacheEmptyBom(Long materialId) {
        operationString.set(BOM_CACHE_KEY_PREFIX, materialId, EMPTY_BOM, Duration.ofMinutes(5));
    }

    public void evictBomCache(Long materialId) {
        operationString.delete(BOM_CACHE_KEY_PREFIX, materialId);
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
