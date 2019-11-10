package space.sentinel.service

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.pi4j.io.gpio.PinState
import org.bytedeco.javacv.Frame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import space.sentinel.camera.CameraReader
import space.sentinel.domain.Chill
import space.sentinel.domain.MotionDetectAlert
import space.sentinel.sensor.PIRReader

internal class MotionDetectorServiceTest {
    private val frame = mock<Frame>()

    private val cameraReader = mock<CameraReader> {
        on { read() }.doReturn(Flux.just(frame))
    }

    @Test
    fun `return alert when state is high`() {
        val pirReader = mock<PIRReader> {
            on { read() }.doReturn(Flux.just(PinState.HIGH))
        }

        StepVerifier.create(MotionDetectorService(pirReader, cameraReader).detect())
                .consumeNextWith {
                    assertEquals(it.image.get(), frame)
                    assertEquals(it::class.java, MotionDetectAlert::class.java)
                }
                .verifyComplete()
    }

    @Test
    fun `return chill when state is low`() {
        val pirReader = mock<PIRReader> {
            on { read() }.doReturn(Flux.just(PinState.LOW))
        }

        StepVerifier.create(MotionDetectorService(pirReader, cameraReader).detect())
                .consumeNextWith {
                    assertEquals(it.image.isEmpty, true)
                    assertEquals(it::class.java, Chill::class.java)
                }
                .verifyComplete()
    }
}