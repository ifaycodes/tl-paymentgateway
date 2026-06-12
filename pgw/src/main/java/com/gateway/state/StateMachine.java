package com.gateway.state;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.gateway.proof.Receipt;



public class StateMachine {

    @Autowired
    private Receipt receipt;

    public StateMachine (Receipt receipt) {
        this.receipt = receipt;
    }

    private State currentState;
    private Map<State, Map<Event, State>> transitions;

    public StateMachine() {
        this.currentState = State.PENDING;
        this.transitions = new EnumMap<>(State.class);
        initializeTransitions();
    }

    private void initializeTransitions() {
        transitions.put(State.PENDING, new EnumMap<>(Event.class));
        transitions.get(State.PENDING).put(Event.RESERVE_FUNDS, State.APPROVED);

        transitions.put(State.APPROVED, new EnumMap<>(Event.class));
        transitions.get(State.APPROVED).put(Event.WITHDRAW_FUNDS, State.CAPTURED);
        transitions.get(State.APPROVED).put(Event.CANCEL_TRANSACTION, State.VOIDED);

        transitions.put(State.CAPTURED, new EnumMap<>(Event.class));
        transitions.get(State.CAPTURED).put(Event.CANCEL_ORDER, State.REFUNDED);

        transitions.put(State.VOIDED, new EnumMap<>(Event.class));
        transitions.put(State.REFUNDED, new EnumMap<>(Event.class));
    }

    public State getCurrentState() {
        return currentState;
    }

    public void processEvent(Event event) {
        Map<Event, State> stateTransitions = transitions.get(currentState);
        if (stateTransitions != null && stateTransitions.containsKey(event)) {
            currentState = stateTransitions.get(event);
            receipt.setCurrentState(currentState);
        } else {
            throw new IllegalStateException("Invalid event: " + event + "for current state: " + currentState);
        }

    }
}
