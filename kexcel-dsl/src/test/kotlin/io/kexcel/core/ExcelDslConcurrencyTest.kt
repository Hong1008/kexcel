package io.kexcel.core

import io.kexcel.driver.ExcelDriver
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ExcelDslConcurrencyTest {

    @Test
    fun `should throw ExcelConcurrentWriteException when multiple threads access same scope`() {
        val driver = SheetTest.MockExcelDriver()
        val sheetScope = SheetScope(driver)

        val startLatch = CountDownLatch(1)
        val firstThreadLatch = CountDownLatch(1)
        val exceptionRef = AtomicReference<Throwable?>(null)

        // 첫 번째 스레드: 락을 획득하고 오래 머무름
        val t1 = Thread {
            sheetScope.row {
                startLatch.countDown() // 락을 잡았음을 알림
                Thread.sleep(500) // 락을 유지함
            }
        }

        // 두 번째 스레드: 첫 번째 스레드가 락을 잡은 동안 접근 시도
        val t2 = Thread {
            startLatch.await() // t1이 락을 잡을 때까지 대기
            try {
                sheetScope.row {
                    cell(value = "Should fail")
                }
            } catch (e: Throwable) {
                exceptionRef.set(e)
            } finally {
                firstThreadLatch.countDown()
            }
        }

        t1.start()
        t2.start()

        assertTrue(firstThreadLatch.await(2, TimeUnit.SECONDS), "Test timed out")

        val exception = exceptionRef.get()
        assertTrue(
            exception is ExcelConcurrentWriteException,
            "Expected ExcelConcurrentWriteException but got $exception"
        )
        assertTrue(exception?.message?.contains("Concurrent write detected") == true)

        t1.join()
        t2.join()
    }
}
