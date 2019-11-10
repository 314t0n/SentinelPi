package space.sentinel.service

import com.pi4j.io.gpio.PinState
import reactor.core.publisher.Flux
import space.sentinel.camera.CameraReader
import space.sentinel.domain.Chill
import space.sentinel.domain.MotionDetectAlert
import space.sentinel.domain.Notification
import space.sentinel.sensor.PIRReader
import java.time.OffsetDateTime

class MotionDetectorService(val pirReader: PIRReader, val cameraReader: CameraReader) {

    fun detect(): Flux<Notification> {
        return pirReader.read()
                .flatMap {
                    createNotification(it, OffsetDateTime.now())
                }
    }

    private fun createNotification(it: PinState, ts: OffsetDateTime): Flux<out Notification> =
            if (it.isHigh) readCamera(ts)
            else Flux.just(Chill(ts))

    private fun readCamera(ts: OffsetDateTime) =
            cameraReader.read().map { frame -> MotionDetectAlert(ts, frame) }
}