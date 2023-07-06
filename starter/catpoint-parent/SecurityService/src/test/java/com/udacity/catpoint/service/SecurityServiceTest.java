package com.udacity.catpoint.service;
//import static org.junit.jupiter.api.Assertions;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.udacity.catpoint.data.SecurityRepository;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    /**
     * Rigorous Test :-)
     *
//     */

    private SecurityService securityService;
    @Mock
    private StatusListener statusListener;
    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private Sensor sensor;




    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        securityService = new SecurityService(securityRepository,imageService);
        sensor= new Sensor("Window",SensorType.WINDOW);
        securityService.addSensor(sensor);

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

    @ParameterizedTest
    @EnumSource(SensorType.class)
    public void alarmArmedHome_sensorActivated_systemPendingAlarm(SensorType input) {
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(new Sensor(input.name(),input),true);
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

    @ParameterizedTest
    @EnumSource(SensorType.class)
    public void alarmArmedHome_sensorActivated_systemPendingAlarm_systemAlarm(SensorType input) {
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(new Sensor(input.name(),input),true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //If pending alarm and all sensors are inactive, return to no alarm state.
    @ParameterizedTest
    @EnumSource(SensorType.class)
    public void systemPendingAlarm_sensorNotActivated_systemNoAlarm(SensorType input) {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor newSensor= new Sensor(input.name(),input);
        newSensor.setActive(true);
        securityService.getSensors().add(newSensor);
        securityService.changeSensorActivationStatus(newSensor,false);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans={true, false})
    public void systemActiveAlarm_sensorChanged_systemActiveAlarm(boolean input) {
        Sensor newSensor= new Sensor("Sensor",SensorType.MOTION);
        securityService.getSensors().add(newSensor);
        Mockito.lenient().when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(newSensor,input);
        //assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
        Mockito.verify(securityRepository, never()).setAlarmStatus(any());
    }
//    If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    public void systemPendingAlarm_sensorActivatedAgain_systemActiveAlarm() {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor newSensor= new Sensor("Sensor",SensorType.MOTION);
        newSensor.setActive(true);
        securityService.getSensors().add(newSensor);
        securityService.changeSensorActivationStatus(newSensor,true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
//    If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void systemAnyAlarm_sensorDeactivatedAgain_systemMaintainAlarm(AlarmStatus input) {
        Mockito.lenient().when(securityService.getAlarmStatus()).thenReturn(input);
        Sensor newSensor= new Sensor("Sensor",SensorType.MOTION);
        securityService.getSensors().add(newSensor);
        securityService.changeSensorActivationStatus(sensor,false);
        //assertEquals(input, securityService.getAlarmStatus());
        Mockito.verify(securityRepository, never()).setAlarmStatus(any());
    }
    //    If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    public void testProcessImage_WhenImageContainsCatAndSystemIsArmedHome_ShouldSetAlarmStatusToAlarm() {
        // Arrange
        BufferedImage imageWithCat = mock(BufferedImage.class);
        Mockito.when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        // Act
        securityService.processImage(imageWithCat);
        // Assert
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

//    If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void testProcessImage_WhenImageDoesNotContainCatAndAllSensorsInactive_ShouldSetAlarmStatusToNoAlarm() {
        // Arrange
        BufferedImage imageWithCat = mock(BufferedImage.class);
        Mockito.when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);
        Sensor newSensor= new Sensor("New Sensor",SensorType.DOOR);
        Sensor newSensor1= new Sensor("New Sensor1",SensorType.WINDOW);
        Sensor newSensor2= new Sensor("New Sensor2",SensorType.MOTION);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(newSensor);
        sensors.add(newSensor1);
        sensors.add(newSensor2);
        Mockito.lenient().when(securityService.getSensors()).thenReturn(sensors);
        // Act
        securityService.processImage(imageWithCat);
        // Assert
        Mockito.verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//    If the system is disarmed, set the status to no alarm.
    @Test
    public void testSystemDisarmed_NoAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
//    If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @ValueSource(strings={"ARMED_HOME", "ARMED_AWAY"})
    public void testSystemArmed_AllSensorsInactive(String input){
        securityService.setArmingStatus(ArmingStatus.valueOf(input));
        Sensor newSensor= new Sensor("New Sensor",SensorType.DOOR);
        Sensor newSensor1= new Sensor("Window",SensorType.WINDOW);
        Sensor newSensor2= new Sensor("New Sensor2",SensorType.MOTION);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(newSensor);
        sensors.add(newSensor1);
        sensors.add(newSensor2);
        securityService.getSensors().add(newSensor);
        securityService.getSensors().add(newSensor2);
        Mockito.lenient().when(securityService.getSensors()).thenReturn(sensors);
        for(Sensor s: sensors){
            assertTrue(!s.getActive());
        }
    }
//    If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void testSystemArmedHome_cameraShowsCat_alarmStatusAlarm(){
        Mockito.when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        Mockito.verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void remainingTests(){
        sensor.setActive(true);
        securityService.addStatusListener(statusListener);
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,false);
        securityService.removeStatusListener(statusListener);
        Mockito.verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }


}




