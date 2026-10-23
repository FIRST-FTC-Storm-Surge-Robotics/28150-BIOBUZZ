package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.MecanumTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.OTOSTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.OctoQuadTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.PinpointTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.ThreeWheelIMUTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.ThreeWheelTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.TwoWheelTuner;

public class Tuning {
    @Tuner
    public static Procedure mecanum() {
        return new MecanumTuner();
    }

    // Run only the localizer procedure matching the hardware on the robot.
    @Tuner
    public static Procedure pinpoint() {
        return new PinpointTuner();
    }

    @Tuner
    public static Procedure otos() {
        return new OTOSTuner();
    }

    @Tuner
    public static Procedure octoQuad() {
        return new OctoQuadTuner();
    }

    @Tuner
    public static Procedure twoWheel() {
        return new TwoWheelTuner();
    }

    @Tuner
    public static Procedure threeWheel() {
        return new ThreeWheelTuner();
    }

    @Tuner
    public static Procedure threeWheelImu() {
        return new ThreeWheelIMUTuner();
    }
}
