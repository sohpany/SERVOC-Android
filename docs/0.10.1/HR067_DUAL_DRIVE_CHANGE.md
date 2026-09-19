# HR067 switchable drive control

HazRob remains Android 0.10.1 / versionCode 25. Firmware HR066 is frozen and unchanged.

HR067 adds a user-selectable drive mode in ControlTab:
- Joystick: existing DriveMixer differential control.
- Dual: two wide floating vertical spring-centred controls over the video.
- Left control maps directly to left command; right control maps directly to right command.
- Each side ranges -100..+100 before the common operator speed limiter.
- Releasing either control returns that side to zero.
- Switching modes sends 0/0, resets both input models, then changes UI.
- Selected mode persists in SharedPreferences.
- E-STOP, RobotApi transport, Direct/Remote paths and firmware 500 ms stale-stop remain shared.
- No firmware source or behavior is modified by this change.

Field diagnostic matrix:
+100/+100 forward; -100/-100 reverse; +100/-100 pivot; -100/+100 opposite pivot; each side can also be exercised independently.
