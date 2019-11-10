package space.sentinel

import com.pi4j.io.gpio.*
import org.bytedeco.javacv.Frame
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import space.sentinel.camera.CameraReader
import space.sentinel.sensor.PIRReader
import space.sentinel.service.MotionDetectorService
import space.sentinel.util.ConfigLoaderFactory
import java.time.OffsetDateTime
import java.util.*


fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Sentinel")

    val config = ConfigLoaderFactory().load()

    logger.info("Setup GPIO &pinAddress")

    val pinAddress = config.getInt("camera.gpio").or(0)
    val gpioSensor: GpioController = GpioFactory.getInstance()
    val sensor: GpioPinDigitalInput = gpioSensor.provisionDigitalInputPin(RaspiPin.getPinByAddress(pinAddress))
    val reader = PIRReader(sensor)
    val camera = CameraReader(config)

    try {
        MotionDetectorService(reader, camera)
                .detect()
                .publishOn(Schedulers.elastic())
                .doOnNext {
                    //send to server
                }

        // service port
        // ping

    } catch (ex: Exception) {
        logger.error(ex.message)
    }
}
