package org.firstinspires.ftc.teamcode;
import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.arcrobotics.ftclib.controller.PIDController;
import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.ColorRangeSensor;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Config
@TeleOp(name="NoDistTele", group = "Concept")
//@Disabled
public class NoDistTele extends LinearOpMode {
    //Instantiate PID controllers for arm and wind motors
    private PIDController armController;
    private PIDController windController;
    //Arm motor PID Constants
    public static double ap = 0.002, ai = 0, ad = 0.0001; //0.002, 0, 0.0001
    public static double af = -0.15; //-0.15
    public static int armDeployTarget = 0;
//    Wind Motor PID Constants
//    public static double wp = 0.07, wi = 0, wd = 0.00001;
//    public static double wf = 0.02;
//    public static int windTarget = 0;

    //Ticks in Degrees for GoBuilda Motors
    private final double ticks_in_degree = 700 / 180.0;

    //HuskyLens Stuff
    private final int READ_PERIOD = 1;
//    private HuskyLens frontCam;

    private DcMotor leftFrontDrive   = null;  //  Used to control the left front drive wheel
    private DcMotor rightFrontDrive  = null;  //  Used to control the right front drive wheel
    private DcMotor leftBackDrive    = null;  //  Used to control the left back drive wheel
    private DcMotor rightBackDrive   = null;  //  Used to control the right back drive wheel
    private DcMotor climbLeft    = null;  //  Left hanging actuator
    private DcMotor climbRight   = null;  //  Right hanging actuator
    private DcMotorEx armMotor = null; //Used to control the arm's up and down movement
    private DcMotorEx windMotor = null; //Used to control the arm's in and out movement
    private Servo clawUD = null; //Used to control the servo's up and down position
    private Servo clawLeft;
    private Servo clawRight;
    private Servo hookLeft; //Left hanging hook
    private Servo hookRight; //Right hanging hook
    private Servo launch;

    boolean clampClose = true;
    int armStage = 1;
    ElapsedTime liftTimer = new ElapsedTime();

    @Override
    public void runOpMode()
    {

        boolean targetFound     = false;    // Set to true when an AprilTag target is detected
        double  drive           = 0;        // Desired forward power/speed (-1 to +1)
        double  strafe          = 0;        // Desired strafe power/speed (-1 to +1)
        double  turn            = 0;        // Desired turning power/speed (-1 to +1)

        // Initialize the Apriltag Detection process


        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must match the names assigned during the robot configuration.
        // step (using the FTC Robot Controller app on the phone).
        leftFrontDrive  = hardwareMap.get(DcMotor.class, "frontLeft");
        rightFrontDrive = hardwareMap.get(DcMotor.class, "frontRight");
        leftBackDrive  = hardwareMap.get(DcMotor.class, "backLeft");
        rightBackDrive = hardwareMap.get(DcMotor.class, "backRight");
        climbLeft = hardwareMap.get(DcMotor.class, "climbLeft");
        climbRight = hardwareMap.get(DcMotor.class, "climbRight");
        armMotor = hardwareMap.get(DcMotorEx.class, "arm");
        windMotor = hardwareMap.get(DcMotorEx.class, "wind");
        clawUD = hardwareMap.get(Servo.class, "clawUD");
        clawLeft = hardwareMap.get(Servo.class,"clawLeft");
        clawRight = hardwareMap.get(Servo.class,"clawRight");
        hookRight = hardwareMap.get(Servo.class, "hookRight");
        hookLeft = hardwareMap.get(Servo.class, "hookLeft");
        launch = hardwareMap.get(Servo.class, "launch");


        // To drive forward, most robots need the motor on one side to be reversed, because the axles point in opposite directions.
        // When run, this OpMode should start both motors driving forward. So adjust these two lines based on your first test drive.
        // Note: The settings here assume direct drive on left and right wheels.  Gear Reduction or 90 Deg drives may require direction flips
        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        leftBackDrive.setDirection(DcMotor.Direction.REVERSE);
        rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        rightBackDrive.setDirection(DcMotor.Direction.FORWARD);

        clawRight.setDirection(Servo.Direction.REVERSE);

        leftFrontDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFrontDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftBackDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightBackDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

//        armMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        armMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        armMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

//        windMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        windMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//        windMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        //Make sure HuskyLens is working
//        if(!frontCam.knock()) {
//            telemetry.addData("-> ", "Problem communicating with " + frontCam.getDeviceName());
//        }
//        else {
//            telemetry.addData("-> ", frontCam.getDeviceName() + " ready");
//        }


        //Init PID components
        armController = new PIDController(ap, ai, ad);
//        windController = new PIDController(wp, wi, wd);
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        waitForStart();

        while (opModeIsActive())
        {
            //telemetry.addData("arm: ", armMotor.getCurrentPosition());
            //telemetry.addData("wind: ", windMotor.getCurrentPosition());

            telemetry.update();

            // Apply desired axes motions to the drivetrain.
            drive  = -gamepad1.left_stick_y  / 1;  // Reduce drive rate to 50%.
            strafe = -gamepad1.left_stick_x  / 1;  // Reduce strafe rate to 50%.
            turn   = -gamepad1.right_stick_x / 2.0;  // Reduce turn rate to 33%.
            moveRobot(drive, strafe, turn);

            //Arm up-down control
            armController.setPID(ap, ai, ad);
            int armPos = armMotor.getCurrentPosition();
            double armPID = armController.calculate(armPos, armDeployTarget);
            double armFF = Math.cos(Math.toRadians(armDeployTarget / ticks_in_degree)) * af;
            double armPower = armPID + armFF;
            armMotor.setPower(armPower);
//
//            windController.setPID(wp, wi, wd);
//            int windPos  = windMotor.getCurrentPosition();
//            double windPID = windController.calculate(windPos, windTarget);
//            double windFF = Math.cos(Math.toRadians(windTarget / ticks_in_degree)) * wf;
//            double windPower = windPID * windFF;
//            windMotor.setPower(windPower);
//
            if(gamepad1.left_bumper && armStage == 1 && liftTimer.seconds() > 0.5){
                armStage = 0;
                liftTimer.reset();
            }

            if(gamepad1.left_bumper && armStage == 2 && liftTimer.seconds() > 0.5){
                armStage = 1;
                liftTimer.reset();
            }

            if(gamepad1.left_bumper && armStage == 3 && liftTimer.seconds() > 0.5){
                armStage = 2;
                liftTimer.reset();
            }

            if(gamepad1.left_bumper && armStage == 4 && liftTimer.seconds() > 0.5){
                armStage = 3;
                liftTimer.reset();
            }
//
            if(gamepad1.right_bumper && armStage == 0 && liftTimer.seconds() > 0.5){
                armStage = 1;
                liftTimer.reset();
            }

            if(gamepad1.right_bumper && armStage == 1 && liftTimer.seconds() > 0.5){
                armStage = 2;
                liftTimer.reset();
            }

            if(gamepad1.right_bumper && armStage == 2 && liftTimer.seconds() > 0.5){
                armStage = 3;
                liftTimer.reset();
            }

            if(gamepad1.right_bumper && armStage == 3 && liftTimer.seconds() > 0.5){
                armStage = 4;
                liftTimer.reset();
            }

            if(gamepad1.right_bumper || gamepad1.left_bumper && armStage == 5 && liftTimer.seconds() > 0.5){
                armStage = 2;
                liftTimer.reset();
            }

            if (gamepad1.b){
                armStage = 1;
            }

            if (gamepad1.y){
                armStage = 5;
            }




            if(armStage == 0) {
                armDeployTarget = -475;

                windMotor.setTargetPosition(-1750);
                windMotor.setPower(1);
                windMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                clawUD.setPosition(0.53);
            }
            if(armStage == 1) {
                armDeployTarget = -475;

                windMotor.setTargetPosition(0);
                windMotor.setPower(1);
                windMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                clawUD.setPosition(0.98);
            }
            if(armStage == 2) {
                armDeployTarget = -3900;

                windMotor.setTargetPosition(-80);
                windMotor.setPower(1);
                windMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                clawUD.setPosition(0.95);
            }
            if(armStage == 3) {
                armDeployTarget = -3800;

                windMotor.setTargetPosition(-1000);
                windMotor.setPower(1);
                windMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                clawUD.setPosition(0.96);
            }
            if(armStage == 4) {
                armDeployTarget = -3700;

                windMotor.setTargetPosition(-1800);
                windMotor.setPower(1);
                windMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                clawUD.setPosition(0.98);
            }
            if(armStage == 5) {
                armDeployTarget = -4075;

                windMotor.setTargetPosition(-80);
                windMotor.setPower(1);
                windMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                clawUD.setPosition(0.93);
            }


//            if (gamepad1.right_bumper) { //move arm to scoring position
//                armDeployTarget = -3000;
//            }
//            if (gamepad1.left_bumper) { //move arm to intake position
//                armDeployTarget = -500;
//            }
//
//
//            if (gamepad1.a & armDeployTarget < -2500) { //arm moves out to score
//                windTarget = 2600;
//                clawUD.setPosition(1);
//            }
//            if (gamepad1.a & armDeployTarget > -1000) { //arm moves out to intake
//                windTarget = 2600;
//                clawUD.setPosition(1);
//            }
//            if (gamepad1.b) { //arm comes in
//                windTarget = 0;
//                clawUD.setPosition(1);
//            }

            if(gamepad1.a){
                launch.setPosition(0.5);
            }



            if (gamepad1.dpad_up) { //hanging mechanism moves up
                climbRight.setTargetPosition(2250);
                climbRight.setPower(1);
                climbRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                climbLeft.setTargetPosition(2250);
                climbLeft.setPower(1);
                climbLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            }
            if (gamepad1.dpad_down) { //hanging mechanism moves down
                climbRight.setTargetPosition(50);
                climbRight.setPower(1);
                climbRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                climbLeft.setTargetPosition(50);
                climbLeft.setPower(1);
                climbLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            }

            if (gamepad1.dpad_right & climbRight.getCurrentPosition() > 1000 & climbLeft.getCurrentPosition() > 1000) { //hanging hooks come up
                hookLeft.setPosition(1);
                hookRight.setPosition(1);
            }
            if (gamepad1.dpad_left & climbRight.getCurrentPosition() > 1000 & climbLeft.getCurrentPosition() > 1000) { //hanging hooks come down
                hookLeft.setPosition(0);
                hookRight.setPosition(0);
            }

            if(gamepad1.right_trigger >  0.5 && clampClose == false && liftTimer.seconds() > 0.5 && clampClose == false && liftTimer.seconds() > 0.5){ //claw close
                clawLeft.setPosition(0);
                clawRight.setPosition(0);
                clampClose = true;
                liftTimer.reset();
            }
            if(gamepad1.right_trigger > 0.5 && clampClose == true && liftTimer.seconds() > 0.5) { // claw open
                if (armStage == 0 || armStage == 1){
                    clawLeft.setPosition(0.15);
                    clawRight.setPosition(0.1);
                    clampClose = false;
                    liftTimer.reset();
                }
                else if (armStage >= 2){
                    clawLeft.setPosition(0.15);
                    clawRight.setPosition(0.06);
                    clampClose = false;
                    liftTimer.reset();
                }
            }



            telemetry.addData("wind: ", windMotor.getCurrentPosition());
            telemetry.addData("arm: ", armMotor.getCurrentPosition());
            telemetry.addData("climb right: ", climbRight.getCurrentPosition());
            telemetry.addData("climb right: ", climbLeft.getCurrentPosition());
            telemetry.addData("clawUD: ", clawUD.getPosition());
            telemetry.update();

        }
    }


    /**
     * Move robot according to desired axes motions
     * <p>
     * Positive X is forward
     * <p>
     * Positive Y is strafe left
     * <p>
     * Positive Yaw is counter-clockwise
     */
    public void moveRobot(double x, double y, double yaw) {
        // Calculate wheel powers.
        double leftFrontPower    =  x -y -yaw;
        double rightFrontPower   =  x +y +yaw;
        double leftBackPower     =  x +y -yaw;
        double rightBackPower    =  x -y +yaw;

        // Normalize wheel powers to be less than 1.0
        double max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
        max = Math.max(max, Math.abs(leftBackPower));
        max = Math.max(max, Math.abs(rightBackPower));

        if (max > 1.0) {
            leftFrontPower /= max;
            rightFrontPower /= max;
            leftBackPower /= max;
            rightBackPower /= max;
        }

        // Send powers to the wheels.
        leftFrontDrive.setPower(leftFrontPower);
        rightFrontDrive.setPower(rightFrontPower);
        leftBackDrive.setPower(leftBackPower);
        rightBackDrive.setPower(rightBackPower);
    }
}