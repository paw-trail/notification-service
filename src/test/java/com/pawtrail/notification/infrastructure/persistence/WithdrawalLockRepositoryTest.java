package com.pawtrail.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pawtrail.notification.IntegrationTestSupport;
import com.pawtrail.notification.domain.repository.WithdrawalLockRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 탈퇴 잠금이 알림 만들기와 탈퇴를 실제로 한 줄로 세우는지 확인합니다. policy 의 장소 잠금 검사와 같은 모양입니다.
 *
 * 테스트 트랜잭션을 쓰지 않고 스레드마다 트랜잭션을 따로 엽니다.
 * 한 트랜잭션 안에서는 같은 잠금을 다시 잡아도 막히지 않아 경쟁을 볼 수 없기 때문입니다.
 * 잠금만 잡고 표에는 아무것도 쓰지 않으므로 치울 행이 없습니다.
 */
class WithdrawalLockRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private WithdrawalLockRepository withdrawalLockRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    @DisplayName("탈퇴는 진행 중인 알림 만들기가 끝날 때까지 기다린다")
    void 탈퇴는_만들기를_기다린다() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> creation = pool.submit(() -> inTransaction(() -> {
            withdrawalLockRepository.lockForCreation();
            holding.countDown();
            awaitQuietly(release);
        }));
        assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

        Future<?> withdrawal = pool.submit(() -> inTransaction(() -> withdrawalLockRepository.lockForWithdrawal()));

        // 만들기가 잡고 있는 동안에는 끝나지 않아야 함
        assertThatThrownBy(() -> withdrawal.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        release.countDown();
        creation.get(5, TimeUnit.SECONDS);
        withdrawal.get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("알림 만들기끼리는 서로 기다리지 않는다")
    void 만들기끼리는_기다리지_않는다() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> first = pool.submit(() -> inTransaction(() -> {
            withdrawalLockRepository.lockForCreation();
            holding.countDown();
            awaitQuietly(release);
        }));
        assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

        Future<?> second = pool.submit(() -> inTransaction(() -> withdrawalLockRepository.lockForCreation()));

        // 앞선 만들기가 아직 잡고 있어도 바로 끝나야 함 — 조건 변경과 제보 결과가 서로 막지 않음
        second.get(5, TimeUnit.SECONDS);

        release.countDown();
        first.get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("알림 만들기는 진행 중인 탈퇴가 끝날 때까지 기다린다")
    void 만들기는_탈퇴를_기다린다() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> withdrawal = pool.submit(() -> inTransaction(() -> {
            withdrawalLockRepository.lockForWithdrawal();
            holding.countDown();
            awaitQuietly(release);
        }));
        assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

        Future<?> creation = pool.submit(() -> inTransaction(() -> withdrawalLockRepository.lockForCreation()));

        // 탈퇴가 잡고 있는 동안에는 끝나지 않아야 함 — 끝난 뒤에 설정을 읽어 탈퇴 표시를 보게 됨
        assertThatThrownBy(() -> creation.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        release.countDown();
        withdrawal.get(5, TimeUnit.SECONDS);
        creation.get(5, TimeUnit.SECONDS);
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
