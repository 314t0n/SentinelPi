package space.sentinel.service

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.pi4j.io.gpio.PinState
import org.bytedeco.javacv.Frame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import space.sentinel.domain.Chill
import space.sentinel.sensor.PIRReader

internal class MotionDetectorServiceTest {
    private val frame = mock<Frame>()

    private val cameraReader = mock<CameraReader2> {
        on { read() }.doReturn(Flux.generate {
//            it.next(frame)
            Thread.sleep(1000)
        })
    }

//    @Test
//    fun `asdasd alert when state is high`() {
//
//        Flux.generate<Any, PinState>(
//                { PinState.LOW },
//                { state, sink ->
//
//                    sink.next(state)
//
//                    if (state.isLow) {
//                        sink.complete()
//                    }
//
//
//                    state
//                })
//
//
//    }


    @Test
    fun `return alert when state is high`() {

        var state = true

        val pirReader = mock<PIRReader> {
            on { read() }.doReturn(Flux.generate {
                if (state) it.next(PinState.HIGH)
                else it.next(PinState.LOW)
                Thread.sleep(5000)
                state = !state
            })

        }

        val cameraReader = mock<CameraReader2> {
            on { read() }.doReturn(Flux.generate {
                Thread.sleep(1000)
//                it.next(frame)
                if(!state) it.complete()
            })
        }

        MotionDetectorService(pirReader, cameraReader).detect().blockLast()

//        StepVerifier.create(MotionDetectorService(pirReader, cameraReader).detect())
//                .consumeNextWith {
//                    assertEquals(it.image.get(), frame)
//                    assertEquals(it::class.java, MotionDetectAlert::class.java)
//                }
//                .verifyComplete()
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