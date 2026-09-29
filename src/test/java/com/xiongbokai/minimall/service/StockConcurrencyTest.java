package com.xiongbokai.minimall.service;

import com.xiongbokai.minimall.exception.InsufficientStockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
class StockConcurrencyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int INITIAL_STOCK = 10;
    private static final int THREAD_COUNT = 20;

    @BeforeEach
    void resetStock() {
        jdbcTemplate.update(
                "UPDATE product SET stock = ? WHERE id = 1",
                INITIAL_STOCK
        );
    }

    @AfterEach
    void restoreStock() {
        jdbcTemplate.update(
                "UPDATE product SET stock = ? WHERE id = 1",
                50
        );
    }

    @Test
    void shouldNotOversellUnderConcurrency() throws Exception {
        ExecutorService pool =
                Executors.newFixedThreadPool(THREAD_COUNT);

        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    productService.deductStock(1L, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException exception) {
                    failureCount.incrementAndGet();
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        boolean finished = done.await(10, TimeUnit.SECONDS);

        pool.shutdown();

        Integer finalStock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product WHERE id = 1",
                Integer.class
        );

        System.out.println("===== 并发扣库存结果 =====");
        System.out.println("全部线程在10秒内结束: " + finished);
        System.out.println("成功扣减次数: " + successCount.get());
        System.out.println("库存不足次数: " + failureCount.get());
        System.out.println("最终库存: " + finalStock);
        System.out.println("==========================");

        assertTrue(finished);
        assertEquals(
                INITIAL_STOCK,
                successCount.get()
        );
        assertEquals(
                THREAD_COUNT - INITIAL_STOCK,
                failureCount.get()
        );
        assertEquals(0, finalStock);

    }
}
