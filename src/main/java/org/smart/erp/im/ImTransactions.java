package org.smart.erp.im;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class ImTransactions {

    private final TransactionTemplate write;

    private final TransactionTemplate read;

    public ImTransactions(PlatformTransactionManager manager) {
        write = new TransactionTemplate(manager);
        read = new TransactionTemplate(manager);
        read.setReadOnly(true);
        read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    public <T> T write(Supplier<T> work) {
        for (int attempt = 0;; attempt++) {
            try {
                return write.execute(status -> work.get());
            }
            catch (DuplicateKeyException | PessimisticLockingFailureException error) {
                // 唯一键竞争/死锁必须在整个事务回滚后重读幂等结果，不能在失败事务内继续写。
                if (attempt >= 2)
                    throw error;
            }
        }
    }

    public <T> T read(Supplier<T> work) {
        return read.execute(status -> work.get());
    }

}
