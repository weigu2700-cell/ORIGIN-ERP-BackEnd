package org.smart.erp.production.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.smart.erp.common.utils.redis.OperationString;
import org.smart.erp.production.dto.BOMCacheDto;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOMRedisTests {

    @Mock
    private OperationString operationString;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    private BOMRedis bomRedis;

    @BeforeEach
    void setUp() {
        bomRedis = new BOMRedis(operationString, redissonClient);
    }

    @Test
    void emptyMarkerIsAHitAndDoesNotInvokeLoader() {
        when(operationString.get(anyString(), eq(1L))).thenReturn("EMPTY");

        assertThat(bomRedis.getOrLoad(1L, () -> {
            throw new AssertionError("EMPTY must not load");
        })).isNull();
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    void normalValueIsReturnedWithoutLock() {
        BOMCacheDto dto = new BOMCacheDto();
        when(operationString.get(anyString(), eq(1L))).thenReturn(dto);

        assertThat(bomRedis.getOrLoad(1L, () -> null)).isSameAs(dto);
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    void missLoadsAndCachesNormalValue() throws Exception {
        BOMCacheDto dto = new BOMCacheDto();
        dto.setMaterialId(1L);
        when(operationString.get(anyString(), eq(1L))).thenReturn(null);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        doReturn(true).when(lock).tryLock(anyLong(), any());

        assertThat(bomRedis.getOrLoad(1L, () -> dto)).isSameAs(dto);
        verify(operationString).set(eq("erp:bom:hot:"), eq(1L), eq(dto), any(Duration.class));
    }

    @Test
    void missCachesEmptyWhenLoaderReturnsNull() throws Exception {
        when(operationString.get(anyString(), eq(1L))).thenReturn(null);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        doReturn(true).when(lock).tryLock(anyLong(), any());

        assertThat(bomRedis.getOrLoad(1L, () -> null)).isNull();
        verify(operationString).set(eq("erp:bom:hot:"), eq(1L), eq("EMPTY"), any(Duration.class));
    }

    @Test
    void evictionRunsOnlyAfterCommitWhenTransactionIsActive() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            bomRedis.evictAfterCommit(1L);
            verify(operationString, never()).delete(anyString(), eq(1L));
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(operationString).delete("erp:bom:hot:", 1L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
