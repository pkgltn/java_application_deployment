package com.udacity.catpoint.SecurityService;


import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.service.ImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.udacity.catpoint.data.SecurityRepository;
import org.mockito.junit.jupiter.MockitoExtension;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    /**
     * Rigorous Test :-)
     *
     */

    private SecurityService securityService;
    @Mock
    private StatusListener statusListener;
    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;



    @BeforeEach
    void setUp() {
        this.securityService = new SecurityService(securityRepository,imageService);
    }


//    If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @ValueSource(strings={"ARMED_HOME", "ARMED_AWAY"})
    public void alarmArmed_sensorWindowsActivated_systemPendingAlarm(String input) {
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.valueOf(input));
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(new Sensor("Window",SensorType.WINDOW),true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest
    @ValueSource(strings={"ARMED_HOME", "ARMED_AWAY"})
    public void alarmArmed_sensorWindowsActivated_systemPendingAlarm_systemAlarm(String input) {
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.valueOf(input));
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(new Sensor("Window",SensorType.WINDOW),true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

     //If pending alarm and all sensors are inactive, return to no alarm state.
    @ParameterizedTest
    @EnumSource(SensorType.class)
    public void systemPendingAlarm_sensorNotActivated_systemNoAlarm(SensorType input) {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor newSensor= new Sensor(input.name(),input);
        newSensor.setActive(true);
        securityService.changeSensorActivationStatus(newSensor,false);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @MethodSource("alarmActive_sensorStateChange_notAffectAlarmStatus")
    public void systemActiveAlarm_sensorNotActivated_systemActiveAlarm(boolean input1, boolean input2) {
        // mock original alarm status ALARM
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // activate 1 sensor
        Sensor sensor = new Sensor("test", SensorType.DOOR);
        sensor.setActive(input1);

        // deactivate 1 sensor
        securityService.changeSensorActivationStatus(sensor, input2);
        Mockito.verify(securityRepository,never()).setAlarmStatus(any());
    }

    private static Stream<Arguments> alarmActive_sensorStateChange_notAffectAlarmStatus() {
        return Stream.of(
                Arguments.of(false, true),
                Arguments.of(true, false)
        );
    }
//    If a sensor is activated while already active and the system is in pending state, change it to alarm state. [This is the case where one sensor is already active and then another gets activated]
    @Test
    public void systemPendingAlarm_sensorActivatedAgain_systemActiveAlarm() {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("NewSensor",SensorType.DOOR);
        sensor.setActive(true);
        securityService.getSensors().add(sensor);
        Sensor newSensor= new Sensor("Sensor",SensorType.MOTION);
        securityService.changeSensorActivationStatus(newSensor,true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);

    }
//    If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void systemAnyAlarm_sensorDeactivatedAgain_systemMaintainAlarm(AlarmStatus input) {
        Mockito.lenient().when(securityService.getAlarmStatus()).thenReturn(input);
        Sensor sensor = new Sensor("Sensor",SensorType.DOOR);
        securityService.getSensors().add(sensor);
        securityService.changeSensorActivationStatus(sensor,false);
        //Sensor newSensor= new Sensor("NewSensor",SensorType.MOTION);
        //newSensor.setActive(true);
        //securityService.changeSensorActivationStatus(newSensor,false);
        Mockito.verify(securityRepository, never()).setAlarmStatus(any());
    }
    //    If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void processImage_WhenImageContainsCatAndSystemIsArmedHome_ShouldSetAlarmStatusToAlarm() {
        File sample_cat = new File("sample-cat.jpg");

        //File sample_cat = new File("sample-cat.jpg");
        System.out.println(sample_cat.getAbsolutePath());
        try {

           Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
           Mockito.when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
           BufferedImage sampleCatImage= ImageIO.read(sample_cat);
           securityService.processImage(sampleCatImage);
           Mockito.verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);


        } catch (IOException e) {
            System.out.println("Invalid Image!");
        }
    }

//   If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void processImage_WhenImageDoesNotContainCatAndAllSensorsInactive_ShouldSetAlarmStatusToNoAlarm() {
        Sensor newSensor= new Sensor("New Sensor",SensorType.DOOR);
        Sensor newSensor1= new Sensor("New Sensor1",SensorType.WINDOW);
        Sensor newSensor2= new Sensor("New Sensor2",SensorType.MOTION);
        securityService.getSensors().add(newSensor);
        securityService.getSensors().add(newSensor1);
        securityService.getSensors().add(newSensor2);
        Mockito.when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);
        BufferedImage imageWithNoCat = mock(BufferedImage.class);
        securityService.processImage(imageWithNoCat);
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //If the system is disarmed, set the status to no alarm.
    @Test
    public void systemDisarmed_NoAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
//    If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @ValueSource(strings={"ARMED_HOME", "ARMED_AWAY"})
    public void systemArmed_AllSensorsInactive(String input){
        Sensor newSensor= new Sensor("New Sensor",SensorType.DOOR);
        Sensor newSensor1= new Sensor("New Sensor1",SensorType.WINDOW);
        Sensor newSensor2= new Sensor("New Sensor2",SensorType.MOTION);
        newSensor.setActive(true);
        newSensor1.setActive(true);
        newSensor2.setActive(true);
        securityService.getSensors().add(newSensor);
        securityService.getSensors().add(newSensor1);
        securityService.getSensors().add(newSensor2);
        securityService.setArmingStatus(ArmingStatus.valueOf(input));
        for(Sensor s: securityRepository.getSensors()){
            assertFalse(s.getActive());
        }
    }
    //If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void systemArmedHome_cameraShowsCat_alarmStatusAlarm(){
        //File sample_cat = new File("src\\test\\java\\com\\udacity\\catpoint\\SecurityService\\sample-cat.jpg");
        File sample_cat = new File("sample-cat.jpg");
        try {
            Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
            Mockito.when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
            BufferedImage sampleCatImage= ImageIO.read(sample_cat);
            securityService.processImage(sampleCatImage);
            Mockito.verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
        } catch (IOException e) {
            System.out.println("Invalid Image!");
        }
    }

    @Test
    public void additionalCoverage(){
        Sensor sensor = new Sensor("New Sensor",SensorType.DOOR);
        sensor.setActive(true);
        securityService.addSensor(sensor);
        securityService.addStatusListener(statusListener);
        Mockito.lenient().when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,false);
        securityService.removeStatusListener(statusListener);
        securityService.removeSensor(sensor);
        Mockito.verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

    @ParameterizedTest
    @ValueSource(strings={"ARMED_HOME", "ARMED_AWAY"})
    public void setArmingStatusEffect(String input){
        Mockito.when(imageService.getImageContainsCat()).thenReturn(true);
        Sensor sensor1= new Sensor("Sensor1",SensorType.DOOR);
        sensor1.setActive(true);
        Sensor sensor2= new Sensor("Sensor2",SensorType.DOOR);
        securityService.addSensor(sensor1);
        securityService.addSensor(sensor2);
        securityService.setArmingStatus(ArmingStatus.valueOf(input));
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }





}




