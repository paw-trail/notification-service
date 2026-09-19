package com.pawtrail.notification.infrastructure.persistence;

import com.pawtrail.notification.domain.repository.WithdrawalLockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL 의 트랜잭션 단위 advisory 잠금으로 알림 만들기와 탈퇴를 한 줄로 세웁니다.
 *
 * 행 잠금(SELECT ... FOR UPDATE)을 쓰지 않는 이유는 잠글 행이 늘 있지 않기 때문입니다.
 * 설정 행이 없는 사람도 막아야 합니다. advisory 잠금은 행과 상관없이 키 하나로 잡히며
 * 트랜잭션이 끝나면 저절로 풀립니다. policy 가 장소 정정을 줄 세운 것과 같은 장치입니다.
 *
 * 키는 고정 문자열의 64비트 해시입니다.
 * advisory 잠금은 데이터베이스마다 따로라 notif_db 밖의 잠금과 겹치지 않습니다.
 *
 * 잠금 함수가 void 를 돌려주므로 FROM 절에 두고 상수를 고릅니다.
 * void 열을 그대로 읽으면 타입 매핑에 걸릴 수 있기 때문입니다.
 */
@Repository
public class WithdrawalLockRepositoryImpl implements WithdrawalLockRepository {

    static final String LOCK_KEY = "notification:withdrawal";

    private static final String SHARED_SQL =
            "SELECT 1 FROM pg_advisory_xact_lock_shared(hashtextextended(CAST(:key AS text), 0))";

    private static final String EXCLUSIVE_SQL =
            "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(CAST(:key AS text), 0))";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void lockForCreation() {
        acquire(SHARED_SQL);
    }

    @Override
    public void lockForWithdrawal() {
        acquire(EXCLUSIVE_SQL);
    }

    private void acquire(String sql) {
        entityManager.createNativeQuery(sql)
                .setParameter("key", LOCK_KEY)
                .getSingleResult();
    }
}
