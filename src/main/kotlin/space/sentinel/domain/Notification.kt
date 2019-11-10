package space.sentinel.domain

import org.bytedeco.javacv.Frame
import java.time.OffsetDateTime
import java.util.*

sealed class Notification(val timestamp: OffsetDateTime, val message: String, val image: Optional<Frame>)
data class Chill(val ts: OffsetDateTime) : Notification(ts, "Chill", Optional.empty())
data class MotionDetectAlert(val ts: OffsetDateTime, val frame: Frame) : Notification(ts, "Motion detected!", Optional.of(frame))