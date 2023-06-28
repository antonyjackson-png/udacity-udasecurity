package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;

    private Sensor doorSensor;
    private Sensor windowSensor;
    private Sensor motionSensor;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;

    @BeforeEach
    void init() {
        doorSensor = new Sensor("door", SensorType.DOOR);
        windowSensor = new Sensor("window", SensorType.WINDOW);
        motionSensor = new Sensor("motion", SensorType.MOTION);
        securityService = new SecurityService(securityRepository, imageService);
    }

    // Test 1
    @Test
    @DisplayName("Verify: arming_status == ARMED && alarm_status == NO_ALARM && sensor == acivated ? call set alarm_status PENDING")
    void armingStatusArmed_alarmStatusNoAlarm_sensorActivated_alarmStatusPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(doorSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2
    @Test
    @DisplayName("Verify: arming_status == ARMED && alarm_status == PENDING && sensor == activated ? call set alarm_status ALARM")
    void armingStatusArmed_alarmStatusPending_sensorActivated_alarmStatusAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(doorSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 3
    @Test
    @DisplayName("Verify: alarm_status == OENDING && sensor == deactivated ? call set alarm_status NO_ALARM")
    void alarmStatusPending_sensorDeactivated_alarmStatusNoAlarm() {
        doorSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(doorSensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 4
    @Test
    @DisplayName("Verify: alarm_status == ALARM && sensor states change ? no call to set alarm_status")
    void alarmStatusAlarm_sensorStatesChanged_alarmStatusAlarm() {
        doorSensor.setActive(true);
        windowSensor.setActive(true);
        motionSensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(doorSensor, false);
        securityService.changeSensorActivationStatus(windowSensor, false);
        securityService.changeSensorActivationStatus(motionSensor, true);
        verify(securityRepository, times(0)).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 5
    @Test
    @DisplayName("Verify: sensor == activated && alarm_status == PENDING && another sensor == activated ? call set alarm_status ALARM")
    void alarmStatusPending_sensorActivatedAgain_alarmStatusAlarm() {
        doorSensor.setActive(true);
        windowSensor.setActive(false);
        motionSensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(windowSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    @DisplayName("Verify: alarm_status == any && sensor == deactivated && same sensor == deactivateed ? no call to set alarm_status")
    void alarmStatusAny_sensorDeactivatedAgain_alarmStatusNoChange(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(doorSensor, false);
        verify(securityRepository, times(0)).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 7
    @Test
    @DisplayName("Verify arming_status == ARMED_HOME && cat_detected ? call set alarm_status ALARM")
    void armingStatusArmedHome_catDetected_alarmStatusAlarm() {
        BufferedImage cat = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(cat);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 8
    @Test
    @DisplayName("Verify: sensors == inactive && cat_not_detected ? call set alarm_status NO_ALARM")
    void sensorsDeactivated_catNotDetected_alarmStatusNoAlarm() {
        doorSensor.setActive(false);
        windowSensor.setActive(false);
        motionSensor.setActive(false);
        BufferedImage dog = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(dog);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 9
    @Test
    @DisplayName("Verify: arming_status == NOT_ARMED ? set alarm_status NO_ALARM")
    void armingStatusArmed_armingStatusDisarmed_alarmStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 10
    @Test
    @DisplayName("Verify: arming_status == ARMED ? sensors deactivated")
    void armingStatusArmed_sensorsDeactivated() {
        doorSensor.setActive(true);
        windowSensor.setActive(true);
        motionSensor.setActive(false);
        Set<Sensor> sensors = new TreeSet<>();
        sensors.add(doorSensor);
        sensors.add(windowSensor);
        sensors.add(motionSensor);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertTrue(!doorSensor.getActive());
        assertTrue(!windowSensor.getActive());
        assertTrue(!motionSensor.getActive());
    }

    // Test 11
    @Test
    @DisplayName("Verify: cat_detected && arming_status == ARMED ? set alarm_status ALARM")
    void catDetected_armingStatusArmed_alarmStatusAlarm() {
        BufferedImage cat = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(cat);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

}
