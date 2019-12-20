package space.sentinel.service

import com.typesafe.config.Config
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import org.slf4j.LoggerFactory
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import reactor.core.scheduler.Schedulers
import space.sentinel.domain.MotionDetectAlert
import space.sentinel.domain.Notification
import space.sentinel.sensor.PIRReader
import java.lang.NullPointerException
import java.time.OffsetDateTime
import java.util.*

class CameraReader2(config: Config) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val grabber: FrameGrabber = FFmpegFrameGrabber(config.getString("camera.path"))

    init {
        logger.debug("Setup camera.")
        grabber.format = config.getString("camera.format")
        grabber.pixelFormat = avutil.AV_PIX_FMT_BGR24
        grabber.frameRate = 5.0
    }

    fun start() {
        grabber.start()
    }

    fun stop() {
        grabber.stop()
    }

    fun read(): Flux<Frame> {
        return Flux.generate { synchronousSink: SynchronousSink<Frame> ->
            try {
                synchronousSink.next(grabber.grab())
            } catch (ex: NullPointerException) {
                synchronousSink.complete()
            }
        }
    }

    override fun close() {
        logger.debug("Close.")
    }
}

class MotionDetectorService(val pirSensor: PIRReader, val camera: CameraReader2) {

    val logger = LoggerFactory.getLogger(MotionDetectorService::class.java)

    val processor = EmitterProcessor.create<Notification>()

    fun detect(): Flux<Notification> {

        pirSensor.read()
                .doOnNext {
                    logger.debug(it.getName())

                    if (it.isHigh) {
                        logger.debug("Starting camera")
                        camera.start()
                        camera.read()
                                .map { frame -> MotionDetectAlert(OffsetDateTime.now(), frame) }
                                .doOnNext { motionDetectAlert ->
                                    logger.debug(motionDetectAlert.message)
                                    processor.onNext(motionDetectAlert)
                                }
                                .subscribeOn(Schedulers.elastic())
                                .subscribe()
                    } else {
                        logger.debug("camera stop")
                        camera.stop()
                    }
                }
                .subscribeOn(Schedulers.elastic())
                .subscribe()

        return processor
    }
}