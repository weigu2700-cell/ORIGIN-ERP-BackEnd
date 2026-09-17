package org.smart.erp.production.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.utils.RandomTtl;
import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.production.dto.BOMCacheDto;
import org.smart.erp.production.dto.BOMItemCacheDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.entity.BOMItem;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class BOMRedis{

    private static final String BOM_CACHE_KEY_PREFIX = "erp:bom:hot:";
    private static final String BOM_LOCK_KEY_PREFIX = "erp:lock:bom:build:";

    private static final String EMPTY_BOM = "EMPTY";

    private final OperationString operationString;
    private final RedissonClient redissonClient;

    public BOMRedis(
            OperationString operationString,
            RedissonClient redissonClient
    ) {
        this.operationString = operationString;
        this.redissonClient = redissonClient;
    }
    /** 读取缓存原值；调用方负责区分 miss、EMPTY 和历史脏类型。 */
    private Object readRaw(Long materialId) {
        return operationString.get(BOM_CACHE_KEY_PREFIX, materialId);
    }

    private void deleteUnknown(Long materialId, Object cached) {
        log.warn("删除 BOM 缓存中的未知值类型: materialId={}, type={}", materialId, cached.getClass());
        operationString.delete(BOM_CACHE_KEY_PREFIX, materialId);
    }

    /**
     * 统一负责 BOM 缓存的读取、空值缓存、双检锁与 DB loader 回源。
     * 返回 null 表示没有 active BOM，不向业务层暴露空值哨兵。
     */
    public BOMCacheDto getOrLoad(Long materialId, Supplier<BOMCacheDto> loader) {
        Object first = readRaw(materialId);
        if (first instanceof BOMCacheDto dto) return dto;
        if (EMPTY_BOM.equals(first)) return null;
        if (first != null) deleteUnknown(materialId, first);

        RLock lock = redissonClient.getLock(BOM_LOCK_KEY_PREFIX + materialId);
        boolean locked;
        try {
            locked = lock.tryLock(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "获取 BOM 缓存锁被中断");
        }
        if (!locked) throw new BusinessException(409, "BOM 缓存构建中，请稍后重试");
        try {
            Object second = readRaw(materialId);
            if (second instanceof BOMCacheDto dto) return dto;
            if (EMPTY_BOM.equals(second)) return null;
            if (second != null) deleteUnknown(materialId, second);
            BOMCacheDto loaded = loader.get();
            if (loaded == null) cacheEmptyBom(materialId);
            else cacheActiveBom(loaded);
            return loaded;
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }


    public void cacheActiveBom(BOMCacheDto bomCacheDto) {
        operationString.set(BOM_CACHE_KEY_PREFIX, bomCacheDto.getMaterialId(), bomCacheDto, RandomTtl.ofMinutes(120, 150));
    }

    public void cacheEmptyBom(Long materialId) {
        operationString.set(BOM_CACHE_KEY_PREFIX, materialId, EMPTY_BOM, RandomTtl.ofMinutes(5, 10));
    }

    public void evictBomCache(Long materialId) {
        operationString.delete(BOM_CACHE_KEY_PREFIX, materialId);
    }

    /** DB 写入成功后失效缓存；事务回滚时不误删仍有效的缓存。 */
    public void evictAfterCommit(Long materialId) {
        if (materialId == null) return;
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { evictSafely(materialId); }
            });
        } else {
            evictSafely(materialId);
        }
    }

    private void evictSafely(Long materialId) {
        try {
            evictBomCache(materialId);
        } catch (RuntimeException e) {
            // 已提交事务不能因缓存故障反向失败，仅保留错误日志供运维处理。
            log.error("BOM 缓存失效失败: materialId={}", materialId, e);
        }
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
