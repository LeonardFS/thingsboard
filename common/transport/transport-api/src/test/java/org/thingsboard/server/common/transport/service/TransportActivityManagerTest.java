/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.AllEventsActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.FirstAndLastEventActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.FirstEventActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.LastEventActivityStrategy;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EXPIRED_NOTIFICATION_PROTO;

@ExtendWith(MockitoExtension.class)
public class TransportActivityManagerTest {

    private final UUID SESSION_ID = UUID.fromString("1306648a-9b26-11ee-b9d1-0242ac120002");

    @Mock
    private DefaultTransportService transportServiceMock;
    private ConcurrentMap<UUID, SessionMetaData> sessions;

    @BeforeEach
    public void setup() {
        sessions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(transportServiceMock, "sessions", sessions);
    }

//    @Override
//    protected void reportActivity(UUID sessionId, TransportProtos.SessionInfoProto currentSessionInfo, long timeToReport, ActivityReportCallback<UUID> callback) {
//        log.debug("[{}] Reporting activity state for session with id: [{}]. Time to report: [{}].", name, sessionId, timeToReport);
//        SessionMetaData session = sessions.get(sessionId);
//        TransportProtos.SubscriptionInfoProto subscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
//                .setAttributeSubscription(session != null && session.isSubscribedToAttributes())
//                .setRpcSubscription(session != null && session.isSubscribedToRPC())
//                .setLastActivityTime(timeToReport)
//                .build();
//        TransportProtos.SessionInfoProto sessionInfo = session != null ? session.getSessionInfo() : currentSessionInfo;
//        process(sessionInfo, subscriptionInfo, new TransportServiceCallback<>() {
//            @Override
//            public void onSuccess(Void msgAcknowledged) {
//                callback.onSuccess(sessionId, timeToReport);
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                callback.onFailure(sessionId, e);
//            }
//        });
//    }

    @Test
    void givenKeyAndTimeToReportAndSessionExists_whenReportingActivity_thenShouldReportActivityWithSubscriptionsAndSessionInfoFromSession() {
        // GIVEN
        long expectedTime = 123L;
        boolean expectedAttributesSubscription = true;
        boolean expectedRPCSubscription = true;
        TransportProtos.SessionInfoProto expectedSessionInfo = TransportProtos.SessionInfoProto.getDefaultInstance();

        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        SessionMetaData session = new SessionMetaData(expectedSessionInfo, TransportProtos.SessionType.ASYNC, listenerMock);
        session.setSubscribedToAttributes(expectedAttributesSubscription);
        session.setSubscribedToRPC(expectedRPCSubscription);
        sessions.put(SESSION_ID, session);

        ActivityReportCallback<UUID> callbackMock = mock(ActivityReportCallback.class);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        doCallRealMethod().when(transportServiceMock).reportActivity(SESSION_ID, sessionInfo, expectedTime, callbackMock);

        // WHEN
        transportServiceMock.reportActivity(SESSION_ID, sessionInfo, expectedTime, callbackMock);

        // THEN
        ArgumentCaptor<TransportProtos.SessionInfoProto> sessionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SessionInfoProto.class);
        ArgumentCaptor<TransportProtos.SubscriptionInfoProto> subscriptionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SubscriptionInfoProto.class);
        ArgumentCaptor<TransportServiceCallback<Void>> callbackCaptor = ArgumentCaptor.forClass(TransportServiceCallback.class);

        verify(transportServiceMock).process(sessionInfoCaptor.capture(), subscriptionInfoCaptor.capture(), callbackCaptor.capture());

        assertThat(sessionInfoCaptor.getValue()).isEqualTo(expectedSessionInfo);

        TransportProtos.SubscriptionInfoProto expectedSubscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(expectedAttributesSubscription)
                .setRpcSubscription(expectedRPCSubscription)
                .setLastActivityTime(expectedTime)
                .build();
        assertThat(subscriptionInfoCaptor.getValue()).isEqualTo(expectedSubscriptionInfo);

        TransportServiceCallback<Void> queueCallback = callbackCaptor.getValue();

        queueCallback.onSuccess(null);
        verify(callbackMock).onSuccess(SESSION_ID, expectedTime);

        var throwable = new Throwable();
        queueCallback.onError(throwable);
        verify(callbackMock).onFailure(SESSION_ID, throwable);
    }

    @Test
    void givenKeyAndTimeToReportAndSessionDoesNotExist_whenReportingActivity_thenShouldReportActivityWithNoSubscriptionsAndPreviousSessionInfo() {
        // GIVEN
        long expectedTime = 123L;
        boolean expectedAttributesSubscription = false;
        boolean expectedRPCSubscription = false;
        TransportProtos.SessionInfoProto expectedSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        ActivityReportCallback<UUID> callbackMock = mock(ActivityReportCallback.class);

        doCallRealMethod().when(transportServiceMock).reportActivity(SESSION_ID, expectedSessionInfo, expectedTime, callbackMock);

        // WHEN
        transportServiceMock.reportActivity(SESSION_ID, expectedSessionInfo, expectedTime, callbackMock);

        // THEN
        ArgumentCaptor<TransportProtos.SessionInfoProto> sessionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SessionInfoProto.class);
        ArgumentCaptor<TransportProtos.SubscriptionInfoProto> subscriptionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SubscriptionInfoProto.class);
        ArgumentCaptor<TransportServiceCallback<Void>> callbackCaptor = ArgumentCaptor.forClass(TransportServiceCallback.class);

        verify(transportServiceMock).process(sessionInfoCaptor.capture(), subscriptionInfoCaptor.capture(), callbackCaptor.capture());

        assertThat(sessionInfoCaptor.getValue()).isEqualTo(expectedSessionInfo);

        TransportProtos.SubscriptionInfoProto expectedSubscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(expectedAttributesSubscription)
                .setRpcSubscription(expectedRPCSubscription)
                .setLastActivityTime(expectedTime)
                .build();
        assertThat(subscriptionInfoCaptor.getValue()).isEqualTo(expectedSubscriptionInfo);

        TransportServiceCallback<Void> queueCallback = callbackCaptor.getValue();

        queueCallback.onSuccess(null);
        verify(callbackMock).onSuccess(SESSION_ID, expectedTime);

        var throwable = new Throwable();
        queueCallback.onError(throwable);
        verify(callbackMock).onFailure(SESSION_ID, throwable);
    }

    @Test
    void givenActivityHappened_whenRecordActivity_thenShouldDelegateToOnActivity() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        doCallRealMethod().when(transportServiceMock).recordActivity(sessionInfo);
        when(transportServiceMock.toSessionId(sessionInfo)).thenReturn(SESSION_ID);

        // WHEN
        transportServiceMock.recordActivity(sessionInfo);

        // THEN
        verify(transportServiceMock).onActivity(SESSION_ID);
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForCreateNewState")
    void givenDifferentReportingStrategies_whenCreatingNewState_thenShouldCreateEmptyStateWithCorrectStrategy(
            String reportingStrategyName, ActivityStrategy reportingStrategy
    ) {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        ReflectionTestUtils.setField(transportServiceMock, "reportingStrategyName", reportingStrategyName);

        when(transportServiceMock.createNewState(SESSION_ID)).thenCallRealMethod();

        ActivityState<TransportProtos.SessionInfoProto> expectedState = new ActivityState<>();
        expectedState.setStrategy(reportingStrategy);
        expectedState.setMetadata(sessionInfo);

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> actualState = transportServiceMock.createNewState(SESSION_ID);

        // THEN
        assertThat(actualState).isEqualTo(expectedState);
    }

    private static Stream<Arguments> provideTestParamsForCreateNewState() {
        return Stream.of(
                Arguments.of("ALL", new AllEventsActivityStrategy()),
                Arguments.of("FIRST", new FirstEventActivityStrategy()),
                Arguments.of("LAST", new LastEventActivityStrategy()),
                Arguments.of("FIRST_AND_LAST", new FirstAndLastEventActivityStrategy())
        );
    }

    @Test
    void givenSessionDoesNotExist_whenUpdatingActivityState_thenShouldReturnNull() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(123L);
        state.setLastReportedTime(312L);
        state.setMetadata(sessionInfo);
        state.setStrategy(new FirstEventActivityStrategy());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isNull();
    }

    @Test
    void givenNoGwSessionId_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;
        long lastReportedTime = 312L;
        ActivityStrategy strategySpy = spy(new FirstEventActivityStrategy());

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setLastReportedTime(lastReportedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());
        state.setStrategy(strategySpy);

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getLastReportedTime()).isEqualTo(lastReportedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
        assertThat(updatedState.getStrategy()).isEqualTo(strategySpy);
        verifyNoInteractions(strategySpy);
    }

    @Test
    void givenHasGwSessionIdButGwSessionIsNotNull_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;
        long lastReportedTime = 312L;
        ActivityStrategy strategySpy = spy(new FirstEventActivityStrategy());

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setLastReportedTime(lastReportedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());
        state.setStrategy(strategySpy);

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getLastReportedTime()).isEqualTo(lastReportedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
        assertThat(updatedState.getStrategy()).isEqualTo(strategySpy);
        verifyNoInteractions(strategySpy);

        verify(transportServiceMock, never()).getLastRecordedTime(gwSessionId);
    }

    @Test
    void givenHasGwSessionWithoutOverwriteEnabled_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        sessions.put(gwSessionId, new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock));

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;
        long lastReportedTime = 312L;
        ActivityStrategy strategySpy = spy(new FirstEventActivityStrategy());

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setLastReportedTime(lastReportedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());
        state.setStrategy(strategySpy);

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getLastReportedTime()).isEqualTo(lastReportedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
        assertThat(updatedState.getStrategy()).isEqualTo(strategySpy);
        verifyNoInteractions(strategySpy);

        verify(transportServiceMock, never()).getLastRecordedTime(gwSessionId);
    }

    @Test
    void givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsGreater_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoAndLastRecordedTime() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        SessionMetaData gwSession = new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock);
        gwSession.setOverwriteActivityTime(true);
        sessions.put(gwSessionId, gwSession);

        long gwLastRecordedTime = 500L;
        when(transportServiceMock.getLastRecordedTime(gwSessionId)).thenReturn(gwLastRecordedTime);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;
        long lastReportedTime = 312L;
        ActivityStrategy strategySpy = spy(new FirstEventActivityStrategy());

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setLastReportedTime(lastReportedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());
        state.setStrategy(strategySpy);

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(gwLastRecordedTime);
        assertThat(updatedState.getLastReportedTime()).isEqualTo(lastReportedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
        assertThat(updatedState.getStrategy()).isEqualTo(strategySpy);
        verifyNoInteractions(strategySpy);
    }

    @Test
    void givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsLess_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoOnly() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        SessionMetaData gwSession = new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock);
        gwSession.setOverwriteActivityTime(true);
        sessions.put(gwSessionId, gwSession);

        long gwLastRecordedTime = 100L;
        when(transportServiceMock.getLastRecordedTime(gwSessionId)).thenReturn(gwLastRecordedTime);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;
        long lastReportedTime = 312L;
        ActivityStrategy strategySpy = spy(new FirstEventActivityStrategy());

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setLastReportedTime(lastReportedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());
        state.setStrategy(strategySpy);

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getLastReportedTime()).isEqualTo(lastReportedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
        assertThat(updatedState.getStrategy()).isEqualTo(strategySpy);
        verifyNoInteractions(strategySpy);
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForHasExpiredTrue")
    public void givenExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnTrue(long currentTimeMillis, long lastRecordedTime, long sessionInactivityTimeout) {
        // GIVEN
        ReflectionTestUtils.setField(transportServiceMock, "sessionInactivityTimeout", sessionInactivityTimeout);

        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(transportServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = transportServiceMock.hasExpired(lastRecordedTime);

        // THEN
        assertThat(hasExpired).isTrue();
    }

    private static Stream<Arguments> provideTestParamsForHasExpiredTrue() {
        return Stream.of(
                Arguments.of(10L, 0L, 9L),
                Arguments.of(10L, 7L, 2L),
                Arguments.of(10L, 8L, 1L),
                Arguments.of(10000L, 5000L, 3000L)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForHasExpiredFalse")
    public void givenNotExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnFalse(long currentTimeMillis, long lastRecordedTime, long sessionInactivityTimeout) {
        // GIVEN
        ReflectionTestUtils.setField(transportServiceMock, "sessionInactivityTimeout", sessionInactivityTimeout);

        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(transportServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = transportServiceMock.hasExpired(lastRecordedTime);

        // THEN
        assertThat(hasExpired).isFalse();
    }

    private static Stream<Arguments> provideTestParamsForHasExpiredFalse() {
        return Stream.of(
                Arguments.of(10L, 9L, 2L),
                Arguments.of(10L, 0L, 11L),
                Arguments.of(10L, 8L, 3L),
                Arguments.of(10000L, 8000L, 3000L)
        );
    }

    @Test
    void givenSessionExists_whenOnStateExpiryCalled_thenShouldPerformExpirationActions() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));
        doCallRealMethod().when(transportServiceMock).onStateExpiry(SESSION_ID, sessionInfo);

        // WHEN
        transportServiceMock.onStateExpiry(SESSION_ID, sessionInfo);

        // THEN
        assertThat(sessions.containsKey(SESSION_ID)).isFalse();
        verify(transportServiceMock).deregisterSession(sessionInfo);
        verify(transportServiceMock).process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
        verify(listenerMock).onRemoteSessionCloseCommand(SESSION_ID, SESSION_EXPIRED_NOTIFICATION_PROTO);
    }

    @Test
    void givenSessionDoesNotExist_whenOnStateExpiryCalled_thenShouldNotPerformExpirationActions() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        doCallRealMethod().when(transportServiceMock).onStateExpiry(SESSION_ID, sessionInfo);

        // WHEN
        transportServiceMock.onStateExpiry(SESSION_ID, sessionInfo);

        // THEN
        assertThat(sessions.containsKey(SESSION_ID)).isFalse();
        verify(transportServiceMock, never()).deregisterSession(sessionInfo);
        verify(transportServiceMock, never()).process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
    }

}
